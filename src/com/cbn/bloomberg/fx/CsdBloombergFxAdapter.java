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

import com.cbn.bloomberg.hp.CsdBloombergFxProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;


/**
 * Adapter manager for Bloomberg FX flow. Handles FILE and WMQ transport concerns including message
 * consumption, caching, acknowledgment, and failure persistence.
 */
public final class CsdBloombergFxAdapter {

    private static final Logger yLogger = Logger.getLogger(CsdBloombergFxAdapter.class.getName());

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
     * Scans directory for JSON files, reads content, counts FOREX_TRANSACTION, moves files to
     * PROCESSED, then builds IDs using the moved path.
     */
    public static List<String> scanDirectoryIds(Path pDirectory, String pDirGlob,
            Path pDirProcessed, ObjectMapper pObjMapper) {
        List<String> ids = new ArrayList<>();

        try {
            if (!Files.exists(pDirProcessed)) {
                Files.createDirectories(pDirProcessed);
                yLogger.log(Level.INFO,
                        "[CsdBloombergFxAdapter] FILE: created PROCESSED directory: {0}",
                        pDirProcessed);
            }
        } catch (IOException e) {
            yLogger.log(Level.SEVERE,
                    "[CsdBloombergFxAdapter] FILE: failed to create PROCESSED directory", e);
            return ids;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pDirectory, pDirGlob)) {
            for (Path file : stream) {
                try {
                    String content = new String(Files.readAllBytes(file),
                            StandardCharsets.UTF_8).trim();
                    if (content.isEmpty()) {
                        continue;
                    }
                    JsonNode root = CsdBloombergFxMapper.normalizeRoot(
                            pObjMapper.readTree(content));
                    if (!CsdBloombergFxMapper.hasForexTransacts(root)) {
                        yLogger.log(Level.WARNING,
                                "[CsdBloombergFxAdapter] FILE: no FOREX_TRANSACTION in {0}", file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = pDirProcessed.resolve(
                                file.getFileName().toString().replace(".json", "") + "-" + ts
                                        + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        yLogger.log(Level.INFO, "[CsdBloombergFxAdapter] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = pDirProcessed.resolve(
                            file.getFileName().toString().replace(".json", "") + "-" + ts
                                    + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    yLogger.log(Level.INFO, "[CsdBloombergFxAdapter] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode fm = root.get("FOREX_TRANSACTION");
                    int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + target.toString() + "|FM|" + i);
                    }
                } catch (Exception ex) {
                    yLogger.log(Level.SEVERE, String.format(
                            "[CsdBloombergFxAdapter] FILE: error reading/moving %s", file), ex);
                }
            }
        } catch (IOException ioe) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxAdapter] FILE: error scanning directory",
                    ioe);
        }

