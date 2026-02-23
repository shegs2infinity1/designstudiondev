package com.cbn.bloomberg.fx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;

/**
 * Mapper and adapter utilities for Bloomberg FX flow. Provides FILE and WMQ
 * adapters plus JSON→FX field mapping helpers.
 */
public final class CsdBloombergFxMapping {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFxMapping.class.getName());

    // Cache: JMSMessageID -> message body
    private static final Map<String, String> MQ_MESSAGE_CACHE = new ConcurrentHashMap<>();

    // Legacy placeholder to maintain compatibility if referenced elsewhere
    private static final Map<String, Message> MQ_MESSAGE_OBJECTS = new ConcurrentHashMap<>();

    // Timestamp format for filenames
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String normalizeDateT24(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }
        try {
            return LocalDate.parse(dateStr, INPUT_FMT).format(OUTPUT_FMT);
        } catch (Exception e) {
            // handle or log invalid date format if needed
            return dateStr;
        }
    }

    private CsdBloombergFxMapping() {
    }

    // ====
    // ==== FILE ADAPTER ====
    // ====

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
                LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] FILE: created PROCESSED directory: {0}",
                        p_processedDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] FILE: failed to create PROCESSED directory", e);
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
                    if (!hasForexTransacts(root)) {
                        LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] FILE: no FOREX_TRANSACTION in {0}", file);
                        // Move empty/invalid files as well to avoid re-scans
                        String ts = LocalDateTime.now().format(TS_FMT);
                        Path target = p_processedDir
                                .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] FILE: moved {0} -> {1}",
                                new Object[] { file, target });
                        continue;
                    }

                    // Move first, then build IDs from the moved path
                    String ts = LocalDateTime.now().format(TS_FMT);
                    Path target = p_processedDir
                            .resolve(file.getFileName().toString().replace(".json", "") + "-" + ts + ".json");
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] FILE: moved {0} -> {1}",
                            new Object[] { file, target });

                    JsonNode fm = root.get("FOREX_TRANSACTION");
                    int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + target.toString() + "|FM|" + i);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] FILE: error reading/moving " + file, ex);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] FILE: error scanning directory", ioe);
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

    // ====
    // ==== WMQ ADAPTER ====
    // ====

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
                        LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] WMQ: non-text message skipped");
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
                        if (hasForexTransacts(root)) {
                            JsonNode fm = root.get("FOREX_TRANSACTION");
                            int size = fm.isArray() ? fm.size() : (fm.isObject() ? 1 : 0);
                            for (int i = 0; i < size; i++) {
                                ids.add("WMQ|" + msgId + "|FM|" + i);
                            }
                            LOGGER.log(Level.INFO,
                                    "[CsdBloombergFxMappers] WMQ: consumed (ack={0}) {1} item(s) from {2}",
                                    new Object[] { ackMode, size, msgId });
                        } else {
                            LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] WMQ: no FOREX_TRANSACTION in {0}",
                                    msgId);
                        }
                    } catch (IOException ioe) {
                        LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] WMQ: JSON parse error", ioe);
                    }
                }

                LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] WMQ: finished consuming messages, total IDs={0}",
                        ids.size());
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException jmse) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] WMQ: JMS error consuming messages", jmse);
        } catch (RuntimeException re) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] WMQ: Runtime error", re);
        }

        return ids;
    }

    public static JsonNode readMqMessage(String p_messageId, ObjectMapper p_om) throws IOException {
        String body = MQ_MESSAGE_CACHE.get(p_messageId);
        if (body == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] WMQ: body not found for {0}", p_messageId);
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
                    LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] WMQ: acknowledged {0}", ref.messageId());
                } else {
                    LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] WMQ: nothing to acknowledge for {0}",
                            ref.messageId());
                }
            } finally {
                closeQuietly(consumer);
                closeQuietly(session);
                closeQuietly(connection);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] WMQ: Error acknowledging message", e);
        }
    }

    // ====
    // ==== FAILURE PERSISTENCE HELPERS (EXCEPTS) ====
    // ====

    /**
     * Persist a failed FILE-mode item to EXCEPTS directory. The original processed
     * file is copied (not moved) to EXCEPTS with a suffix including the FM index
     * and timestamp, and a sidecar .err file with the reason.
     */
    public static void persistFailedFileItem(FileItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.file() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] persistFailedFileItem: null ref/file");
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
            LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] FILE EXCEPTS: copied {0} -> {1}",
                    new Object[] { ref.file(), outFile });
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] FILE EXCEPTS: failed to persist failed item", ex);
        }
    }

    /**
     * Persist a failed WMQ-mode item to EXCEPTS directory. The message body is
     * written to a JSON file named with messageId, FM index, and timestamp, and a
     * sidecar .err file is created with the reason.
     */
    public static void persistFailedMqItem(MqItemRef ref, Path exceptsDir, String reason) {
        if (ref == null || ref.messageId() == null) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] persistFailedMqItem: null ref/messageId");
            return;
        }
        try {
            ensureDir(exceptsDir);
            String body = MQ_MESSAGE_CACHE.get(ref.messageId());
            if (body == null || body.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] WMQ EXCEPTS: no body in cache for {0}",
                        ref.messageId());
                return;
            }
            String sanitizedId = ref.messageId().replaceAll("[^A-Za-z0-9_\\-]", "_");
            String ts = LocalDateTime.now().format(TS_FMT);
            String outName = String.format("WMQ_%s_FM%d_%s.json", sanitizedId, ref.index(), ts);
            Path outFile = exceptsDir.resolve(outName);
            Files.write(outFile, body.getBytes(StandardCharsets.UTF_8));
            writeReasonSidecar(outFile, reason);
            LOGGER.log(Level.INFO, "[CsdBloombergFxMappers] WMQ EXCEPTS: wrote {0}", outFile);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxMappers] WMQ EXCEPTS: failed to persist failed item", ex);
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
            LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] Failed to write .err sidecar for {0}", dataFile);
        }
    }

    // ====
    // ==== JSON HELPERS ====
    // ====

    public static boolean hasForexTransacts(JsonNode p_root) {
        if (p_root == null)
            return false;
        JsonNode fm = p_root.get("FOREX_TRANSACTION");
        if (fm == null || fm.isNull())
            return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    public static JsonNode getForexTransactAt(JsonNode p_root, int p_index) {
        JsonNode fm = p_root.get("FOREX_TRANSACTION");
        if (fm == null)
            return null;
        if (fm.isArray())
            return fm.get(p_index);
        return fm.isObject() ? fm : null;
    }

    /**
     * Map a single funds movement node to FX field map. Only basic fields are
     * mapped here; extend as needed.
     */
    public static Map<String, String> mapForexTransactToFx(JsonNode p_fm) {

        // Mapping incoming bloomberg json feed to Forex Trading Module
        Map<String, String> map = new HashMap<>();

        // Global fields
        String crParty = asText(p_fm, "COUNTERPARTY");
        String dealtype = asText(p_fm, "DEAL_TYPE");
        String dealDesk = asText(p_fm, "DEALER_DESK");
        String dealdate = asText(p_fm, "DEAL_DATE");
        String ccyBuy = asText(p_fm, "CURRENCY_BOUGHT");
        String buyAmt = asText(p_fm, "BUY_AMOUNT");
        String vDateBuy = asText(p_fm, "VALUE_DATE_BUY");
        String ccySold = asText(p_fm, "CURRENCY_SOLD");
        String sellAmt = asText(p_fm, "SELL_AMOUNT");
        String vDateSell = asText(p_fm, "VALUE_DATE_SELL");
        String sptRate = asText(p_fm, "SPOT_RATE");
        String sptDate = asText(p_fm, "SPOT_DATE");
        String baseCcy = asText(p_fm, "BASE_CCY");
        String dBroker = asText(p_fm, "BROKER");
        String dlrNotes = asText(p_fm, "DEALER_NOTES");
        String ourAcPay = asText(p_fm, "OUR_ACCOUNT_PAY");
        String ourAcRec = asText(p_fm, "OUR_ACCOUNT_REC");
        String cCorrNum = asText(p_fm, "CPARTY_CORR_NO");
        String cCorrAdd = asText(p_fm, "CPY_CORR_ADD");
        String cpBnkAcc = asText(p_fm, "CPARTY_BANK_ACC");
        String bkbkInfo = asText(p_fm, "BK_TO_BK_INF");
        String iRateBuy = asText(p_fm, "INT_RATE_BUY");
        String iRateSel = asText(p_fm, "INT_RATE_SELL");

        map.put("CPTY", safe(crParty));
        map.put("DTYP", safe(dealtype));
        map.put("DESK", safe(dealDesk));
        map.put("DDAT", normalizeDateT24(safe(dealdate)));
        map.put("CCYB", safe(ccyBuy));
        map.put("BAMT", normalizeAmount(safe(buyAmt)));
        map.put("VBUY", normalizeDateT24(safe(vDateBuy)));
        map.put("CCYS", safe(ccySold));
        map.put("SAMT", normalizeAmount(safe(sellAmt)));
        map.put("VSEL", normalizeDateT24(safe(vDateSell)));
        map.put("SPRT", safe(sptRate));
        map.put("SPDT", normalizeDateT24(safe(sptDate)));
        map.put("BCCY", safe(baseCcy));
        map.put("BRKR", safe(dBroker));
        map.put("NOTS", safe(dlrNotes));
        map.put("OACP", safe(ourAcPay));
        map.put("OACR", safe(ourAcRec));
        map.put("CCNO", safe(cCorrNum));
        map.put("CCAD", safe(cCorrAdd));
        map.put("CBNK", safe(cpBnkAcc));
        map.put("BKBK", safe(bkbkInfo));
        map.put("INTB", safe(iRateBuy));
        map.put("INTS", safe(iRateSel));

        // Map deal-specific fields based on DEAL_TYPE
        if (dealtype != null && !dealtype.trim().isEmpty()) {
            String normalizedDealType = dealtype.trim().toUpperCase();

            switch (normalizedDealType) {
            case "FW": // Forward deal
                String fwdRate = asText(p_fm, "FORWARD_RATE");
                map.put("FWRT", safe(fwdRate));
                LOGGER.log(Level.FINE, "[CsdBloombergFxMappers] FW: mapped FORWARD_RATE={0}", fwdRate);
                break;

            case "SW": // Swap deal
                String swBsCcy = asText(p_fm, "SWAP_BASE_CCY");
                String leg1Rate = asText(p_fm, "LEG1_FWD_RATE");
                String fwdRateSw = asText(p_fm, "FORWARD_RATE");
                String fwdVDtBuy = asText(p_fm, "FORWARD_VALUE_DATE_BUY");
                String fwdVDtSell = asText(p_fm, "FORWARD_VALUE_DATE_SELL");
                String fwdFwdSwap = asText(p_fm, "FWD_FWD_SWAP");
                String unevenSwap = asText(p_fm, "UNEVEN_SWAP");

                map.put("SCCY", safe(swBsCcy));
                map.put("LG1R", safe(leg1Rate));
                map.put("FWRT", safe(fwdRateSw));
                map.put("FVDB", normalizeDateT24(safe(fwdVDtBuy)));
                map.put("FVDS", normalizeDateT24(safe(fwdVDtSell)));
                map.put("FFWS", safe(fwdFwdSwap));
                map.put("UEVS", safe(unevenSwap));

                // Handle SWAP_REF_NO array
                if (p_fm.has("SWAP_REF_NO") && p_fm.get("SWAP_REF_NO").isArray()) {
                    JsonNode swapRefArray = p_fm.get("SWAP_REF_NO");
                    if (swapRefArray.size() > 0) {
                        map.put("SRF1", safe(swapRefArray.get(0).asText()));
                    }
                    if (swapRefArray.size() > 1) {
                        map.put("SRF2", safe(swapRefArray.get(1).asText()));
                    }
                }

                LOGGER.log(Level.FINE, "[CsdBloombergFxMappers] SW: mapped swap-specific fields");
                break;

            case "SP": // Spot deal - no additional fields
                LOGGER.log(Level.FINE, "[CsdBloombergFxMappers] SP: no deal-specific fields");
                break;

            default:
                LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] Unknown DEAL_TYPE: {0}", dealtype);
            }
        }

        // Map OFS_VSN based on DEAL_TYPE
        if (dealtype != null && !dealtype.trim().isEmpty()) {
            try {
                String ofsVsn = mapDealTypeToOfsVsn(dealtype);
                map.put("DVSN", ofsVsn);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "[CsdBloombergFxMappers] Invalid DEAL_TYPE: {0}", dealtype);
                map.put("DVSN", "");
            }
        } else {
            map.put("DVSN", "DVSN");
        }

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

    /**
     * Maps DEAL_TYPE to OFS_VSN flag value.
     *
     * @param dealType the deal type code (SW, SP, FW)
     * @return the corresponding OFS_VSN value
     * @throws IllegalArgumentException if dealType is null or unknown
     */
    public static String mapDealTypeToOfsVsn(String dealType) {
        if (dealType == null) {
            throw new IllegalArgumentException("DEAL_TYPE cannot be null");
        }
        String normalized = dealType.trim().toUpperCase();

        switch (normalized) {
        case "SW":
            return "FOREX,FX.SWAP";
        case "SP":
            return "FOREX,SPOTDEAL";
        case "FW":
            return "FOREX,FORWARDDEAL";
        default:
            throw new IllegalArgumentException("Unknown DEAL_TYPE: " + dealType);
        }
    }

    // ====
    // ==== NORMALIZER ====
    // ====

    /**
     * Normalize top-level payloads so parsing is uniform: - [ { … } ] -> {
     * "FOREX_TRANSACTION": [ … ] } - { … } -> { "FOREX_TRANSACTION": [ { … } ] } -
     * { "FOREX_TRANSACTION": { … } } -> { "FOREX_TRANSACTION": [ { … } ] } - {
     * "FOREX_TRANSACTION": [ … ] } -> unchanged
     */
    private static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull())
            return om.createObjectNode();

        // Already wrapped
        if (root.isObject() && root.has("FOREX_TRANSACTION")) {
            JsonNode fm = root.get("FOREX_TRANSACTION");
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set("FOREX_TRANSACTION", arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set("FOREX_TRANSACTION", root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set("FOREX_TRANSACTION", arr);
            return w;
        }

        return om.createObjectNode();
    }

    // ====
    // ==== MQ CONFIG ====
    // ====

    private static Properties loadMqProperties() {
        Properties props = new Properties();
        // Defaults. Replace with external config in production.
        props.setProperty("mq.host", "172.105.249.157");
        props.setProperty("mq.port", "1414");
        props.setProperty("mq.channel", "DEV.APP.SVRCONN");
        props.setProperty("mq.qmgr", "QM_BLOOMBERG");
        props.setProperty("mq.queue", "TEST.QUEUE");
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
        factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
        return factory;
    }

    // ====
    // ==== QUIET CLOSERS ====
    // ====

    private static void closeQuietly(MessageConsumer c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFxMappers] close consumer ignored", e);
            }
    }

    private static void closeQuietly(Session s) {
        if (s != null)
            try {
                s.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFxMappers] close session ignored", e);
            }
    }

    private static void closeQuietly(Connection c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFxMappers] close connection ignored", e);
            }
    }
}