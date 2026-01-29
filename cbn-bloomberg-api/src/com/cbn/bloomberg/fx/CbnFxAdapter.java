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

import com.cbn.bloomberg.util.CbnTfBrowsing;
import com.cbn.bloomberg.util.CbnTfBrowsing.BrowseResult;
import com.cbn.bloomberg.util.CbnTfBrowsing.ModuleType;
import com.cbn.bloomberg.util.CbnTfProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;


/**
 * Adapter manager for Bloomberg FX flow. Handles FILE and WMQ transport concerns including message
 * consumption, caching, acknowledgment, and failure persistence.
 * 
 * IMPORTANT: This adapter uses browse-then-selective-consume pattern via CbnTfBrowsing
 * to only process messages containing FOREX_TRANSACTION, leaving other module messages 
 * (FT, PD, PR, SC) on the queue for their respective batch jobs.
 */
public final class CbnFxAdapter {

    private static final Logger yLogger = Logger.getLogger(CbnFxAdapter.class.getName());

    /** The module type this adapter handles */
    private static final ModuleType MODULE = ModuleType.FX;

    // Cache: JMSMessageID -> message body
    private static final ConcurrentHashMap<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();

    // Legacy placeholder to maintain compatibility if referenced elsewhere
    private static final ConcurrentHashMap<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();

