package com.cbn.bloomberg.fx;

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

import com.cbn.bloomberg.hp.CsdBloombergProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;

/**
 * Adapter manager for Bloomberg FX flow. Handles FILE and WMQ transport
 * concerns including message consumption, caching, acknowledgment, and failure
 * persistence.
 */
public final class CsdBloombergFxAdapter {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFxAdapter.class.getName());

    // Cache: JMSMessageID -> message body
    private static final ConcurrentHashMap<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();

    // Legacy placeholder to maintain compatibility if referenced elsewhere
    private static final ConcurrentHashMap<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();

    // Timestamp format for filenames
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CsdBloombergFxAdapter() {
    }

    // ====================================================================
    // FILE ADAPTER
    // ====================================================================

    /**
     * Scans directory for JSON files, reads content, counts FOREX_TRANSACTION,
     * moves files to PROCESSED, then builds IDs using the moved path.
     */
    public static List<String> extractIdsFromDirectoryAndMove(Path p_dir, String p_glob, Path p_processedDir,
            ObjectMapper p_om) {
        List<String> ids = new ArrayList<>();

        try {
            if (!Files.exists(p_processedDir)) {
                Files.createDirectories(p_processedDir);
                LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] FILE: created PROCESSED directory: {0}",
                        p_processedDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] FILE: failed to create PROCESSED directory", e);
            return ids;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p_dir, p_glob)) {
            for (Path file : stream) {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                    if (content.isEmpty()) {
                        continue;
                    }
                    JsonNode root = CsdBloombergFxMapper.normalizeRoot(p_om.readTree(content));
                    if (!CsdBloombergFxMapper.hasForexTransacts(root)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] FILE: no FOREX_TRANSACTION in {0}",
                                file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = p_processedDir
                                .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = p_processedDir
                            .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode fm = root.get("FOREX_TRANSACTION");
                    int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + target.toString() + "|FM|" + i);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE,
                            String.format("[CsdBloombergFxAdapterManager] FILE: error reading/moving %s", file), ex);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] FILE: error scanning directory", ioe);
        }

        return ids;
    }

    /**
     * Reads and normalizes JSON from a file.
     */
    public static JsonNode readRoot(Path p_file, ObjectMapper p_om) throws IOException {
        String content = new String(Files.readAllBytes(p_file), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return null;
        }
        return CsdBloombergFxMapper.normalizeRoot(p_om.readTree(content));
    }

    /**
     * Parses a FILE-mode ID string into a FileItemRef.
     */
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

    /**
     * Reference to a specific item within a FILE-mode message.
     */
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

    // ====================================================================
    // WMQ ADAPTER
    // ====================================================================

    /**
     * Consumes messages from WMQ, caches them, and returns a list of IDs.
     */
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
                        LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] WMQ: non-text message skipped");
                        continue;
                    }
                    String body = ((TextMessage) m).getText();
                    if (body == null || body.trim().isEmpty())
                        continue;
                    String msgId = m.getJMSMessageID();

                    // FIX: Strip "ID:" prefix before caching to ensure consistent key format
                    String cacheKey = msgId;
                    if (msgId != null && msgId.startsWith("ID:")) {
                        cacheKey = msgId.substring(3);
                    }
                    MQ_MESSAGE_CACHE.put(cacheKey, body);

                    try {
                        JsonNode root = CsdBloombergFxMapper.normalizeRoot(p_om.readTree(body));
                        if (CsdBloombergFxMapper.hasForexTransacts(root)) {
                            JsonNode fm = root.get("FOREX_TRANSACTION");
                            int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);

                            for (int i = 0; i < size; i++) {
                                ids.add("WMQ|" + cacheKey + "|FM|" + i);
                            }
                            LOGGER.log(Level.INFO,
                                    "[CsdBloombergFxAdapterManager] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                    new Object[] { ackMode, size, msgId });
                        } else {
                            LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] WMQ: no FOREX_TRANSACTION in {0}",
                                    msgId);
                        }
                    } catch (IOException ioe) {
                        LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] WMQ: JSON parse error", ioe);
                    }
                }

                LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] WMQ: finished consuming messages, total IDs={0}",
                        ids.size());
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException jmse) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] WMQ: JMS error consuming messages", jmse);
        } catch (RuntimeException re) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] WMQ: Runtime error", re);
        }

        return ids;
    }

    /**
     * Reads a cached MQ message body and normalizes it.
     */
    public static JsonNode readMqMessage(String p_messageId, ObjectMapper p_om) throws IOException {
        String body = MQ_MESSAGE_CACHE.get(p_messageId);
        if (body == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] WMQ: body not found for {0}", p_messageId);
            return null;
        }
        return CsdBloombergFxMapper.normalizeRoot(p_om.readTree(body));
    }

    /**
     * Parses a WMQ-mode ID string into a MqItemRef.
     */
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

    /**
     * Reference to a specific item within a WMQ message.
     */
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

    /**
     * Acknowledges a WMQ message (manual ack mode) or clears cache (auto ack mode).
     */
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
                    LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] WMQ: acknowledged {0}", ref.messageId());
                } else {
                    LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] WMQ: Error acknowledging message", e);
        }
    }

    // ====================================================================
    // FAILURE PERSISTENCE (EXCEPTS)
    // ====================================================================

    /**
     * Persist a failed FILE-mode item to EXCEPTS directory. The original processed
     * file is copied (not moved) to EXCEPTS with a suffix including the FM index
     * and timestamp, and a sidecar .err file with the reason.
     */
    public static void persistFailedFileItem(FileItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.file() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] persistFailedFileItem: null ref/file");
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
            LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] FILE EXCEPTS: failed to persist failed item", ex);
        }
    }

    /**
     * Persist a failed WMQ-mode item to EXCEPTS directory. The message body is
     * written to a JSON file named with messageId, FM index, and timestamp, and a
     * sidecar .err file is created with the reason.
     */
    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_FM%d_%s.json", sanitizedId, ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            LOGGER.log(Level.INFO, "[CsdBloombergFxAdapterManager] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxAdapterManager] WMQ EXCEPTS: failed to persist failed item", ex);
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
            LOGGER.log(Level.WARNING, "[CsdBloombergFxAdapterManager] Failed to write .err sidecar for {0}", dataFile);
        }
    }

    // ====================================================================
    // MQ CONFIGURATION
    // ====================================================================

    /**
     * Loads MQ properties from CsdBloombergProperties.
     */
    private static Properties loadMqProperties() {
        CsdBloombergProperties props = CsdBloombergProperties.getInstance();
        Properties mqProps = new Properties();

        mqProps.setProperty("mq.host", props.getProperty("mq.host", "172.105.249.157"));
        mqProps.setProperty("mq.port", props.getProperty("mq.port", "1414"));
        mqProps.setProperty("mq.channel", props.getProperty("mq.channel", "DEV.APP.SVRCONN"));
        mqProps.setProperty("mq.qmgr", props.getProperty("mq.qmgr", "QM_BLOOMBERG"));
        mqProps.setProperty("mq.queue", props.getProperty("fx.mq.inbound.queue", "FX.INBOUND.QUEUE"));
        mqProps.setProperty("mq.user", props.getProperty("mq.user", ""));
        mqProps.setProperty("mq.password", props.getProperty("mq.password", ""));
        mqProps.setProperty("mq.ack", props.getProperty("mq.ack", "auto"));

        return mqProps;
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

    // ====================================================================
    // RESOURCE CLEANUP
    // ====================================================================

    private static void closeQuietly(MessageConsumer c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFxAdapterManager] close consumer ignored", e);
            }
    }

    private static void closeQuietly(Session s) {
        if (s != null)
            try {
                s.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFxAdapterManager] close session ignored", e);
            }
    }

    private static void closeQuietly(Connection c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFxAdapterManager] close connection ignored", e);
            }
    }
}