        return ids;
    }

    /**
     * Reads and normalizes JSON from a file.
     */
    public static JsonNode readRoot(Path pFile, ObjectMapper pObjMapper) throws IOException {
        String content = new String(Files.readAllBytes(pFile), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return null;
        }
        return CsdBloombergFxMapper.normalizeRoot(pObjMapper.readTree(content));
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
    public static List<String> extractIdsFromMq(ObjectMapper pObjMapper) {
        List<String> ids = new ArrayList<>();

        try {
            Properties props = loadMqProperties();
            MQConnectionFactory factory = createMqFactory(props);

            String user = props.getProperty("fx.wmq.username", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("fx.wmq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                String ackMode = props.getProperty("fx.wmq.ackledge", "auto").trim().toLowerCase();
                int jmsAck = "auto".equals(ackMode) ? Session.AUTO_ACKNOWLEDGE
                        : Session.CLIENT_ACKNOWLEDGE;
                session = connection.createSession(false, jmsAck);
                javax.jms.Queue queue = session.createQueue(
                        "queue:///" + props.getProperty("fx.wmq.queue"));
                consumer = session.createConsumer(queue);

                while (true) {
                    Message m = consumer.receive(1000);
                    if (m == null) break;
                    if (!(m instanceof TextMessage)) {
                        yLogger.log(Level.WARNING,
                                "[CsdBloombergFxAdapter] WMQ: non-text message skipped");
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
                        JsonNode root = CsdBloombergFxMapper.normalizeRoot(
                                pObjMapper.readTree(body));
                        if (CsdBloombergFxMapper.hasForexTransacts(root)) {
                            JsonNode fm = root.get("FOREX_TRANSACTION");
                            int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);

                            for (int i = 0; i < size; i++) {
                                ids.add("WMQ|" + cacheKey + "|FM|" + i);
                            }
                            yLogger.log(Level.INFO,
                                    "[CsdBloombergFxAdapter] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                    new Object[] { ackMode, size, msgId });
                        } else {
                            yLogger.log(Level.WARNING,
                                    "[CsdBloombergFxAdapter] WMQ: no FOREX_TRANSACTION in {0}",
                                    msgId);
                        }
                    } catch (IOException ioe) {
                        yLogger.log(Level.SEVERE, "[CsdBloombergFxAdapter] WMQ: JSON parse error",
                                ioe);
                    }
                }

                yLogger.log(Level.INFO,
                        "[CsdBloombergFxAdapter] WMQ: finished consuming messages, total IDs={0}",
                        ids.size());
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException jmse) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxAdapter] WMQ: JMS error consuming messages",
                    jmse);
        } catch (RuntimeException re) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxAdapter] WMQ: Runtime error", re);
        }

        return ids;
    }

    /**
     * Reads a cached MQ message body and normalizes it.
     */
    public static JsonNode readMqMessage(String pMessageId, ObjectMapper pObjMapper)
            throws IOException {
        String body = MQ_MESSAGE_CACHE.get(pMessageId);
        if (body == null) {
            yLogger.log(Level.WARNING, "[CsdBloombergFxAdapter] WMQ: body not found for {0}",
                    pMessageId);
            return null;
        }
        return CsdBloombergFxMapper.normalizeRoot(pObjMapper.readTree(body));
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
            String ackMode = props.getProperty("fx.wmq.ackledge", "auto").trim().toLowerCase();

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
            String user = props.getProperty("fx.wmq.user", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("fx.wmq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                javax.jms.Queue queue = session.createQueue(
                        "queue:///" + props.getProperty("fx.wmq.queue"));
                String selector = "JMSMessageID = '" + ref.messageId().replace("'", "''") + "'";

                consumer = session.createConsumer(queue, selector);
                Message message = consumer.receive(2000);
                if (message != null) {
                    message.acknowledge();
                    MQ_MESSAGE_CACHE.remove(ref.messageId());
                    MQ_MESSAGE_OBJECTS.remove(ref.messageId());
                    yLogger.log(Level.INFO, "[CsdBloombergFxAdapter] WMQ: acknowledged {0}",
                            ref.messageId());
                } else {
                    yLogger.log(Level.WARNING,
                            "[CsdBloombergFxAdapter] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException e) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxAdapter] WMQ: Error acknowledging message",
                    e);
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
            yLogger.log(Level.WARNING,
                    "[CsdBloombergFxAdapter] persistFailedFileItem: null ref/file");
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
            yLogger.log(Level.INFO, "[CsdBloombergFxAdapter] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE,
                    "[CsdBloombergFxAdapter] FILE EXCEPTS: failed to persist failed item", ex);
        }
    }

    /**
     * Persist a failed WMQ-mode item to EXCEPTS directory. The message body is written to a JSON
     * file named with messageId, FM index, and timestamp, and a sidecar .err file is created with
     * the reason.
     */
    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            yLogger.log(Level.WARNING,
                    "[CsdBloombergFxAdapter] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                yLogger.log(Level.WARNING,
                        "[CsdBloombergFxAdapter] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_FM%d_%s.json", sanitizedId, ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            yLogger.log(Level.INFO, "[CsdBloombergFxAdapter] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE,
                    "[CsdBloombergFxAdapter] WMQ EXCEPTS: failed to persist failed item", ex);
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
            yLogger.log(Level.WARNING,
                    "[CsdBloombergFxAdapter] Failed to write .err sidecar for {0}", dataFile);
        }
    }

    // ====================================================================
    // MQ CONFIGURATION
    // ====================================================================

    /**
     * Loads MQ properties from CsdBloombergProperties.
     */
    private static Properties loadMqProperties() {
        CsdBloombergFxProperties props = CsdBloombergFxProperties.getInstance();
        Properties mqProps = new Properties();

        mqProps.setProperty("fx.wmq.host", props.getProperty("fx.wmq.host", "172.22.105.46"));
        mqProps.setProperty("fx.wmq.port", props.getProperty("fx.wmq.port", "1414"));
        mqProps.setProperty("fx.wmq.channel",
                props.getProperty("fx.wmq.channel", "DEV.APP.SVRCONN"));
        mqProps.setProperty("fx.wmq.manager", props.getProperty("fx.wmq.manager", "QM_BLOOMBERG"));
        mqProps.setProperty("fx.wmq.queue",
                props.getProperty("fx.wmq.inbound.queue", "FX.INBOUND.QUEUE"));
        mqProps.setProperty("fx.wmq.username", props.getProperty("fx.wmq.username", ""));
        mqProps.setProperty("fx.wmq.password", props.getProperty("fx.wmq.password", ""));
        mqProps.setProperty("fx.wmq.ackledge", props.getProperty("fx.wmq.ackledge", "auto"));
        return mqProps;
    }

    private static MQConnectionFactory createMqFactory(Properties pProps) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();

        factory.setHostName(pProps.getProperty("fx.wmq.host"));
        factory.setPort(Integer.parseInt(pProps.getProperty("fx.wmq.port")));
        factory.setChannel(pProps.getProperty("fx.wmq.channel"));
        factory.setQueueManager(pProps.getProperty("fx.wmq.manager"));
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
            yLogger.log(Level.FINE, "[CsdBloombergFxAdapter] close consumer ignored", e);
        }
    }

    private static void closeQuietly(Session s) {
        if (s != null) try {
            s.close();
        } catch (Exception e) {
            yLogger.log(Level.FINE, "[CsdBloombergFxAdapter] close session ignored", e);
        }
    }

    private static void closeQuietly(Connection c) {
        if (c != null) try {
            c.close();
        } catch (Exception e) {
            yLogger.log(Level.FINE, "[CsdBloombergFxAdapter] close connection ignored", e);
        }
    }
}