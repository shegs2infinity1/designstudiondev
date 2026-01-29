package com.cbn.bloomberg.sc;

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
 * Transformer utilities for Bloomberg SC flow. Provides JSON normalization, field mapping, and
 * business logic transformations.
 */
public final class CbnScMapper {

    private static final Logger LOGGER = Logger.getLogger(CbnScMapper.class.getName());
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CbnScMapper() {
    }

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
            return dateStr;
        }
    }

    /**
     * Normalizes amount by removing commas.
     */
    private static String normalizeAmount(String amt) {
        if (amt == null || amt.trim().isEmpty()) return "";
        return amt.replace(",", "");
    }

    /**
     * Checks if the root JSON contains SECURITY_MASTER data.
     */
    public static boolean hasSecurityMaster(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode fm = pRoot.get("SECURITY_MASTER");
        if (fm == null || fm.isNull()) return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
     * Retrieves a specific SECURITY_MASTER item by index.
     */
    public static JsonNode getSecurityMasterAt(JsonNode pRoot, int pIndex) {
        JsonNode fm = pRoot.get("SECURITY_MASTER");
        if (fm == null) return null;
        if (fm.isArray()) return fm.get(pIndex);
        return fm.isObject() ? fm : null;
    }

    /**
     * Normalize top-level payloads so parsing is uniform: - [ { … } ] -> { "SECURITY_MASTER": [ … ]
     * } - { … } -> { "SECURITY_MASTER": [ { … } ] } - { "SECURITY_MASTER": { … } } -> {
     * "SECURITY_MASTER": [ { … } ] } - { "SECURITY_MASTER": [ … ] } -> unchanged
     */
    public static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull()) return om.createObjectNode();

        // Already wrapped
        if (root.isObject() && root.has("SECURITY_MASTER")) {
            JsonNode fm = root.get("SECURITY_MASTER");
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set("SECURITY_MASTER", arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set("SECURITY_MASTER", root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set("SECURITY_MASTER", arr);
            return w;
        }

        return om.createObjectNode();
    }

    /**
     * Map a single SECURITY_MASTER node to SC field map. Only basic fields are mapped here; extend
     * as needed.
     */
    public static Map<String, String> mapSecurityMasterToSc(JsonNode pFm) {
        LOGGER.log(Level.INFO, "[CbnScMapper] Starting mapping of JSON message");

        // Mapping incoming bloomberg json feed to SecurityMaster Module
        Map<String, String> map = new HashMap<>();
        String cNme = asText(pFm, "COMPANY_NAME");
        String sDes = asText(pFm, "DESCRIPTION");
        String sNme = asText(pFm, "SHORT_NAME");
        String mMne = asText(pFm, "MNEMONIC");
        String cDom = asText(pFm, "COMPANY_DOMICILE");
        String sDom = asText(pFm, "SECURITY_DOMICILE");
        String sCcy = asText(pFm, "SECURITY_CURRENCY");
        String bOsh = asText(pFm, "BOND_OR_SHARE");
        String sAst = asText(pFm, "SUB_ASSET_TYPE");
        String pCcy = asText(pFm, "PRICE_CURRENCY");
        String pTyp = asText(pFm, "PRICE_TYPE");
        String lPrc = asText(pFm, "LAST_PRICE");
        String pCde = asText(pFm, "PRICE_UPDATE_CODE");
        String iCde = asText(pFm, "INDUSTRY_CODE");
        String sExc = asText(pFm, "STOCK_EXCHANGE");
        String cTax = asText(pFm, "COUPON_TAX_CODE");
        String bInt = asText(pFm, "INTEREST_DAY_BASIS");
        String iRte = asText(pFm, "INTEREST_RATE");
        String iDte = asText(pFm, "ISSUE_DATE");
        String mDte = asText(pFm, "MATURITY_DATE");
        String nPay = asText(pFm, "NO_OF_PAYMENT");
        String aDte = asText(pFm, "ACCRUAL_START_DATE");
        String pDte = asText(pFm, "INT_PAYMENT_DATE");
        String cDte = asText(pFm, "FIRST_CPN_DATE");
        String iSin = asText(pFm, "ISIN");
        String sDte = asText(pFm, "SETUP_DATE");

        map.put("CNME", safe(cNme));
        map.put("SDES", safe(sDes));
        map.put("SNME", safe(sNme));
        map.put("MMNE", safe(mMne));
        map.put("CDOM", safe(cDom));
        map.put("SDOM", safe(sDom));
        map.put("SCCY", safe(sCcy));
        map.put("BOSH", safe(bOsh));
        map.put("SAST", safe(sAst));
        map.put("PCCY", safe(pCcy));
        map.put("PTYP", safe(pTyp));
        map.put("LPRC", normalizeAmount(safe(lPrc)));
        map.put("PCDE", safe(pCde));
        map.put("ICDE", safe(iCde));
        map.put("SEXC", safe(sExc));
        map.put("CTAX", safe(cTax));
        map.put("BINT", safe(bInt));
        map.put("IRTE", normalizeAmount(safe(iRte)));
        map.put("IDTE", normalizeDateT24(safe(iDte)));
        map.put("MDTE", normalizeDateT24(safe(mDte)));
        map.put("NPAY", safe(nPay));
        map.put("ADTE", normalizeDateT24(safe(aDte)));
        map.put("PDTE", normalizeDateT24(safe(pDte)));
        map.put("CDTE", normalizeDateT24(safe(cDte)));
        map.put("ISIN", safe(iSin));
        map.put("SDTE", normalizeDateT24(safe(sDte)));
        return map;
    }

    private static String asText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
