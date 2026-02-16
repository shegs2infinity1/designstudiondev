package com.cbn.bloomberg.pr;

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
 * Transformer utilities for Bloomberg RO flow. Provides JSON normalization,
 * field mapping, and business logic transformations.
 */
public final class CbnPrMapper {

    private static final Logger LOGGER = Logger.getLogger(CbnPrMapper.class.getName());
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CbnPrMapper() {
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
     * Checks if the root JSON contains REPO data.
     */
    public static boolean hasRepository(JsonNode pRoot) {
        if (pRoot == null)
            return false;
        JsonNode fm = pRoot.get("REPO");
        if (fm == null || fm.isNull())
            return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
     * Retrieves a specific REPO item by index.
     */
    public static JsonNode getRepositoryAt(JsonNode pRoot, int pIndex) {
        JsonNode fm = pRoot.get("REPO");
        if (fm == null)
            return null;
        if (fm.isArray())
            return fm.get(pIndex);
        return fm.isObject() ? fm : null;
    }

    // ====================================================================
    // JSON NORMALIZATION
    // ====================================================================

    /**
     * Normalize top-level payloads so parsing is uniform: - [ { … } ] -> {
     * "REPO": [ … ] } - { … } -> { "REPO": [ { … } ] } - {
     * "REPO": { … } } -> { "REPO": [ { … } ] } - {
     * "REPO": [ … ] } -> unchanged
     */
    public static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull())
            return om.createObjectNode();

        // Already wrapped
        if (root.isObject() && root.has("REPO")) {
            JsonNode fm = root.get("REPO");
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set("REPO", arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set("REPO", root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set("REPO", arr);
            return w;
        }

        return om.createObjectNode();
    }

    // ====================================================================
    // FIELD MAPPING
    // ====================================================================

    /**
     * Map a single REPO node to RO field map. Only basic fields are
     * mapped here; extend as needed.
     */
    public static Map<String, String> mapRepositoryToRo(JsonNode pFm) {
        LOGGER.log(Level.INFO, "[CsdBloombergRoMapper] Starting mapping of JSON message");

        // Mapping incoming bloomberg json feed to Funds Transfer Module
        Map<String, String> map = new HashMap<>();

        String cPty = asText(pFm, "COUNTERPARTY");
        String dCcy = asText(pFm, "CURRENCY");
        String tDte = asText(pFm, "TRADE_DATE");
        String vDte = asText(pFm, "VALUE_DATE");
        String mDte = asText(pFm, "MATURITY_DATE");
        String pAm1 = asText(pFm, "PRINCIPAL_AMOUNT.1");
        String pAm2 = asText(pFm, "PRINCIPAL_AMOUNT.2");
        String rRte = asText(pFm, "REPO_RATE");
        String rPrd = asText(pFm, "PRODUCT");
        String dAcc = asText(pFm, "DRAWDOWN_ACCOUNT");
        String iAcc = asText(pFm, "DRAWIN_ACCOUNT");
        String tInt = asText(pFm, "TOTAL_INTEREST_AMT");
        String bloombergId = asText(pFm, "BLOOMBERG_ID");

        map.put("CPTY", safe(cPty));
        map.put("DCCY", safe(dCcy));
        map.put("TDTE", normalizeDateT24(safe(tDte)));
        map.put("VDTE", normalizeDateT24(safe(vDte)));
        map.put("MDTE", normalizeDateT24(safe(mDte)));
        map.put("AMT1", normalizeAmount(safe(pAm1)));
        map.put("AMT2", normalizeAmount(safe(pAm2)));
        map.put("RRTE", safe(rRte));
        map.put("RPRD", safe(rPrd));
        map.put("DDAC", safe(dAcc));
        map.put("DIAC", safe(iAcc));
        map.put("TINT", safe(tInt));
        map.put("BLOOMBERG_ID", safe(bloombergId));
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









