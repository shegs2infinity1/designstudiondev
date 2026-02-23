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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * Mapper and adapter utilities for Bloomberg FT flow. Provides FILE and WMQ
 * adapters plus JSON→FT field mapping helpers.
 */
public final class CsdBloombergFtMappers {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtMappers.class.getName());

    // Cache: JMSMessageID -> message body
    private static final Map<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();

    // Legacy placeholder to maintain compatibility if referenced elsewhere
    private static final Map<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();

    private CsdBloombergFtMappers() {
    }

    // ============================
    // ===== FILE ADAPTER =========
    // ============================

    /**
     * Scans directory for JSON files, reads content, counts FUNDS_MOVEMENTS, moves
     * files to PROCESSED, then builds IDs using the moved path.
     */
    public static List<String> extractIdsFromDirectoryAndMove(Path p_dir, String p_glob, Path p_processedDir,
            ObjectMapper p_om) {
        List<String> ids = new ArrayList<>();

        try {
            if (!Files.exists(p_processedDir)) {
                Files.createDirectories(p_processedDir);
                LOGGER.log(Level.INFO, "[CsdBloombergFtMappers] FILE: created PROCESSED directory: {0}",
                        p_processedDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] FILE: failed to create PROCESSED directory", e);
            return ids;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p_dir, p_glob)) {
            for (Path file : stream) {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                    if (content.isEmpty()) {
                        continue;
                    }
                    JsonNode root = p_om.readTree(content);
                    if (!hasFundsMovements(root)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergFtMappers] FILE: no FUNDS_MOVEMENTS in {0}", file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                        Path target = p_processedDir
                                .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.log(Level.INFO, "[CsdBloombergFtMappers] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    Path target = p_processedDir
                            .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.INFO, "[CsdBloombergFtMappers] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode arr = root.get("FUNDS_MOVEMENTS");
                    for (int i = 0; i < arr.size(); i++) {
                        ids.add("FILE|" + target.toString() + "|FM|" + i);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] FILE: error reading/moving " + file, ex);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] FILE: error scanning directory", ioe);
        }

        return ids;
    }

    public static JsonNode readRoot(Path p_file, ObjectMapper p_om) throws IOException {
        String content = new String(Files.readAllBytes(p_file), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return null;
        }
        return p_om.readTree(content);
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

    // ============================
    // ===== WMQ ADAPTER ==========
    // ============================

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
                        LOGGER.log(Level.WARNING, "[CsdBloombergFtMappers] WMQ: non-text message skipped");
                        continue;
                    }
                    String body = ((TextMessage) m).getText();
                    if (body == null || body.trim().isEmpty())
                        continue;
                    String msgId = m.getJMSMessageID();

                    MQ_MESSAGE_CACHE.put(msgId, body);
                    // do not cache Message objects in AUTO ack mode

                    try {
                        JsonNode root = p_om.readTree(body);
                        if (hasFundsMovements(root)) {
                            JsonNode arr = root.get("FUNDS_MOVEMENTS");
                            for (int i = 0; i < arr.size(); i++) {
                                ids.add("WMQ|" + msgId + "|FM|" + i);
                            }
                            LOGGER.log(Level.INFO,
                                    "[CsdBloombergFtMappers] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                    new Object[] { ackMode, arr.size(), msgId });
                        } else {
                            LOGGER.log(Level.WARNING, "[CsdBloombergFtMappers] WMQ: no FUNDS_MOVEMENTS in {0}", msgId);
                        }
                    } catch (IOException ioe) {
                        LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] WMQ: JSON parse error", ioe);
                    }
                }

                LOGGER.log(Level.INFO, "[CsdBloombergFtMappers] WMQ: finished consuming messages, total IDs={0}",
                        ids.size());
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException jmse) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] WMQ: JMS error consuming messages", jmse);
        } catch (RuntimeException re) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] WMQ: Runtime error", re);
        }

        return ids;
    }

    public static JsonNode readMqMessage(String p_messageId, ObjectMapper p_om) throws IOException {
        String body = MQ_MESSAGE_CACHE.get(p_messageId);
        if (body == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtMappers] WMQ: body not found for {0}", p_messageId);
            return null;
        }
        return p_om.readTree(body);
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
                    LOGGER.log(Level.INFO, "[CsdBloombergFtMappers] WMQ: acknowledged {0}", ref.messageId());
                } else {
                    LOGGER.log(Level.WARNING, "[CsdBloombergFtMappers] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtMappers] WMQ: Error acknowledging message", e);
        }
    }

    // ============================
    // ===== JSON HELPERS =========
    // ============================

    public static boolean hasFundsMovements(JsonNode p_root) {
        return p_root != null && p_root.has("FUNDS_MOVEMENTS") && p_root.get("FUNDS_MOVEMENTS").isArray()
                && p_root.get("FUNDS_MOVEMENTS").size() > 0;
    }

    public static JsonNode getFundsMovementAt(JsonNode p_root, int p_index) {
        return p_root.get("FUNDS_MOVEMENTS").get(p_index);
    }

    /**
     * Map a single funds movement node to FT field map. Only basic fields are
     * mapped here; extend as needed.
     */
    public static Map<String, String> mapFundsMovementToFt(JsonNode p_fm) {
        Map<String, String> map = new HashMap<>();

        String dAcc = asText(p_fm, "DEBIT_ACCT_NO");
        String dCcy = asText(p_fm, "DEBIT_CURRENCY");
        String dAmt = asText(p_fm, "DEBIT_AMOUNT");
        String dDat = asText(p_fm, "DEBIT_VALUE_DATE");
        String dRef = asText(p_fm, "DEBIT_THIER_REF");

        String oCus = asText(p_fm, "ORDERING_CUSTOMER");
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

    // ============================
    // ===== MQ CONFIG ============
    // ============================

    private static Properties loadMqProperties() {
        Properties props = new Properties();
        // Defaults. Replace with external config in production.
        props.setProperty("mq.host", "172.24.48.214");
        props.setProperty("mq.port", "1414");
        props.setProperty("mq.channel", "T24.CLIENT.SVRCONN");
        props.setProperty("mq.qmgr", "QM.T24.BETA");
        props.setProperty("mq.queue", "T24.Q.IN");
        props.setProperty("mq.user", "");
        props.setProperty("mq.password", "");
        props.setProperty("mq.ack", "auto"); // auto | manual
        return props;
    }

    private static MQConnectionFactory createMqFactory(Properties p_props) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(p_props.getProperty("mq.host"));
        factory.setPort(Integer.parseInt(p_props.getProperty("mq.port")));
        factory.setChannel(p_props.getProperty("mq.channel"));
        factory.setQueueManager(p_props.getProperty("mq.qmgr"));
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        return factory;
    }

    // ============================
    // ===== QUIET CLOSERS ========
    // ============================

    private static void closeQuietly(MessageConsumer c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtMappers] close consumer ignored", e);
            }
    }

    private static void closeQuietly(Session s) {
        if (s != null)
            try {
                s.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtMappers] close session ignored", e);
            }
    }

    private static void closeQuietly(Connection c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtMappers] close connection ignored", e);
            }
    }
}
