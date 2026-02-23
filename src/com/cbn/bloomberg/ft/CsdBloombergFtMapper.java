package com.cbn.bloomberg.ft;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Transformer utilities for Bloomberg FT flow. Provides JSON normalization,
 * field mapping, and business logic transformations.
 */
public final class CsdBloombergFtMapper {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtMapper.class.getName());
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CsdBloombergFtMapper() {
    }

    // ====================================================================
    // DATE AND AMOUNT FORMATTING
    // ====================================================================

    /**
     * Normalizes date from yyyy-MM-dd to yyyyMMdd format for T24.
     */
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

    /**
     * Normalizes amount by removing commas.
     */
    private static String normalizeAmount(String amt) {
        if (amt == null || amt.trim().isEmpty())
            return "";
        return amt.replace(",", "");
    }

    // ====================================================================
    // JSON NAVIGATION HELPERS
    // ====================================================================

    /**
     * Checks if the root JSON contains FUNDS_MOVEMENT data.
     */
    public static boolean hasFundsMovement(JsonNode p_root) {
        if (p_root == null)
            return false;
        JsonNode fm = p_root.get("FUNDS_MOVEMENT");
        if (fm == null || fm.isNull())
            return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
     * Retrieves a specific FUNDS_MOVEMENT item by index.
     */
    public static JsonNode getFundsMovementAt(JsonNode p_root, int p_index) {
        JsonNode fm = p_root.get("FUNDS_MOVEMENT");
        if (fm == null)
            return null;
        if (fm.isArray())
            return fm.get(p_index);
        return fm.isObject() ? fm : null;
    }

    // ====================================================================
    // JSON NORMALIZATION
    // ====================================================================

    /**
     * Normalize top-level payloads so parsing is uniform: - [ { … } ] -> {
     * "FUNDS_MOVEMENT": [ … ] } - { … } -> { "FUNDS_MOVEMENT": [ { … } ] } - {
     * "FUNDS_MOVEMENT": { … } } -> { "FUNDS_MOVEMENT": [ { … } ] } - {
     * "FUNDS_MOVEMENT": [ … ] } -> unchanged
     */
    public static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull())
            return om.createObjectNode();

        // Already wrapped
        if (root.isObject() && root.has("FUNDS_MOVEMENT")) {
            JsonNode fm = root.get("FUNDS_MOVEMENT");
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set("FUNDS_MOVEMENT", arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set("FUNDS_MOVEMENT", root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set("FUNDS_MOVEMENT", arr);
            return w;
        }

        return om.createObjectNode();
    }

    // ====================================================================
    // FIELD MAPPING
    // ====================================================================

    /**
     * Map a single FUNDS_MOVEMENT node to FT field map. Only basic fields are
     * mapped here; extend as needed.
     */
    public static Map<String, String> mapFundsMovementToFt(JsonNode p_fm) {
        LOGGER.log(Level.INFO, "[CsdBloombergFtMapper] Starting mapping of JSON message");

        // Mapping incoming bloomberg json feed to Funds Transfer Module
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

    // ====================================================================
    // PRIVATE HELPERS
    // ====================================================================

    private static String asText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}