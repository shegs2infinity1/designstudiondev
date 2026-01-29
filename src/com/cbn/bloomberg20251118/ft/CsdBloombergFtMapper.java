package com.cbn.bloomberg.ft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.cbn.bloomberg.hp.CsdBloombergProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;

/**
 * Mapper and adapter utilities for Bloomberg FT flow. Provides FILE and WMQ
 * adapters plus JSON→FT field mapping helpers.
 */
public final class CsdBloombergFtMapper {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtMapper.class.getName());
    private static final String FTMOVE = "FUNDS_MOVEMENTS";
    private static final Map<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CsdBloombergFtMapper() {
    }

    public static List<String> extractIdsFromDirectoryAndMove(Path p_dir, String p_glob, Path p_processedDir,
            ObjectMapper p_om) {
        List<String> ids = new ArrayList<>();

        try {
            if (!Files.exists(p_processedDir)) {
                Files.createDirectories(p_processedDir);
                LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] FILE: created PROCESSED directory: {0}", p_processedDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] FILE: failed to create PROCESSED directory", e);
            return ids;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p_dir, p_glob)) {
            for (Path file : stream) {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                    if (content.isEmpty()) {
                        continue;
                    }
                    JsonNode root = normalizeRoot(p_om.readTree(content));
                    if (!hasFundsMovements(root)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] FILE: no" + FTMOVE + "in {0}", file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = p_processedDir
                                .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = p_processedDir
                            .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode fm = root.get(FTMOVE);
                    int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + target.toString() + "|FM|" + i);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] FILE: error reading/moving " + file, ex);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] FILE: error scanning directory", ioe);
        }

        return ids;
    }

    public static JsonNode readRoot(Path p_file, ObjectMapper p_om) throws IOException {
        String content = new String(Files.readAllBytes(p_file), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return null;
        }
        return normalizeRoot(p_om.readTree(content));
    }

    public static FileItemRef parseFileItemRef(String p_id) {
        if (p_id == null || !p_id.startsWith("FILE|"))
            return null;
        String[] parts = p_id.split("\\|", 4);
        if (parts.length != 4 || !"FM".equals(parts[2]))
            return null;
        try {
            int idx = Integer.parseInt(parts[3]);
            return new FileItemRef(Paths.get(parts[1]), idx);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public static final class FileItemRef {
        private final Path m_file;
        private final int m_index;

        FileItemRef(Path f, int i) {
            this.m_file = f;
            this.m_index = i;
        }

        public Path file() {
            return m_file;
        }

        public int index() {
            return m_index;
        }
    }

    public static List<String> extractIdsFromMq(ObjectMapper p_om) {
        List<String> ids = new ArrayList<>();

        try {
            Properties props = loadMqProperties();
            MQConnectionFactory factory = createMqFactory(props);

            String user = props.getProperty("mq.user", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("mq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                String ackMode = props.getProperty("mq.ack", "auto").trim().toLowerCase();
                int jmsAck = "auto".equals(ackMode) ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE;
                session = connection.createSession(false, jmsAck);
                javax.jms.Queue queue = session.createQueue("queue:///" + props.getProperty("mq.queue"));
                consumer = session.createConsumer(queue);

                while (true) {
                    Message m = consumer.receive(1000);
                    if (m == null)
                        break;
                    if (!(m instanceof TextMessage)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] WMQ: non-text message skipped");
                        continue;
                    }
                    String body = ((TextMessage) m).getText();
                    if (body == null || body.trim().isEmpty())
                        continue;
                    String msgId = m.getJMSMessageID();

                    MQ_MESSAGE_CACHE.put(msgId, body);
                    // do not cache Message objects in AUTO ack mode

                    try {
                        JsonNode root = normalizeRoot(p_om.readTree(body));
                        if (hasFundsMovements(root)) {
                            JsonNode fm = root.get(FTMOVE);
                            int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                            for (int i = 0; i < size; i++) {
                                ids.add("WMQ|" + msgId + "|FM|" + i);
                            }
                            LOGGER.log(Level.INFO,
                                    "[CsdBloombergFtMapper] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                    new Object[] { ackMode, size, msgId });
                        } else {
                            LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] WMQ: no" + FTMOVE + " in {0}", msgId);
                        }
                    } catch (IOException ioe) {
                        LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] WMQ: JSON parse error", ioe);
                    }
                }

                LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] WMQ: finished consuming messages, total IDs={0}",
                        ids.size());
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException jmse) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] WMQ: JMS error consuming messages", jmse);
        } catch (RuntimeException re) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] WMQ: Runtime error", re);
        }

        return ids;
    }

    public static JsonNode readMqMessage(String p_messageId, ObjectMapper p_om) throws IOException {
        String body = MQ_MESSAGE_CACHE.get(p_messageId);
        if (body == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] WMQ: body not found for {0}", p_messageId);
            return null;
        }
        return normalizeRoot(p_om.readTree(body));
    }

    public static MqItemRef parseMqItemRef(String p_id) {
        if (p_id == null || !p_id.startsWith("WMQ|"))
            return null;
        String[] parts = p_id.split("\\|", 4);
        if (parts.length != 4 || !"FM".equals(parts[2]))
            return null;
        try {
            int idx = Integer.parseInt(parts[3]);
            return new MqItemRef(parts[1], idx);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public static final class MqItemRef {
        private final String m_messageId;
        private final int m_index;

        MqItemRef(String mid, int i) {
            this.m_messageId = mid;
            this.m_index = i;
        }

        public String messageId() {
            return m_messageId;
        }

        public int index() {
            return m_index;
        }
    }

    public static void acknowledgeMqMessage(String p_id) {
        try {
            Properties props = loadMqProperties();
            String ackMode = props.getProperty("mq.ack", "auto").trim().toLowerCase();

            MqItemRef ref = parseMqItemRef(p_id);
            if (ref == null)
                return;

            if ("auto".equals(ackMode)) {
                // auto-ack already occurred on receive; just clear cache
                MQ_MESSAGE_CACHE.remove(ref.messageId());
                MQ_MESSAGE_OBJECTS.remove(ref.messageId());
                return;
            }

            // manual ack via selector
            MQConnectionFactory factory = createMqFactory(props);
            String user = props.getProperty("mq.user", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("mq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                javax.jms.Queue queue = session.createQueue("queue:///" + props.getProperty("mq.queue"));
                String selector = "JMSMessageID = '" + ref.messageId().replace("'", "''") + "'";
                consumer = session.createConsumer(queue, selector);
                Message message = consumer.receive(2000);
                if (message != null) {
                    message.acknowledge();
                    MQ_MESSAGE_CACHE.remove(ref.messageId());
                    MQ_MESSAGE_OBJECTS.remove(ref.messageId());
                    LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] WMQ: acknowledged {0}", ref.messageId());
                } else {
                    LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] WMQ: Error acknowledging message", e);
        }
    }

    public static void persistFailedFileItem(FileItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.file() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] persistFailedFileItem: null ref/file");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String baseName = ref.file().getFileName().toString().replace(".json", "");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("%s_FM%d_%s.json", baseName, ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.copy(ref.file(), outFile, StandardCopyOption.REPLACE_EXISTING);
            writeReasonSidecar(outFile, reason);
            LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] FILE EXCEPTS: failed to persist failed item", ex);
        }
    }

    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_FM%d_%s.json", sanitizedId, ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMapper] WMQ EXCEPTS: failed to persist failed item", ex);
        }
    }

    private static void ensureDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private static void writeReasonSidecar(Path dataFile, String reason) {
        try {
            Path errFile = Paths.get(dataFile.toString().replaceAll("\\.json$", "") + ".err");
            String ts = LocalDateTime.now().format(TS_FMT);
            String contents = "[" + ts + "] FAILURE REASON: " + (reason == null ? "" : reason);
            Files.write(errFile, contents.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtMapper] Failed to write .err sidecar for {0}", dataFile);
        }
    }

    // ====
    // ==== JSON HELPERS ====
    // ====

    public static boolean hasFundsMovements(JsonNode p_root) {
        if (p_root == null)
            return false;
        JsonNode fm = p_root.get(FTMOVE);
        if (fm == null || fm.isNull())
            return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    public static JsonNode getFundsMovementAt(JsonNode p_root, int p_index) {
        JsonNode fm = p_root.get(FTMOVE);
        if (fm == null)
            return null;
        if (fm.isArray())
            return fm.get(p_index);
        return fm.isObject() ? fm : null;
    }

    public static Map<String, String> mapFundsMovementToFt(JsonNode p_fm) {
        Map<String, String> map = new HashMap<>();

        String dAcc = asText(p_fm, "DEBIT_ACCT_NO");
        String dCcy = asText(p_fm, "DEBIT_CURRENCY");
        String dAmt = asText(p_fm, "DEBIT_AMOUNT");
        String dDat = asText(p_fm, "DEBIT_VALUE_DATE");
        String dRef = asText(p_fm, "DEBIT_THIER_REF");
        String oCus = asText(p_fm, "ORDERING_CUST");
        String pDet = asText(p_fm, "PAYMENT_DETAILS");
        String cAcc = asText(p_fm, "CREDIT_ACCT_NO");
        String cCcy = asText(p_fm, "CREDIT_CURRENCY");
        String cAmt = asText(p_fm, "CREDIT_AMOUNT");
        String cDat = asText(p_fm, "CREDIT_VALUE_DATE");
        String cRef = asText(p_fm, "CREDIT_THIER_REF");

        map.put("DACC", safe(dAcc));
        map.put("DCCY", safe(dCcy));
        map.put("DAMT", normalizeAmount(safe(dAmt)));
        map.put("DDAT", safe(dDat));
        map.put("DREF", safe(dRef));
        map.put("OCUS", safe(oCus));
        map.put("PDET", safe(pDet));
        map.put("CACC", safe(cAcc));
        map.put("CCCY", safe(cCcy));
        map.put("CAMT", normalizeAmount(safe(cAmt)));
        map.put("CDAT", safe(cDat));
        map.put("CREF", safe(cRef));
        return map;
    }

    private static String asText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String normalizeAmount(String amt) {
        if (amt == null || amt.trim().isEmpty())
            return "";
        return amt.replace(",", "");
    }

    private static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull())
            return om.createObjectNode();

        // Already wrapped
        if (root.isObject() && root.has(FTMOVE)) {
            JsonNode fm = root.get(FTMOVE);
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set(FTMOVE, arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set(FTMOVE, root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set(FTMOVE, arr);
            return w;
        }

        return om.createObjectNode();
    }

    private static Properties loadMqProperties() {
        Properties props = new Properties();
        CsdBloombergProperties config = CsdBloombergProperties.getInstance();

        // Load from CsdBloombergProperties with fallback defaults
        props.setProperty("mq.host", config.getProperty("bmrg.mq.host", "172.105.249.157"));
        props.setProperty("mq.port", config.getProperty("bmrg.mq.port", "1414"));
        props.setProperty("mq.channel", config.getProperty("bmrg.mq.channel", "DEV.APP.SVRCONN"));
        props.setProperty("mq.qmgr", config.getProperty("bmrg.mq.qmgr", "QM_BLOOMBERG"));
        props.setProperty("mq.queue", config.getProperty("bmrg.mq.queue", "TEST.QUEUE"));
        props.setProperty("mq.user", config.getProperty("bmrg.mq.user", ""));
        props.setProperty("mq.password", config.getProperty("bmrg.mq.password", ""));
        props.setProperty("mq.ack", config.getProperty("bmrg.mq.ack", "auto"));

        return props;
    }

    private static MQConnectionFactory createMqFactory(Properties p_props) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(p_props.getProperty("mq.host"));
        factory.setPort(Integer.parseInt(p_props.getProperty("mq.port")));
        factory.setChannel(p_props.getProperty("mq.channel"));
        factory.setQueueManager(p_props.getProperty("mq.qmgr"));
        factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
        return factory;
    }

    private static void closeQuietly(MessageConsumer c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtMapper] close consumer ignored", e);
            }
    }

    private static void closeQuietly(Session s) {
        if (s != null)
            try {
                s.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtMapper] close session ignored", e);
            }
    }

    private static void closeQuietly(Connection c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtMapper] close connection ignored", e);
            }
    }
}