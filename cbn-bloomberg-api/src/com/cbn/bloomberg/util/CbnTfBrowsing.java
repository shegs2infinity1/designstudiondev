package com.cbn.bloomberg.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * =============================================================================
 * CSD API Title: CbnTfBrowsing.java
 * Author: CSD Development Team
 * Created: 2025-10-11
 * Last Modified: 2026-02-03
 * =============================================================================
 *
 * PURPOSE: Shared utility for Bloomberg message queue processing across all T24 modules.
 * Supports selective message consumption pattern where each module's batch job
 * only consumes messages belonging to that module, leaving others on the queue.
 *
 * Module execution order (managed by Temenos Service Manager):
 * 1. FX (FOREX_TRANSACTION)
 * 2. FT (FUNDS_MOVEMENT)
 * 3. PD (PLACEMENT_DEPOSIT)
 * 4. PR (REPO_TRANSACTION)
 * 5. SC (SECURITY_MASTER)
 * 6. ST (SEC_TRADE)
 *
 * MODIFICATION HISTORY:
 * - 2025-10-11 | Initial creation
 * - 2026-02-03 | Added ST ModuleType for SEC_TRADE support
 * =============================================================================
 */
public final class CbnTfBrowsing {

    private static final Logger yLogger = Logger.getLogger(CbnTfBrowsing.class.getName());

    /**
     * Enumeration of Bloomberg message types and their corresponding JSON root nodes.
     * Each module adapter should use this to identify its target messages.
     */
    public enum ModuleType {

        /** 1. Funds Transfer - T24 FUNDS.TRANSFER application */
        FT("FUNDS_MOVEMENT", "FT"),
        
        /** 2. Foreign Exchange - T24 FOREX application */
        FX("FOREX_TRANSACTION", "FX"),

        /** 3. Placement/Money Market - T24 Deposit Placement application */
        PD("PLACEMENTS", "PD"),

        /** 4. Repo Transactions - T24 Repository Placement application */
        PR("REPO", "PR"),

        /** 5. Security Master - T24 SECURITY.MASTER application (static reference data) */
        SC("SECURITY_MASTER", "SC"),

        /** 6. Security Trade - T24 SEC.TRADE application (trading transactions) */
        ST("SEC_TRADE", "ST");

        private final String jsonRootNode;
        private final String idPrefix;

        ModuleType(String jsonRootNode, String idPrefix) {
            this.jsonRootNode = jsonRootNode;
            this.idPrefix = idPrefix;
        }

        /** The JSON root node name to look for in Bloomberg messages */
        public String getJsonRootNode() {
            return jsonRootNode;
        }

        /** The prefix used in generated IDs (e.g., "SC" for Security Master, "ST" for Sec Trade) */
        public String getIdPrefix() {
            return idPrefix;
        }

        /**
         * Checks if the given JSON root contains this module's transaction type.
         */
        public boolean hasTransaction(JsonNode root) {
            return root != null && root.has(jsonRootNode);
        }

        /**
         * Gets the transaction node from the JSON root.
         */
        public JsonNode getTransaction(JsonNode root) {
            return root != null ? root.get(jsonRootNode) : null;
        }
    }

    /**
     * Result of browsing the queue for messages belonging to a specific module.
     */
    public static final class BrowseResult {
        private final List<String> matchingMessageIds;
        private final int totalBrowsed;
        private final int matchCount;
        private final int skippedCount;

        public BrowseResult(List<String> matchingMessageIds, int totalBrowsed,
                           int matchCount, int skippedCount) {
            this.matchingMessageIds = matchingMessageIds;
            this.totalBrowsed = totalBrowsed;
            this.matchCount = matchCount;
            this.skippedCount = skippedCount;
        }

        public List<String> getMatchingMessageIds() {
            return matchingMessageIds;
        }

        public int getTotalBrowsed() {
            return totalBrowsed;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public boolean hasMatches() {
            return !matchingMessageIds.isEmpty();
        }
    }

    private CbnTfBrowsing() {
    }

