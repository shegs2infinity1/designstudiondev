package com.cbn.bloomberg.pd;

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
import java.util.List;
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

import com.cbn.bloomberg.hp.CsdBloombergPdProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;


/**
 * Adapter manager for Bloomberg RO flow. Handles FILE and WMQ transport concerns including message
 * consumption, caching, acknowledgment, and failure persistence.
 */
public final class CsdBloombergPdAdapter {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergPdAdapter.class.getName());

    // Cache: JMSMessageID -> message body
    private static final ConcurrentHashMap<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();

    // Legacy placeholder to maintain compatibility if referenced elsewhere
    private static final ConcurrentHashMap<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();

    // Timestamp format for filenames
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CsdBloombergPdAdapter() {
    }

    // ====================================================================
    // FILE ADAPTER
    // ====================================================================

    /**
    * Scans directory for JSON files, reads content, counts PLACEMENTS, moves files to PROCESSED,
    * then builds IDs using the moved path.
    */
    public static List<String> extractIdsFromDirectoryAndMove(Path pDir, String pGlob, Path pProcessedDir,
            ObjectMapper pOjbMap) {
        List<String> ids = new ArrayList<>();

        try {
            if (!Files.exists(pProcessedDir)) {
                Files.createDirectories(pProcessedDir);
                LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] FILE: created PROCESSED directory: {0}",
                        pProcessedDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] FILE: failed to create PROCESSED directory", e);
            return ids;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pDir, pGlob)) {
            for (Path file : stream) {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                    if (content.isEmpty()) {
                        continue;
                    }
                    JsonNode root = CsdBloombergPdMapper.normalizeRoot(pOjbMap.readTree(content));
                    if (!CsdBloombergPdMapper.hasPlacements(root)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] FILE: no PLACEMENTS in {0}", file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = pProcessedDir.resolve(
                                file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = pProcessedDir.resolve(
                            file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode fm = root.get("PLACEMENTS");
                    int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + target.toString() + "|FM|" + i);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE,
                            String.format("[CsdBloombergPdAdapter] FILE: error reading/moving %s", file), ex);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] FILE: error scanning directory", ioe);
        }

        return ids;
    }

    /**
    * Reads and normalizes JSON from a file.
    */
    public static JsonNode readRoot(Path pFile, ObjectMapper pOjbMap) throws IOException {
        String content = new String(Files.readAllBytes(pFile), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return null;
        }
        return CsdBloombergPdMapper.normalizeRoot(pOjbMap.readTree(content));
    }

    /**
    * Parses a FILE-mode ID string into a FileItemRef.
    */
    public static FileItemRef parseFileItemRef(String pId) {
        if (pId == null || !pId.startsWith("FILE|")) return null;
        String[] parts = pId.split("\\|", 4);
        if (parts.length != 4 || !"FM".equals(parts[2])) return null;
        try {
            int idx = Integer.parseInt(parts[3]);
            return new FileItemRef(Paths.get(parts[1]), idx);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
    * Reference to a specific item within a FILE-mode message.
    */
    public static final class FileItemRef {

        private final Path mFile;
        private final int mIndex;

        FileItemRef(Path f, int i) {
            this.mFile = f;
            this.mIndex = i;
        }

        public Path file() {
            return mFile;
        }

        public int index() {
            return mIndex;
        }
    }

    // ====================================================================
    // WMQ ADAPTER
    // ====================================================================

    /**
    * Consumes messages from WMQ, caches them, and returns a list of IDs.
    */
    public static List<String> extractIdsFromMq(ObjectMapper pOjbMap) {
        List<String> ids = new ArrayList<>();

        try {
            Properties props = loadMqProperties();
            MQConnectionFactory factory = createMqFactory(props);

            String user = props.getProperty("pd.mq.user", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("pd.mq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                String ackMode = props.getProperty("pd.mq.ack", "auto").trim().toLowerCase();
                int jmsAck = "auto".equals(ackMode) ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE;
                session = connection.createSession(false, jmsAck);
                javax.jms.Queue queue = session.createQueue("queue:///" + props.getProperty("pd.mq.queue"));
                consumer = session.createConsumer(queue);

                while (true) {
                    Message m = consumer.receive(1000);
                    if (m == null) break;
                    if (!(m instanceof TextMessage)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] WMQ: non-text message skipped");
                        continue;
                    }
                    String body = ((TextMessage) m).getText();
                    if (body == null || body.trim().isEmpty()) continue;
                    String msgId = m.getJMSMessageID();

                    // FIX: Strip "ID:" prefix before caching to ensure consistent key format
                    String cacheKey = msgId;
                    if (msgId != null && msgId.startsWith("ID:")) {
                        cacheKey = msgId.substring(3);
                    }
                    MQ_MESSAGE_CACHE.put(cacheKey, body);

                    try {
                        JsonNode root = CsdBloombergPdMapper.normalizeRoot(pOjbMap.readTree(body));
                        if (CsdBloombergPdMapper.hasPlacements(root)) {
                            JsonNode fm = root.get("PLACEMENTS");
                            int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);

                            for (int i = 0; i < size; i++) {
                                ids.add("WMQ|" + cacheKey + "|FM|" + i);
                            }
                            LOGGER.log(Level.INFO,
                                    "[CsdBloombergPdAdapter] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                    new Object[] { ackMode, size, msgId });
                        } else {
                            LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] WMQ: no PLACEMENTS in {0}", msgId);
                        }
                    } catch (IOException ioe) {
                        LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] WMQ: JSON parse error", ioe);
                    }
                }

                LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] WMQ: finished consuming messages, total IDs={0}",
                        ids.size());
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException jmse) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] WMQ: JMS error consuming messages", jmse);
        } catch (RuntimeException re) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] WMQ: Runtime error", re);
        }

        return ids;
    }

    /**
    * Reads a cached MQ message body and normalizes it.
    */
    public static JsonNode readMqMessage(String pMessageId, ObjectMapper pOjbMap) throws IOException {
        String body = MQ_MESSAGE_CACHE.get(pMessageId);
        if (body == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] WMQ: body not found for {0}", pMessageId);
            return null;
        }
        return CsdBloombergPdMapper.normalizeRoot(pOjbMap.readTree(body));
    }

    /**
    * Parses a WMQ-mode ID string into a MqItemRef.
    */
    public static MqItemRef parseMqItemRef(String pId) {
        if (pId == null || !pId.startsWith("WMQ|")) return null;
        String[] parts = pId.split("\\|", 4);
        if (parts.length != 4 || !"FM".equals(parts[2])) return null;
        try {
            int idx = Integer.parseInt(parts[3]);
            return new MqItemRef(parts[1], idx);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
    * Reference to a specific item within a WMQ message.
    */
    public static final class MqItemRef {

        private final String mMessageId;
        private final int mIndex;

        MqItemRef(String mid, int i) {
            this.mMessageId = mid;
            this.mIndex = i;
        }

        public String messageId() {
            return mMessageId;
        }

        public int index() {
            return mIndex;
        }
    }

    /**
    * Acknowledges a WMQ message (manual ack mode) or clears cache (auto ack mode).
    */
    public static void acknowledgeMqMessage(String pId) {
        try {
            Properties props = loadMqProperties();
            String ackMode = props.getProperty("pd.mq.ack", "auto").trim().toLowerCase();

            MqItemRef ref = parseMqItemRef(pId);
            if (ref == null) return;

            if ("auto".equals(ackMode)) {
                // auto-ack already occurred on receive; just clear cache
                MQ_MESSAGE_CACHE.remove(ref.messageId());
                MQ_MESSAGE_OBJECTS.remove(ref.messageId());
                return;
            }

            // manual ack via selector
            MQConnectionFactory factory = createMqFactory(props);
            String user = props.getProperty("pd.mq.user", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("pd.mq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                javax.jms.Queue queue = session.createQueue("queue:///" + props.getProperty("pd.mq.queue"));
                String selector = "JMSMessageID = '" + ref.messageId().replace("'", "''") + "'";
                consumer = session.createConsumer(queue, selector);
                Message message = consumer.receive(2000);
                if (message != null) {
                    message.acknowledge();
                    MQ_MESSAGE_CACHE.remove(ref.messageId());
                    MQ_MESSAGE_OBJECTS.remove(ref.messageId());
                    LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] WMQ: acknowledged {0}", ref.messageId());
                } else {
                    LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] WMQ: Error acknowledging message", e);
        }
    }

    // ====================================================================
    // FAILURE PERSISTENCE (EXCEPTS)
    // ====================================================================

    /**
    * Persist a failed FILE-mode item to EXCEPTS directory. The original processed file is copied
    * (not moved) to EXCEPTS with a suffix including the FM index and timestamp, and a sidecar .err
    * file with the reason.
    */
    public static void persistFailedFileItem(FileItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.file() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] persistFailedFileItem: null ref/file");
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

            LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] FILE EXCEPTS: failed to persist failed item", ex);
        }
    }

    /**
    * Persist a failed WMQ-mode item to EXCEPTS directory. The message body is written to a JSON
    * file named with messageId, FM index, and timestamp, and a sidecar .err file is created with
    * the reason.
    */
    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_FM%d_%s.json", sanitizedId, ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            LOGGER.log(Level.INFO, "[CsdBloombergPdAdapter] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdAdapter] WMQ EXCEPTS: failed to persist failed item", ex);
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
            LOGGER.log(Level.WARNING, "[CsdBloombergPdAdapter] Failed to write .err sidecar for {0}", dataFile);
        }
    }

    // ====================================================================
    // MQ CONFIGURATION
    // ====================================================================

    /**
    * Loads MQ properties from CsdBloombergProperties.
    */
    private static Properties loadMqProperties() {
        CsdBloombergPdProperties props = CsdBloombergPdProperties.getInstance();
        Properties mqProps = new Properties();

        mqProps.setProperty("pd.mq.host", props.getProperty("pd.mq.host", "172.22.105.46"));
        mqProps.setProperty("pd.mq.port", props.getProperty("pd.mq.port", "1414"));
        mqProps.setProperty("pd.mq.channel", props.getProperty("pd.mq.channel", "DEV.APP.SVRCONN"));
        mqProps.setProperty("pd.mq.qmgr", props.getProperty("pd.mq.qmgr", "QM_BLOOMBERG"));
        mqProps.setProperty("pd.mq.queue", props.getProperty("pd.mq.inbound.queue", "PD.INBOUND.QUEUE"));
        mqProps.setProperty("pd.mq.user", props.getProperty("pd.mq.user", ""));
        mqProps.setProperty("pd.mq.password", props.getProperty("pd.mq.password", ""));
        mqProps.setProperty("pd.mq.ack", props.getProperty("pd.mq.ack", "auto"));

        return mqProps;
    }

    private static MQConnectionFactory createMqFactory(Properties pProps) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(pProps.getProperty("pd.mq.host"));
        factory.setPort(Integer.parseInt(pProps.getProperty("pd.mq.port")));
        factory.setChannel(pProps.getProperty("pd.mq.channel"));
        factory.setQueueManager(pProps.getProperty("pd.mq.qmgr"));
        factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
        return factory;
    }

    // ====================================================================
    // RESOURCE CLEANUP
    // ====================================================================

    private static void closeQuietly(MessageConsumer c) {
        if (c != null) try {
            c.close();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "[CsdBloombergPdAdapter] close consumer ignored", e);
        }
    }

    private static void closeQuietly(Session s) {
        if (s != null) try {
            s.close();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "[CsdBloombergPdAdapter] close session ignored", e);
        }
    }

    private static void closeQuietly(Connection c) {
        if (c != null) try {
            c.close();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "[CsdBloombergPdAdapter] close connection ignored", e);
        }
    }
}