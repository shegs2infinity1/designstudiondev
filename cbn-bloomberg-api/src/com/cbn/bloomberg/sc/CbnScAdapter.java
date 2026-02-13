package com.cbn.bloomberg.sc;

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
 * =============================================================================
 * CSD API Title: CbnScAdapter.java
 * Author: CSD Development Team
 * Created: 2026-01-07
 * Last Modified: 2026-02-03
 * =============================================================================
 *
 * PURPOSE: Adapter manager for Bloomberg SC (Securities) flow. Handles FILE and WMQ
 * transport concerns including message consumption, caching, acknowledgment, and
 * failure persistence for both SECURITY_MASTER and SEC_TRADE message types.
 *
 * IMPORTANT: This adapter uses browse-then-selective-consume pattern via CbnTfBrowsing
 * to only process messages containing SECURITY_MASTER or SEC_TRADE, leaving other
 * module messages (FX, FT, PD, PR) on the queue for their respective batch jobs.
 *
 * MODIFICATION HISTORY:
 * - 2026-01-07 | Initial creation for SECURITY_MASTER
 * - 2026-02-03 | Added SEC_TRADE support (dual module browsing, ST ID parsing)
 * =============================================================================
 */
public final class CbnScAdapter {

    private static final Logger yLogger = Logger.getLogger(CbnScAdapter.class.getName());

    /** The module types this adapter handles */
    private static final ModuleType MODULE_SC = ModuleType.SC; // SECURITY_MASTER
    private static final ModuleType MODULE_ST = ModuleType.ST; // SEC_TRADE

    // Cache: JMSMessageID -> message body
    private static final ConcurrentHashMap<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();

    // Legacy placeholder to maintain compatibility if referenced elsewhere
    private static final ConcurrentHashMap<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();

    // Timestamp format for filenames
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String WMQ_QUE = "tf.wmq.queue";
    private static final String WMQ_USR = "tf.wmq.username";
    private static final String WMQ_KEY = "tf.wmq.password";
    private static final String WMQ_ACK = "tf.wmq.ackledge";

    private CbnScAdapter() {
    }

    // ====================================================================
    // FILE ADAPTER
    // ====================================================================

    /**
     * Scans directory for JSON files, reads content, counts SECURITY_MASTER and SEC_TRADE,
     * moves files to PROCESSED, then builds IDs using the moved path.
     */
    public static List<String> scanDirectoryIds(Path pDirectory, String pDirGlob,
            Path pDirProcessed, ObjectMapper pObjMapper) {
        List<String> ids = new ArrayList<>();

        try {
            if (!Files.exists(pDirProcessed)) {
                Files.createDirectories(pDirProcessed);
                yLogger.log(Level.INFO, "[CbnScAdapter] FILE: created PROCESSED directory: {0}",
                        pDirProcessed);
            }
        } catch (IOException e) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] FILE: failed to create PROCESSED directory",
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

                    // Check for either SECURITY_MASTER or SEC_TRADE
                    boolean hasSc = MODULE_SC.hasTransaction(root);
                    boolean hasSt = MODULE_ST.hasTransaction(root);