    /**
     * Browses the queue and identifies messages belonging to the specified module.
     * This is a non-destructive read - messages remain on the queue.
     *
     * @param pConnection Active JMS connection
     * @param pQueueName  Name of the queue to browse
     * @param pModule     The module type to filter for
     * @param pObjMapper  Jackson ObjectMapper for JSON parsing
     * @return BrowseResult containing matching message IDs and statistics
     */
    public static BrowseResult browseForModule(Connection pConnection, String pQueueName,
            ModuleType pModule, ObjectMapper pObjMapper) throws JMSException {

        List<String> matchingIds = new ArrayList<>();
        int totalBrowsed = 0;
        int matchCount = 0;
        int skippedCount = 0;

        Session browseSession = null;
        QueueBrowser browser = null;

        try {
            browseSession = pConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            javax.jms.Queue queue = browseSession.createQueue("queue:///" + pQueueName);
            browser = browseSession.createBrowser(queue);

            yLogger.log(Level.INFO, "[CbnTfBrowsing] Browsing queue {0} for {1} messages",
                    new Object[] { pQueueName, pModule.name() });

            @SuppressWarnings("unchecked")
            Enumeration<Message> messages = browser.getEnumeration();

            while (messages.hasMoreElements()) {
                Message m = messages.nextElement();
                totalBrowsed++;

                if (!(m instanceof TextMessage)) {
                    skippedCount++;
                    continue;
                }

                String body = ((TextMessage) m).getText();
                if (body == null || body.trim().isEmpty()) {
                    skippedCount++;
                    continue;
                }

                String msgId = m.getJMSMessageID();

                try {
                    JsonNode root = pObjMapper.readTree(body);
                    // Handle both wrapped and unwrapped formats
                    root = normalizeRoot(root);

                    if (pModule.hasTransaction(root)) {
                        matchingIds.add(msgId);
                        matchCount++;
                        yLogger.log(Level.FINE,
                                "[CbnTfBrowsing] Identified {0} message: {1}",
                                new Object[] { pModule.name(), msgId });
                    } else {
                        ModuleType detected = detectModuleType(root);
                        yLogger.log(Level.FINE,
                                "[CbnTfBrowsing] Skipping {0} message {1} (target: {2})",
                                new Object[] { detected != null ? detected.name() : "UNKNOWN",
                                              msgId, pModule.name() });
                        skippedCount++;
                    }
                } catch (IOException ioe) {
                    yLogger.log(Level.WARNING,
                            "[CbnTfBrowsing] JSON parse error for message {0}", msgId);
                    skippedCount++;
                }
            }

            yLogger.log(Level.INFO,
                    "[CbnTfBrowsing] Browse complete for {0}: total={1}, matches={2}, skipped={3}",
                    new Object[] { pModule.name(), totalBrowsed, matchCount, skippedCount });

        } finally {
            closeQuietly(browser);
            closeQuietly(browseSession);
        }

        return new BrowseResult(matchingIds, totalBrowsed, matchCount, skippedCount);
    }

    /**
     * Detects which module type a message belongs to based on its JSON content.
     *
     * @param root Normalized JSON root node
     * @return The detected ModuleType, or null if unknown
     */
    public static ModuleType detectModuleType(JsonNode root) {
        if (root == null) {
            return null;
        }
        for (ModuleType module : ModuleType.values()) {
            if (module.hasTransaction(root)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Normalizes JSON root to handle both wrapped and unwrapped Bloomberg message formats.
     *
     * Bloomberg may send:
     * - Unwrapped: { "FOREX_TRANSACTION": {...} }
     * - Wrapped:   { "data": { "FOREX_TRANSACTION": {...} } }
     */
    public static JsonNode normalizeRoot(JsonNode root) {
        if (root == null) {
            return null;
        }
        // Check for common wrapper nodes
        if (root.has("data") && root.get("data").isObject()) {
            return root.get("data");
        }
        if (root.has("payload") && root.get("payload").isObject()) {
            return root.get("payload");
        }
        if (root.has("message") && root.get("message").isObject()) {
            return root.get("message");
        }
        return root;
    }

    /**
     * Normalizes a JMS message ID for use as a cache key.
     * Strips the "ID:" prefix that IBM MQ adds.
     *
     * @param msgId The JMS message ID
     * @return Normalized cache key
     */
    public static String normalizeCacheKey(String msgId) {
        if (msgId != null && msgId.startsWith("ID:")) {
            return msgId.substring(3);
        }
        return msgId;
    }

    /**
     * Builds a JMS selector to retrieve a specific message by ID.
     * Handles SQL escaping for the message ID.
     */
    public static String buildMessageSelector(String msgId) {
        return "JMSMessageID = '" + msgId.replace("'", "''") + "'";
    }

    /**
     * Calculates the number of transaction items in a module's JSON node.
     * Handles both array and single-object formats.
     */
    public static int countTransactionItems(JsonNode transactionNode) {
        if (transactionNode == null) {
            return 0;
        }
        if (transactionNode.isArray()) {
            return transactionNode.size();
        }
        if (transactionNode.isObject()) {
            return 1;
        }
        return 0;
    }

    // ====================================================================
    // RESOURCE CLEANUP HELPERS
    // ====================================================================

    public static void closeQuietly(QueueBrowser b) {
        if (b != null) {
            try {
                b.close();
            } catch (Exception e) {
                yLogger.log(Level.FINE, "[CbnTfBrowsing] close browser ignored", e);
            }
        }
    }

    public static void closeQuietly(Session s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception e) {
                yLogger.log(Level.FINE, "[CbnTfBrowsing] close session ignored", e);
            }
        }
    }

    public static void closeQuietly(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                yLogger.log(Level.FINE, "[CbnTfBrowsing] close connection ignored", e);
            }
        }
    }

    public static void closeQuietly(javax.jms.MessageConsumer c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                yLogger.log(Level.FINE, "[CbnTfBrowsing] close consumer ignored", e);
            }
        }
    }
}
