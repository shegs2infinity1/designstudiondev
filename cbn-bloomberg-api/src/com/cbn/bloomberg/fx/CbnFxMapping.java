package com.cbn.bloomberg.fx;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cbn.bloomberg.util.CbnTfProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Transformer utilities for Bloomberg FX flow. Provides JSON normalization, field mapping, and
 * business logic transformations.
 */
public final class CbnFxMapping {

    private static final Logger yLogger = Logger.getLogger(CbnFxMapping.class.getName());
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CbnFxMapping() {
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
        if (amt == null || amt.trim().isEmpty()) return "";
        return amt.replace(",", "");
    }

    // ====================================================================
    // JSON NAVIGATION HELPERS
    // ====================================================================

    /**
     * Checks if the root JSON contains FOREX_TRANSACTION data.
     */
    public static boolean hasForexTransacts(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode fm = pRoot.get("FOREX_TRANSACTION");
        if (fm == null || fm.isNull()) return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
     * Retrieves a specific FOREX_TRANSACTION item by index.
     */
    public static JsonNode getForexTransactAt(JsonNode pRoot, int p_index) {
        JsonNode fm = pRoot.get("FOREX_TRANSACTION");
        if (fm == null) return null;
        if (fm.isArray()) return fm.get(p_index);
        return fm.isObject() ? fm : null;
    }

    // ====================================================================
    // JSON NORMALIZATION
    // ====================================================================

    /**
     * Normalize top-level payloads so parsing is uniform: - [ { … } ] -> { "FOREX_TRANSACTION": [ …
     * ] } - { … } -> { "FOREX_TRANSACTION": [ { … } ] } - { "FOREX_TRANSACTION": { … } } -> {
     * "FOREX_TRANSACTION": [ { … } ] } - { "FOREX_TRANSACTION": [ … ] } -> unchanged
     */
    public static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull()) return om.createObjectNode();

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

    // ====================================================================
    // FIELD MAPPING
    // ====================================================================