                    if (!hasSc && !hasSt) {
                        yLogger.log(Level.WARNING,
                                "[CbnScAdapter] FILE: no SECURITY_MASTER or SEC_TRADE in {0}",
                                file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = pDirProcessed.resolve(
                                file.getFileName().toString().replace(".json", "") + "-" + ts
                                        + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        yLogger.log(Level.INFO, "[CbnScAdapter] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = pDirProcessed.resolve(
                            file.getFileName().toString().replace(".json", "") + "-" + ts
                                    + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    yLogger.log(Level.INFO, "[CbnScAdapter] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    // Build IDs for SECURITY_MASTER
                    if (hasSc) {
                        JsonNode txnNode = MODULE_SC.getTransaction(root);
                        int size = CbnTfBrowsing.countTransactionItems(txnNode);
                        for (int i = 0; i < size; i++) {
                            ids.add("FILE|" + target.toString() + "|" + MODULE_SC.getIdPrefix()
                                    + "|" + i);
                        }
                        yLogger.log(Level.INFO,
                                "[CbnScAdapter] FILE: found {0} SECURITY_MASTER items", size);
                    }

                    // Build IDs for SEC_TRADE
                    if (hasSt) {
                        JsonNode txnNode = MODULE_ST.getTransaction(root);
                        int size = CbnTfBrowsing.countTransactionItems(txnNode);
                        for (int i = 0; i < size; i++) {
                            ids.add("FILE|" + target.toString() + "|" + MODULE_ST.getIdPrefix()
                                    + "|" + i);
                        }
                        yLogger.log(Level.INFO, "[CbnScAdapter] FILE: found {0} SEC_TRADE items",
                                size);
                    }

                } catch (Exception ex) {
                    yLogger.log(Level.SEVERE,
                            String.format("[CbnScAdapter] FILE: error reading/moving %s", file),
                            ex);
                }
            }
        } catch (IOException ioe) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] FILE: error scanning directory", ioe);
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
     * Supports both SC (SECURITY_MASTER) and ST (SEC_TRADE) prefixes.
     */
    public static FileItemRef parseFileItemRef(String pId) {
        if (pId == null || !pId.startsWith("FILE|")) return null;
        String[] parts = pId.split("\\|", 4);
        if (parts.length != 4) return null;

        // Accept both SC and ST prefixes
        String prefix = parts[2];
        if (!MODULE_SC.getIdPrefix().equals(prefix) && !MODULE_ST.getIdPrefix().equals(prefix)) {
            return null;
        }

        try {
            int idx = Integer.parseInt(parts[3]);
            return new FileItemRef(Paths.get(parts[1]), idx, prefix);
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
        private final String mModulePrefix;

        FileItemRef(Path f, int i, String modulePrefix) {
            this.mFile = f;
            this.mIndex = i;
            this.mModulePrefix = modulePrefix;
        }

        public Path file() {
            return mFile;
        }

        public int index() {
            return mIndex;
        }

        public String modulePrefix() {
            return mModulePrefix;
        }

        public boolean isSecurityMaster() {
            return MODULE_SC.getIdPrefix().equals(mModulePrefix);
        }

        public boolean isSecTrade() {
            return MODULE_ST.getIdPrefix().equals(mModulePrefix);
        }
    }

    // ====================================================================
    // WMQ ADAPTER - BROWSE-THEN-SELECTIVE-CONSUME PATTERN
    // ====================================================================

    /**
     * Browses the queue for messages containing SECURITY_MASTER or SEC_TRADE, then selectively
     * consumes only those messages. Messages for other modules (FX, FT, PD, PR) are
     * left on the queue for their respective batch jobs.
     */
    public static List<String> extractIdsFromWmq(ObjectMapper pObjMapper) {
        List<String> ids = new ArrayList<>();

        Connection connection = null;
        try {
            Properties props = loadMqProperties();
            MQConnectionFactory factory = createMqFactory(props);

            String user = props.getProperty(WMQ_USR, "").trim();
            connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty(WMQ_KEY, ""));
            connection.start();

            String queueName = props.getProperty(WMQ_QUE);

            // Phase 1a: Browse to identify SC (SECURITY_MASTER) messages
            BrowseResult browseResultSc = CbnTfBrowsing.browseForModule(connection, queueName,
                    MODULE_SC, pObjMapper);

            yLogger.log(Level.INFO,
                    "[CbnScAdapter] WMQ: browse found {0} SC messages out of {1} total",
                    new Object[] { browseResultSc.getMatchCount(),
                            browseResultSc.getTotalBrowsed() });

            // Phase 1b: Browse to identify ST (SEC_TRADE) messages
            BrowseResult browseResultSt = CbnTfBrowsing.browseForModule(connection, queueName,
                    MODULE_ST, pObjMapper);

            yLogger.log(Level.INFO,
                    "[CbnScAdapter] WMQ: browse found {0} ST messages out of {1} total",
                    new Object[] { browseResultSt.getMatchCount(),
                            browseResultSt.getTotalBrowsed() });

            // Phase 2a: Selectively consume SC messages
            if (browseResultSc.hasMatches()) {
                ids.addAll(consumeMatchingMessages(connection, props,
                        browseResultSc.getMatchingMessageIds(), pObjMapper, MODULE_SC));
            }

            // Phase 2b: Selectively consume ST messages
            if (browseResultSt.hasMatches()) {
                ids.addAll(consumeMatchingMessages(connection, props,
                        browseResultSt.getMatchingMessageIds(), pObjMapper, MODULE_ST));
            }

        } catch (JMSException jmse) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] WMQ: JMS error during browse/consume", jmse);
        } catch (RuntimeException re) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] WMQ: Runtime error", re);
        } finally {
            CbnTfBrowsing.closeQuietly(connection);
        }

        yLogger.log(Level.INFO, "[CbnScAdapter] WMQ: finished processing, total IDs generated={0}",
                ids.size());
        return ids;
    }

    /**
     * Consumes messages by their JMSMessageID using selector-based retrieval.
     * Only messages identified during browse phase are consumed.
     */
    private static List<String> consumeMatchingMessages(Connection pConnection, Properties pProps,
            List<String> pMatchingIds, ObjectMapper pObjMapper, ModuleType pModule)
            throws JMSException {

        List<String> ids = new ArrayList<>();
        String ackMode = pProps.getProperty(WMQ_ACK, "auto").trim().toLowerCase();
        int jmsAck = "auto".equals(ackMode) ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE;

        Session consumeSession = null;
        try {
            consumeSession = pConnection.createSession(false, jmsAck);
            javax.jms.Queue queue = consumeSession.createQueue(
                    "queue:///" + pProps.getProperty(WMQ_QUE));

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
                                "[CbnScAdapter] WMQ: message {0} no longer available", msgId);
                        continue;
                    }

                    if (!(m instanceof TextMessage)) {
                        continue;
                    }

                    String body = ((TextMessage) m).getText();
                    if (body == null || body.trim().isEmpty()) {
                        continue;
                    }

                    // Normalize cache key using shared utility
                    String cacheKey = CbnTfBrowsing.normalizeCacheKey(msgId);
                    MQ_MESSAGE_CACHE.put(cacheKey, body);

                    try {
                        JsonNode root = CbnTfBrowsing.normalizeRoot(pObjMapper.readTree(body));
                        JsonNode txnNode = pModule.getTransaction(root);
                        int size = CbnTfBrowsing.countTransactionItems(txnNode);

                        for (int i = 0; i < size; i++) {
                            ids.add("WMQ|" + cacheKey + "|" + pModule.getIdPrefix() + "|" + i);
                        }

                        yLogger.log(Level.INFO,
                                "[CbnScAdapter] WMQ: consumed (ack={0}) {1} {2} item(s) from {3}",
                                new Object[] { ackMode, size, pModule.name(), msgId });

                    } catch (IOException ioe) {
                        yLogger.log(Level.SEVERE, "[CbnScAdapter] WMQ: JSON parse error on consume",
                                ioe);
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
            yLogger.log(Level.WARNING, "[CbnScAdapter] WMQ: body not found for {0}", pMessageId);
            return null;
        }
        return CbnTfBrowsing.normalizeRoot(pObjMapper.readTree(body));
    }

    /**
     * Parses a WMQ-mode ID string into a MqItemRef.
     * Supports both SC (SECURITY_MASTER) and ST (SEC_TRADE) prefixes.
     */
    public static MqItemRef parseMqItemRef(String pId) {
        if (pId == null || !pId.startsWith("WMQ|")) return null;
        String[] parts = pId.split("\\|", 4);
        if (parts.length != 4) return null;

        // Accept both SC and ST prefixes
        String prefix = parts[2];
        if (!MODULE_SC.getIdPrefix().equals(prefix) && !MODULE_ST.getIdPrefix().equals(prefix)) {
            return null;
        }

        try {
            int idx = Integer.parseInt(parts[3]);
            return new MqItemRef(parts[1], idx, prefix);
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
        private final String mModulePrefix;

        MqItemRef(String mid, int i, String modulePrefix) {
            this.mMessageId = mid;
            this.mIndex = i;
            this.mModulePrefix = modulePrefix;
        }

        public String messageId() {
            return mMessageId;
        }

        public int index() {
            return mIndex;
        }

        public String modulePrefix() {
            return mModulePrefix;
        }

        public boolean isSecurityMaster() {
            return MODULE_SC.getIdPrefix().equals(mModulePrefix);
        }

        public boolean isSecTrade() {
            return MODULE_ST.getIdPrefix().equals(mModulePrefix);
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
            String user = props.getProperty(WMQ_USR, "").trim();
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
                    yLogger.log(Level.INFO, "[CbnScAdapter] WMQ: acknowledged {0}",
                            ref.messageId());
                } else {
                    yLogger.log(Level.WARNING, "[CbnScAdapter] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                CbnTfBrowsing.closeQuietly(consumer);
                CbnTfBrowsing.closeQuietly(session);
                CbnTfBrowsing.closeQuietly(connection);
            }
        } catch (JMSException e) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] WMQ: Error acknowledging message", e);
        }
    }

    // ====================================================================
    // FAILURE PERSISTENCE (EXCEPTS)
    // ====================================================================

    /**
     * Persist a failed FILE-mode item to EXCEPTS directory.
     */
    public static void persistFailedFileItem(FileItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.file() == null) {
            yLogger.log(Level.WARNING, "[CbnScAdapter] persistFailedFileItem: null ref/file");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String baseName = ref.file().getFileName().toString().replace(".json", "");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("%s_%s%d_%s.json", baseName, ref.modulePrefix(),
                    ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.copy(ref.file(), outFile, StandardCopyOption.REPLACE_EXISTING);
            writeReasonSidecar(outFile, reason);
            yLogger.log(Level.INFO, "[CbnScAdapter] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] FILE EXCEPTS: failed to persist failed item",
                    ex);
        }
    }

    /**
     * Persist a failed WMQ-mode item to EXCEPTS directory.
     */
    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            yLogger.log(Level.WARNING, "[CbnScAdapter] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                yLogger.log(Level.WARNING, "[CbnScAdapter] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_%s%d_%s.json", sanitizedId, ref.modulePrefix(),
                    ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            yLogger.log(Level.INFO, "[CbnScAdapter] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            yLogger.log(Level.SEVERE, "[CbnScAdapter] WMQ EXCEPTS: failed to persist failed item",
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
            yLogger.log(Level.WARNING, "[CbnScAdapter] Failed to write .err sidecar for {0}",
                    dataFile);
        }
    }

    // ====================================================================
    // MQ CONFIGURATION
    // ====================================================================

    private static Properties loadMqProperties() {
        CbnTfProperties props = CbnTfProperties.getInstance();
        Properties mqProps = new Properties();

        mqProps.setProperty("tf.wmq.host", props.getProperty("tf.wmq.host", "172.22.105.46"));
        mqProps.setProperty("tf.wmq.port", props.getProperty("tf.wmq.port", "1414"));
        mqProps.setProperty("tf.wmq.channel",
                props.getProperty("tf.wmq.channel", "DEV.APP.SVRCONN"));
        mqProps.setProperty("tf.wmq.manager", props.getProperty("tf.wmq.manager", "QM_BLOOMBERG"));
        mqProps.setProperty("tf.wmq.queue",
                props.getProperty("tf.wmq.inbound.queue", "TF.INBOUND.QUEUE"));
        mqProps.setProperty(WMQ_USR, props.getProperty(WMQ_USR, ""));
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
