package com.cbn.bloomberg.fx;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cbn.bloomberg.hp.CsdBloombergProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Transformer utilities for Bloomberg FX flow. Provides JSON normalization,
 * field mapping, and business logic transformations.
 */
public final class CsdBloombergFxMapper {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFxMapper.class.getName());

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CsdBloombergFxMapper() {
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
     * Checks if the root JSON contains FOREX_TRANSACTION data.
     */
    public static boolean hasForexTransacts(JsonNode p_root) {
        if (p_root == null)
            return false;
        JsonNode fm = p_root.get("FOREX_TRANSACTION");
        if (fm == null || fm.isNull())
            return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
     * Retrieves a specific FOREX_TRANSACTION item by index.
     */
    public static JsonNode getForexTransactAt(JsonNode p_root, int p_index) {
        JsonNode fm = p_root.get("FOREX_TRANSACTION");
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
     * Normalize top-level payloads so parsing is uniform:
     * - [ { … } ] -> { "FOREX_TRANSACTION": [ … ] }
     * - { … } -> { "FOREX_TRANSACTION": [ { … } ] }
     * - { "FOREX_TRANSACTION": { … } } -> { "FOREX_TRANSACTION": [ { … } ] }
     * - { "FOREX_TRANSACTION": [ … ] } -> unchanged
     */
    public static JsonNode normalizeRoot(JsonNode root) {
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

    // ====================================================================
    // FIELD MAPPING
    // ====================================================================

    /**
     * Map a single FOREX_TRANSACTION node to FX field map. Only basic fields are
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
        String cPtyCrNo = asText(p_fm, "CPARTY_CORR_NO");
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
        map.put("CCNO", safe(cPtyCrNo));
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
                LOGGER.log(Level.FINE, "[CsdBloombergFxMapper] FW: mapped FORWARD_RATE={0}", fwdRate);
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
                map.put("VBUY", safe(vDateBuy));

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

                LOGGER.log(Level.FINE, "[CsdBloombergFxMapper] SW: mapped swap-specific fields");
                break;

            case "SP": // Spot deal - no additional fields
                LOGGER.log(Level.FINE, "[CsdBloombergFxMapper] SP: no deal-specific fields");
                break;

            default:
                LOGGER.log(Level.WARNING, "[CsdBloombergFxMapper] Unknown DEAL_TYPE: {0}", dealtype);
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

        CsdBloombergProperties config = CsdBloombergProperties.getInstance();
        String ofsVersion = config.getFxOfsVersionForDealType(dealType);

        if (ofsVersion == null || ofsVersion.isEmpty()) {
            throw new IllegalArgumentException("Unknown DEAL_TYPE: " + dealType);
        }

        return ofsVersion;
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