    /**
     * Map a single FOREX_TRANSACTION node to FX field map. Only basic fields are mapped here;
     * extend as needed.
     */
    public static Map<String, String> mapForexTransactToFx(JsonNode pFm) {

        // Mapping incoming bloomberg json feed to Forex Trading Module
        Map<String, String> map = new HashMap<>();

        // Global fields
        String cpPty = asText(pFm, "COUNTERPARTY");
        String dlTpe = asText(pFm, "DEAL_TYPE");
        String dlDsk = asText(pFm, "DEALER_DESK");
        String dlDte = asText(pFm, "DEAL_DATE");
        String byCcy = asText(pFm, "CURRENCY_BOUGHT");
        String byAmt = asText(pFm, "BUY_AMOUNT");
        String vdBuy = asText(pFm, "VALUE_DATE_BUY");
        String slCcy = asText(pFm, "CURRENCY_SOLD");
        String slAmt = asText(pFm, "SELL_AMOUNT");
        String vdSel = asText(pFm, "VALUE_DATE_SELL");
        String spRte = asText(pFm, "SPOT_RATE");
        String spDte = asText(pFm, "SPOT_DATE");
        String beCcy = asText(pFm, "BASE_CCY");
        String dlBrk = asText(pFm, "BROKER");
        String dlNts = asText(pFm, "DEALER_NOTES");
        String oaPay = asText(pFm, "OUR_ACCOUNT_PAY");
        String oaRec = asText(pFm, "OUR_ACCOUNT_REC");
        String cpCon = asText(pFm, "CPARTY_CORR_NO");
        String cpCoa = asText(pFm, "CPY_CORR_ADD");
        String cpAcc = asText(pFm, "CPARTY_BANK_ACC");
        String bkInf = asText(pFm, "BK_TO_BK_INF");
        String irBuy = asText(pFm, "INT_RATE_BUY");
        String irSel = asText(pFm, "INT_RATE_SELL");

        map.put("CPTY", safe(cpPty));
        map.put("DTYP", safe(dlTpe));
        map.put("DESK", safe(dlDsk));
        map.put("DDAT", normalizeDateT24(safe(dlDte)));
        map.put("CCYB", safe(byCcy));
        map.put("BAMT", normalizeAmount(safe(byAmt)));
        map.put("VBUY", normalizeDateT24(safe(vdBuy)));
        map.put("CCYS", safe(slCcy));
        map.put("SAMT", normalizeAmount(safe(slAmt)));
        map.put("VSEL", normalizeDateT24(safe(vdSel)));
        map.put("SPRT", safe(spRte));
        map.put("SPDT", normalizeDateT24(safe(spDte)));
        map.put("BCCY", safe(beCcy));
        map.put("BRKR", safe(dlBrk));
        map.put("NOTS", safe(dlNts));
        map.put("OACP", safe(oaPay));
        map.put("OACR", safe(oaRec));
        map.put("CCNO", safe(cpCon));
        map.put("CCAD", safe(cpCoa));
        map.put("CBNK", safe(cpAcc));
        map.put("BKBK", safe(bkInf));
        map.put("INTB", safe(irBuy));
        map.put("INTS", safe(irSel));

        // Map deal-specific fields based on DEAL_TYPE
        if (dlTpe != null && !dlTpe.trim().isEmpty()) {
            String normalizedDealType = dlTpe.trim().toUpperCase();

            switch (normalizedDealType) {
                case "SD": // Bskd deal
                    String dRate = asText(pFm, "RATE");
                    String prAcc = asText(pFm, "PAY_REC_ACCOUNT");
                    String pStat = asText(pFm, "STATUS");
                    String iRtgs = asText(pFm, "RTGS_ID");
                    String idCom = asText(pFm, "COMPLETED_ID");
                    String bRate = asText(pFm, "BUY_RATE");
                    String sRate = asText(pFm, "SELL_RATE");
                    String mRate = asText(pFm, "MID_RATE");
                    String mErrr = asText(pFm, "MDC_ERROR");

                    map.put("DRTE", safe(dRate));
                    map.put("PACC", safe(prAcc));
                    map.put("STSS", safe(pStat));
                    map.put("RTGS", safe(iRtgs));
                    map.put("IDCM", safe(idCom));
                    map.put("BUYR", safe(bRate));
                    map.put("SELR", safe(sRate));
                    map.put("MRTE", safe(mRate));
                    map.put("MERR", safe(mErrr));
                    yLogger.log(Level.FINE,
                            "[CbnFxMapping] SW: mapped swap-specific fields");
                    break;

                case "FW": // Forward deal
                    String fwdRate = asText(pFm, "FORWARD_RATE");
                    map.put("FWRT", safe(fwdRate));

                    yLogger.log(Level.FINE, "[CbnFxMapping] FW: mapped FORWARD_RATE={0}",
                            fwdRate);
                    break;

                case "SW": // Swap deal
                    String swCcy = asText(pFm, "SWAP_BASE_CCY");
                    String leg1r = asText(pFm, "LEG1_FWD_RATE");
                    String fwRte = asText(pFm, "FORWARD_RATE");
                    String fdBuy = asText(pFm, "FORWARD_VALUE_DATE_BUY");
                    String fdSel = asText(pFm, "FORWARD_VALUE_DATE_SELL");
                    String fwFws = asText(pFm, "FWD_FWD_SWAP");
                    String swUvn = asText(pFm, "UNEVEN_SWAP");

                    map.put("SCCY", safe(swCcy));
                    map.put("LG1R", safe(leg1r));
                    map.put("FWRT", safe(fwRte));
                    map.put("FVDB", normalizeDateT24(safe(fdBuy)));
                    map.put("FVDS", normalizeDateT24(safe(fdSel)));
                    map.put("FFWS", safe(fwFws));
                    map.put("UEVS", safe(swUvn));
                    map.put("VBUY", safe(vdBuy));

                    // Handle SWAP_REF_NO array
                    if (pFm.has("SWAP_REF_NO") && pFm.get("SWAP_REF_NO").isArray()) {
                        JsonNode swapRefArray = pFm.get("SWAP_REF_NO");
                        if (swapRefArray.size() > 0) {
                            map.put("SRF1", safe(swapRefArray.get(0).asText()));
                        }
                        if (swapRefArray.size() > 1) {
                            map.put("SRF2", safe(swapRefArray.get(1).asText()));
                        }
                    }

                    yLogger.log(Level.FINE,
                            "[CbnFxMapping] SW: mapped swap-specific fields");
                    break;
                case "SP":
                    // Spot deal - no additional fields
                    yLogger.log(Level.FINE, "[CbnFxMapping] SP: no deal-specific fields");
                    break;

                default:
                    yLogger.log(Level.WARNING, "[CbnFxMapping] Unknown DEAL_TYPE: {0}",
                            dlTpe);
            }
        }

        return map;
    }

    /**
     * Maps DEAL_TYPE to OFS_VSN flag value using CsdBloombergProperties.
     *
     * @param dealType the deal type code (SW, SP, FW)
     * @return the corresponding OFS_VSN value
     * @throws IllegalArgumentException if dealType is null or unknown
     */
    public static String mapDealTypeToOfsVsn(String dealType) {
        if (dealType == null) {
            throw new IllegalArgumentException("DEAL_TYPE cannot be null");
        }

        CbnTfProperties config = CbnTfProperties.getInstance();
        String ofsVersion = config.getOfsVersionFx(dealType);

        if (ofsVersion == null || ofsVersion.isEmpty()) {
            throw new IllegalArgumentException("Unknown DEAL_TYPE: " + dealType);
        }

        return ofsVersion;
    }

    // ====================================================================
    // PRIVATE HELPERS
    // ====================================================================

    private static String asText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}