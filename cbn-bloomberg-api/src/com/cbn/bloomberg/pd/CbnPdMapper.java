package com.cbn.bloomberg.pd;

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
 * Transformer utilities for Bloomberg PD flow. Provides JSON normalization, field mapping, and
 * business logic transformations.
 */
public final class CbnPdMapper {

    private static final Logger LOGGER = Logger.getLogger(CbnPdMapper.class.getName());
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CbnPdMapper() {
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
    * Checks if the root JSON contains PLACEMENTS data.
    */
    public static boolean hasPlacements(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode fm = pRoot.get("PLACEMENTS");
        if (fm == null || fm.isNull()) return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
    * Retrieves a specific PLACEMENTS item by index.
    */
    public static JsonNode getPlacementsAt(JsonNode pRoot, int pIndex) {
        JsonNode fm = pRoot.get("PLACEMENTS");
        if (fm == null) return null;
        if (fm.isArray()) return fm.get(pIndex);
        return fm.isObject() ? fm : null;
    }

    /**
    * Normalize top-level payloads so parsing is uniform: - [ { … } ] -> { "PLACEMENTS": [ … ] } - {
    * … } -> { "PLACEMENTS": [ { … } ] } - { "PLACEMENTS": { … } } -> { "PLACEMENTS": [ { … } ] } -
    * { "PLACEMENTS": [ … ] } -> unchanged
    */
    public static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull()) return om.createObjectNode();

        // Already wrapped
        if (root.isObject() && root.has("PLACEMENTS")) {
            JsonNode fm = root.get("PLACEMENTS");
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set("PLACEMENTS", arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set("PLACEMENTS", root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set("PLACEMENTS", arr);
            return w;
        }

        return om.createObjectNode();
    }

    /**
    * Map a single PLACEMENTS node to PD field map. Only basic fields are mapped here; extend as
    * needed.
    */
    public static Map<String, String> mapPlacementsToPd(JsonNode pFm) {
        LOGGER.log(Level.INFO, "[CbnPdMapper] Starting mapping of JSON message");

        // Mapping incoming bloomberg json feed to Depo Placement Module
        Map<String, String> map = new HashMap<>();
        String cNum = asText(pFm, "CUSTOMER_NO");
        String tCcy = asText(pFm, "CURRENCY");
        String pPpl = asText(pFm, "PRINCIPAL");
        String eRte = asText(pFm, "EXCH_RATE");
        String aLcy = asText(pFm, "LCY_AMOUNT");
        String dDte = asText(pFm, "DEAL_DATE");
        String vDte = asText(pFm, "VALUE_DATE");
        String mDte = asText(pFm, "MATURITY_DATE");
        String tCat = asText(pFm, "CATEGORY");
        String iRte = asText(pFm, "INT_RATE");
        String iBss = asText(pFm, "INTEREST_BASIS");
        String tInt = asText(pFm, "TOT_INTEREST_AMT");
        String iDte = asText(pFm, "INT_DUE_DATE");
        String dInt = asText(pFm, "LIQ_DEFER_INTEREST");
        String tRmk = asText(pFm, "REMARKS");
        String dAcc = asText(pFm, "DRAWDOWN_ACCOUNT");
        String pAcc = asText(pFm, "PR_LIQUID_ACCT");
        String iAcc = asText(pFm, "INT_LIQUID_ACCT");
        String cAmt = asText(pFm, "CHARGE_ACCOUNT");
        String cCde = asText(pFm, "CHARGE_CODE");
        String fAcc = asText(pFm, "FGN_FED_ACCT");
        String lAcc = asText(pFm, "LIAB_ACCOUNT");
        String mSod = asText(pFm, "MATURE_AT_SOD");
        String pIcr = asText(pFm, "PRIN_INCR_DECR");
        String pDte = asText(pFm, "INCR_DECR_EFF_DATE");
        String rInd = asText(pFm, "ROLLOVER_IND");
        String rIns = asText(pFm, "ROLLOVER_INSTR");
        String nInt = asText(pFm, "NEW_INT_RATE");
        String tCap = asText(pFm, "CAPITALISATION");
        String pAmt = asText(pFm, "PREV_PRIN_AMOUNT");
        String bloombergId = asText(pFm, "BLOOMBERG_ID");

        map.put("CNUM", safe(cNum));
        map.put("TCCY", safe(tCcy));
        map.put("PPPL", safe(pPpl));
        map.put("ERTE", safe(eRte));
        map.put("ALCY", normalizeAmount(safe(aLcy)));
        map.put("DDTE", normalizeAmount(safe(dDte)));
        map.put("VDTE", normalizeAmount(safe(vDte)));
        map.put("MDTE", normalizeAmount(safe(mDte)));
        map.put("TCAT", safe(tCat));
        map.put("IRTE", safe(iRte));
        map.put("IBSS", safe(iBss));
        map.put("TINT", safe(tInt));
        map.put("IDTE", normalizeAmount(safe(iDte)));
        map.put("DINT", safe(dInt));
        map.put("TRMK", safe(tRmk));
        map.put("DACC", safe(dAcc));
        map.put("PACC", safe(pAcc));
        map.put("IACC", safe(iAcc));
        map.put("CAMT", safe(cAmt));
        map.put("CCDE", safe(cCde));
        map.put("FACC", safe(fAcc));
        map.put("LACC", safe(lAcc));
        map.put("MSOD", safe(mSod));
        map.put("PICR", safe(pIcr));
        map.put("PDTE", normalizeAmount(safe(pDte)));
        map.put("RIND", safe(rInd));
        map.put("RINS", safe(rIns));
        map.put("NINT", safe(nInt));
        map.put("TCAP", safe(tCap));
        map.put("PAMT", normalizeAmount(safe(pAmt)));
        map.put("BLOOMBERG_ID", safe(bloombergId));
        return map;
    }

    private static String asText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