    // Timestamp format for filenames
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CbnFxAdapter() {
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
                yLogger.log(Level.INFO, "[CbnFxAdapter] FILE: created PROCESSED directory: {0}",
                        pDirProcessed);
            }
        } catch (IOException e) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] FILE: failed to create PROCESSED directory",
                    e);
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
                    JsonNode root = CbnTfBrowsing.normalizeRoot(pObjMapper.readTree(content));
                    if (!MODULE.hasTransaction(root)) {
                        yLogger.log(Level.WARNING,
                                "[CbnFxAdapter] FILE: no {0} in {1}", 
                                new Object[] { MODULE.getJsonRootNode(), file });
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = pDirProcessed.resolve(
                                file.getFileName().toString().replace(".json", "") + "-" + ts
                                        + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        yLogger.log(Level.INFO, "[CbnFxAdapter] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = pDirProcessed.resolve(
                            file.getFileName().toString().replace(".json", "") + "-" + ts
                                    + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    yLogger.log(Level.INFO, "[CbnFxAdapter] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode txnNode = MODULE.getTransaction(root);
                    int size = CbnTfBrowsing.countTransactionItems(txnNode);
                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + target.toString() + "|" + MODULE.getIdPrefix() + "|" + i);
                    }
                } catch (Exception ex) {
                    yLogger.log(Level.SEVERE,
                            String.format("[CbnFxAdapter] FILE: error reading/moving %s", file),
                            ex);
                }
            }
        } catch (IOException ioe) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] FILE: error scanning directory", ioe);
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
        return CbnTfBrowsing.normalizeRoot(pObjMapper.readTree(content));
    }

    /**
     * Parses a FILE-mode ID string into a FileItemRef.
     */
    public static FileItemRef parseFileItemRef(String pId) {
        if (pId == null || !pId.startsWith("FILE|")) return null;
        String[] parts = pId.split("\\|", 4);
        if (parts.length != 4 || !MODULE.getIdPrefix().equals(parts[2])) return null;
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
    // WMQ ADAPTER - BROWSE-THEN-SELECTIVE-CONSUME PATTERN
    // ====================================================================

    /**
     * Browses the queue for messages containing FOREX_TRANSACTION, then selectively
     * consumes only those messages. Messages for other modules (FT, PD, PR, SC) are
     * left on the queue for their respective batch jobs.
     * 
     * Uses CbnTfBrowsing for the browse phase to ensure consistent behavior
     * across all module adapters.
     */
    public static List<String> extractIdsFromWmq(ObjectMapper pObjMapper) {
        List<String> ids = new ArrayList<>();

        Connection connection = null;
        try {
            Properties props = loadMqProperties();
            MQConnectionFactory factory = createMqFactory(props);

            String user = props.getProperty("tf.wmq.username", "").trim();
            connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("tf.wmq.password", ""));
            connection.start();

            String queueName = props.getProperty("tf.wmq.queue");

            // Phase 1: Browse to identify FX messages using shared utility
            BrowseResult browseResult = CbnTfBrowsing.browseForModule(
                    connection, queueName, MODULE, pObjMapper);

            yLogger.log(Level.INFO,
                    "[CbnFxAdapter] WMQ: browse found {0} FX messages out of {1} total",
                    new Object[] { browseResult.getMatchCount(), browseResult.getTotalBrowsed() });

            // Phase 2: Selectively consume matching messages
            if (browseResult.hasMatches()) {
                ids = consumeMatchingMessages(connection, props, 
                        browseResult.getMatchingMessageIds(), pObjMapper);
            }

        } catch (JMSException jmse) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] WMQ: JMS error during browse/consume", jmse);
        } catch (RuntimeException re) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] WMQ: Runtime error", re);
        } finally {
            CbnTfBrowsing.closeQuietly(connection);
        }

        yLogger.log(Level.INFO,
                "[CbnFxAdapter] WMQ: finished processing, total IDs generated={0}", ids.size());
        return ids;
    }

    /**
     * Consumes messages by their JMSMessageID using selector-based retrieval.
     * Only messages identified during browse phase are consumed.
     * Handles both TextMessage and BytesMessage formats.
     */
    private static List<String> consumeMatchingMessages(Connection pConnection, Properties pProps,
            List<String> pMatchingIds, ObjectMapper pObjMapper) throws JMSException {
        
        List<String> ids = new ArrayList<>();
        String ackMode = pProps.getProperty("tf.wmq.ackledge", "auto").trim().toLowerCase();
        int jmsAck = "auto".equals(ackMode) ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE;

        Session consumeSession = null;
        try {
            consumeSession = pConnection.createSession(false, jmsAck);
            javax.jms.Queue queue = consumeSession.createQueue(
                    "queue:///" + pProps.getProperty("tf.wmq.queue"));

            for (String msgId : pMatchingIds) {
                MessageConsumer consumer = null;
                try {
                    // Build selector using shared utility
                    String selector = CbnTfBrowsing.buildMessageSelector(msgId);
                    consumer = consumeSession.createConsumer(queue, selector);

                    // Short timeout since we know the message exists
                    Message m = consumer.receive(2000);
                    
                    if (m == null) {
                        yLogger.log(Level.WARNING,
                                "[CbnFxAdapter] WMQ: message {0} no longer available", msgId);
                        continue;
                    }

                    // Extract message body - handle both TextMessage and BytesMessage
                    String body = null;
                    if (m instanceof TextMessage) {
                        body = ((TextMessage) m).getText();
                        yLogger.log(Level.FINE, "[CbnFxAdapter] WMQ: processing TextMessage {0}", msgId);
                    } else if (m instanceof javax.jms.BytesMessage) {
                        try {
                            javax.jms.BytesMessage bytesMsg = (javax.jms.BytesMessage) m;
                            byte[] bytes = new byte[(int) bytesMsg.getBodyLength()];
                            bytesMsg.readBytes(bytes);
                            body = new String(bytes, StandardCharsets.UTF_8);
                            yLogger.log(Level.FINE, "[CbnFxAdapter] WMQ: processing BytesMessage {0}", msgId);
                        } catch (JMSException jmsEx) {
                            yLogger.log(Level.SEVERE,
                                    "[CbnFxAdapter] WMQ: failed to read BytesMessage {0}", msgId);
                            continue;
                        }
                    } else {
                        yLogger.log(Level.WARNING,
                                "[CbnFxAdapter] WMQ: unsupported message type {0} for {1}",
                                new Object[] { m.getClass().getSimpleName(), msgId });
                        continue;
                    }

                    if (body == null || body.trim().isEmpty()) {
                        yLogger.log(Level.WARNING,
                                "[CbnFxAdapter] WMQ: empty body for message {0}", msgId);
                        continue;
                    }

                    // Normalize cache key using shared utility
                    String cacheKey = CbnTfBrowsing.normalizeCacheKey(msgId);
                    MQ_MESSAGE_CACHE.put(cacheKey, body);

                    try {
                        JsonNode root = CbnTfBrowsing.normalizeRoot(pObjMapper.readTree(body));
                        JsonNode txnNode = MODULE.getTransaction(root);
                        int size = CbnTfBrowsing.countTransactionItems(txnNode);

                        for (int i = 0; i < size; i++) {
                            ids.add("WMQ|" + cacheKey + "|" + MODULE.getIdPrefix() + "|" + i);
                        }

                        yLogger.log(Level.INFO,
                                "[CbnFxAdapter] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                new Object[] { ackMode, size, msgId });

                    } catch (IOException ioe) {
                        yLogger.log(Level.SEVERE, 
                                "[CbnFxAdapter] WMQ: JSON parse error on consume for {0}", msgId);
                        yLogger.log(Level.SEVERE, "Body preview: " + 
                                body.substring(0, Math.min(200, body.length())), ioe);
                    }

                } finally {
                    CbnTfBrowsing.closeQuietly(consumer);
                }
            }
        } finally {
            CbnTfBrowsing.closeQuietly(consumeSession);
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
            yLogger.log(Level.WARNING, "[CbnFxAdapter] WMQ: body not found for {0}", pMessageId);
            return null;
        }
        return CbnTfBrowsing.normalizeRoot(pObjMapper.readTree(body));
    }

    /**
     * Parses a WMQ-mode ID string into a MqItemRef.
     */
    public static MqItemRef parseMqItemRef(String pId) {
        if (pId == null || !pId.startsWith("WMQ|")) return null;
        String[] parts = pId.split("\\|", 4);
        if (parts.length != 4 || !MODULE.getIdPrefix().equals(parts[2])) return null;
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
            String ackMode = props.getProperty("tf.wmq.ackledge", "auto").trim().toLowerCase();

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
            String user = props.getProperty("tf.wmq.username", "").trim();
            Connection connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("tf.wmq.password", ""));

            Session session = null;
            MessageConsumer consumer = null;
            try {
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                javax.jms.Queue queue = session.createQueue(
                        "queue:///" + props.getProperty("tf.wmq.queue"));
                String selector = CbnTfBrowsing.buildMessageSelector(ref.messageId());

                consumer = session.createConsumer(queue, selector);
                Message message = consumer.receive(2000);
                if (message != null) {
                    message.acknowledge();
                    MQ_MESSAGE_CACHE.remove(ref.messageId());
                    MQ_MESSAGE_OBJECTS.remove(ref.messageId());
                    yLogger.log(Level.INFO, "[CbnFxAdapter] WMQ: acknowledged {0}",
                            ref.messageId());
                } else {
                    yLogger.log(Level.WARNING, "[CbnFxAdapter] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                CbnTfBrowsing.closeQuietly(consumer);
                CbnTfBrowsing.closeQuietly(session);
                CbnTfBrowsing.closeQuietly(connection);
            }
        } catch (JMSException e) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] WMQ: Error acknowledging message", e);
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
            yLogger.log(Level.WARNING, "[CbnFxAdapter] persistFailedFileItem: null ref/file");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String baseName = ref.file().getFileName().toString().replace(".json", "");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("%s_%s%d_%s.json", baseName, MODULE.getIdPrefix(), 
                    ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.copy(ref.file(), outFile, StandardCopyOption.REPLACE_EXISTING);
            writeReasonSidecar(outFile, reason);
            yLogger.log(Level.INFO, "[CbnFxAdapter] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] FILE EXCEPTS: failed to persist failed item",
                    ex);
        }
    }

    /**
     * Persist a failed WMQ-mode item to EXCEPTS directory. The message body is written to a JSON
     * file named with messageId, FM index, and timestamp, and a sidecar .err file is created with
     * the reason.
     */
    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            yLogger.log(Level.WARNING, "[CbnFxAdapter] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                yLogger.log(Level.WARNING, "[CbnFxAdapter] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_%s%d_%s.json", sanitizedId, MODULE.getIdPrefix(),
                    ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            yLogger.log(Level.INFO, "[CbnFxAdapter] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE, "[CbnFxAdapter] WMQ EXCEPTS: failed to persist failed item",
                    ex);
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
            yLogger.log(Level.WARNING, "[CbnFxAdapter] Failed to write .err sidecar for {0}",
                    dataFile);
        }
    }

    // ====================================================================
    // MQ CONFIGURATION
    // ====================================================================

    /**
     * Loads MQ properties from CsdBloombergProperties.
     */
    private static Properties loadMqProperties() {
        CbnTfProperties props = CbnTfProperties.getInstance();
        Properties mqProps = new Properties();

        mqProps.setProperty("tf.wmq.host", props.getProperty("tf.wmq.host", "172.22.105.46"));
        mqProps.setProperty("tf.wmq.port", props.getProperty("tf.wmq.port", "1414"));
        mqProps.setProperty("tf.wmq.channel",
                props.getProperty("tf.wmq.channel", "DEV.APP.SVRCONN"));
        mqProps.setProperty("tf.wmq.manager", props.getProperty("tf.wmq.manager", "QM_BLOOMBERG"));
        mqProps.setProperty("tf.wmq.queue",
                props.getProperty("tf.wmq.inbound.queue", "BLOOMBERG.INBOUND.QUEUE"));
        mqProps.setProperty("tf.wmq.username", props.getProperty("tf.wmq.username", ""));
        mqProps.setProperty("tf.wmq.password", props.getProperty("tf.wmq.password", ""));
        mqProps.setProperty("tf.wmq.ackledge", props.getProperty("tf.wmq.ackledge", "auto"));
        return mqProps;
    }

    private static MQConnectionFactory createMqFactory(Properties pProps) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();

        factory.setHostName(pProps.getProperty("tf.wmq.host"));
        factory.setPort(Integer.parseInt(pProps.getProperty("tf.wmq.port")));
        factory.setChannel(pProps.getProperty("tf.wmq.channel"));
        factory.setQueueManager(pProps.getProperty("tf.wmq.manager"));
        factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
        return factory;
    }
}