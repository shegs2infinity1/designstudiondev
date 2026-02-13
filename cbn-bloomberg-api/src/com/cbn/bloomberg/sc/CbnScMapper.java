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
 * =============================================================================
 * CSD API Title: CbnScMapper.java
 * Author: CSD Development Team
 * Created: 2026-01-07
 * Last Modified: 2026-02-03
 * =============================================================================
 *
 * PURPOSE: Transformer utilities for Bloomberg SC flow. Provides JSON normalization,
 * field mapping, and business logic transformations for both SECURITY_MASTER and SEC_TRADE.
 *
 * TARGETS:
 * - SECURITY_MASTER → T24 SECURITY.MASTER table (static reference data)
 * - SEC_TRADE → T24 SEC.TRADE table (trading transactions)
 *
 * MODIFICATION HISTORY:
 * - 2026-01-07 | Initial creation for SECURITY_MASTER
 * - 2026-02-03 | Added SEC_TRADE support (hasSecTrade, getSecTradeAt, mapSecTradeToSt)
 * =============================================================================
 */
public final class CbnScMapper {

    private static final Logger LOGGER = Logger.getLogger(CbnScMapper.class.getName());
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // JSON root node constants
    private static final String ROOT_SEC_TRADE = "SEC_TRADE";
    private static final String ROOT_SEC_MASTER = "SECURITY_MASTER";

    private CbnScMapper() {
    }

    // ========================================================================
    // MESSAGE TYPE DETECTION
    // ========================================================================

    /**
     * Enumeration for message type discrimination.
     */
    public enum ScMessageType {
        SECURITY_MASTER, SEC_TRADE, UNKNOWN
    }

    /**
     * Detects the message type from the JSON root node.
     * 
     * @param pRoot the JSON root node
     * @return the detected message type
     */
    public static ScMessageType getMessageType(JsonNode pRoot) {
        if (pRoot == null) {
            return ScMessageType.UNKNOWN;
        }
        if (pRoot.has(ROOT_SEC_MASTER) && !pRoot.get(ROOT_SEC_MASTER).isNull()) {
            return ScMessageType.SECURITY_MASTER;
        }
        if (pRoot.has(ROOT_SEC_TRADE) && !pRoot.get(ROOT_SEC_TRADE).isNull()) {
            return ScMessageType.SEC_TRADE;
        }
        return ScMessageType.UNKNOWN;
    }

    // ========================================================================
    // DATE AND AMOUNT UTILITIES
    // ========================================================================

    /**
     * Normalizes date from yyyy-MM-dd to yyyyMMdd format for T24.
     * If already in yyyyMMdd format, returns as-is.
     */
    public static String normalizeDateT24(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }
        // Skip if already in T24 format (8 digits, no dashes)
        if (dateStr.matches("\\d{8}")) {
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

    private static String asText(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ========================================================================
    // SECURITY_MASTER METHODS (Existing)
    // ========================================================================

    /**
     * Checks if the root JSON contains SECURITY_MASTER data.
     */
    public static boolean hasSecurityMaster(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode fm = pRoot.get(ROOT_SEC_MASTER);
        if (fm == null || fm.isNull()) return false;
        return (fm.isArray() && fm.size() > 0) || fm.isObject();
    }

    /**
     * Retrieves a specific SECURITY_MASTER item by index.
     */
    public static JsonNode getSecurityMasterAt(JsonNode pRoot, int pIndex) {
        JsonNode fm = pRoot.get(ROOT_SEC_MASTER);
        if (fm == null) return null;
        if (fm.isArray()) return fm.get(pIndex);
        return fm.isObject() ? fm : null;
    }

    /**
     * Normalize top-level payloads so parsing is uniform for SECURITY_MASTER.
     */
    public static JsonNode normalizeRoot(JsonNode root) {
        ObjectMapper om = new ObjectMapper();
        if (root == null || root.isNull()) return om.createObjectNode();

        // Already wrapped with SECURITY_MASTER
        if (root.isObject() && root.has(ROOT_SEC_MASTER)) {
            JsonNode fm = root.get(ROOT_SEC_MASTER);
            if (fm != null && fm.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(fm);
                w.set(ROOT_SEC_MASTER, arr);
                return w;
            }
            return root;
        }

        // Already wrapped with SEC_TRADE
        if (root.isObject() && root.has(ROOT_SEC_TRADE)) {
            JsonNode st = root.get(ROOT_SEC_TRADE);
            if (st != null && st.isObject()) {
                ObjectNode w = om.createObjectNode();
                ArrayNode arr = om.createArrayNode();
                arr.add(st);
                w.set(ROOT_SEC_TRADE, arr);
                return w;
            }
            return root;
        }

        if (root.isArray()) {
            ObjectNode w = om.createObjectNode();
            w.set(ROOT_SEC_MASTER, root);
            return w;
        }

        if (root.isObject()) {
            ObjectNode w = om.createObjectNode();
            ArrayNode arr = om.createArrayNode();
            arr.add(root);
            w.set(ROOT_SEC_MASTER, arr);
            return w;
        }

        return om.createObjectNode();
    }

    /**
     * Map a single SECURITY_MASTER node to SC field map.
     */
    public static Map<String, String> mapSecurityMasterToSc(JsonNode pFm) {
        LOGGER.log(Level.INFO, "[CbnScMapper] Starting mapping of SECURITY_MASTER message");

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

        LOGGER.log(Level.INFO, "[CbnScMapper] SECURITY_MASTER mapping complete: {0} fields",
                map.size());
        return map;
    }

    // ========================================================================
    // SEC_TRADE METHODS (New - Added 2026-02-03)
    // ========================================================================

    /**
     * Checks if the root JSON contains SEC_TRADE data.
     */
    public static boolean hasSecTrade(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode st = pRoot.get(ROOT_SEC_TRADE);
        if (st == null || st.isNull()) return false;
        return (st.isArray() && st.size() > 0) || st.isObject();
    }

    /**
     * Retrieves a specific SEC_TRADE item by index.
     */
    public static JsonNode getSecTradeAt(JsonNode pRoot, int pIndex) {
        JsonNode st = pRoot.get(ROOT_SEC_TRADE);
        if (st == null) return null;
        if (st.isArray()) return st.get(pIndex);
        return st.isObject() ? st : null;
    }

    /**
     * Maps a SEC_TRADE JSON node to T24 field map.
     * Note: SEC_TRADE dates from Bloomberg are already in yyyyMMdd format.
     *
     * @param pSt the SEC_TRADE JSON node
     * @return a map of T24 field abbreviations to values
     */
    public static Map<String, String> mapSecTradeToSt(JsonNode pSt) {
        LOGGER.log(Level.INFO, "[CbnScMapper] Starting mapping of SEC_TRADE message");

        Map<String, String> map = new HashMap<>();

        // Delegate to focused helper methods for SonarQube cognitive complexity
        mapStTradeIdentifiers(pSt, map);
        mapStTradeDates(pSt, map);
        mapStCurrencyAndRates(pSt, map);
        mapStCustomerInfo(pSt, map);
        mapStAmounts(pSt, map);
        mapStCharges(pSt, map);
        mapStAccounts(pSt, map);
        mapStBrokerInfo(pSt, map);
        mapStDescription(pSt, map);

        LOGGER.log(Level.INFO, "[CbnScMapper] SEC_TRADE mapping complete: {0} fields", map.size());
        return map;
    }

    /**
     * Maps transaction identifier fields from SEC_TRADE.
     */
    private static void mapStTradeIdentifiers(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("TTYP", safe(asText(pSt, "TRANS_TYPE")));
        pMap.put("PMKT", safe(asText(pSt, "PRIM_SEC_MKT")));
        // Note: Source field has typo "SECUIRTY_NO" - preserved to match Bloomberg feed
        pMap.put("SENO", safe(asText(pSt, "SECUIRTY_NO")));
        pMap.put("DEPO", safe(asText(pSt, "DEPOSITORY")));
    }

    /**
     * Maps date fields from SEC_TRADE.
     * Note: SEC_TRADE dates are already in yyyyMMdd format from Bloomberg.
     */
    private static void mapStTradeDates(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("TDDT", safe(asText(pSt, "TRADE_DATE")));
        pMap.put("VLDT", safe(asText(pSt, "VALUE_DATE")));
        pMap.put("ISDT", safe(asText(pSt, "ISSUE_DATE")));
        pMap.put("MTDT", safe(asText(pSt, "MATURITY_DATE")));
        pMap.put("IPDT", safe(asText(pSt, "INT_PAYMENT_DATE")));
    }

    /**
     * Maps currency and rate fields from SEC_TRADE.
     */
    private static void mapStCurrencyAndRates(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("TCCY", safe(asText(pSt, "TRADE_CCY")));
        pMap.put("IRTE", normalizeAmount(safe(asText(pSt, "INTEREST_RATE"))));
        pMap.put("IDYS", safe(asText(pSt, "INTEREST_DAYS")));
        pMap.put("IAMT", normalizeAmount(safe(asText(pSt, "INTEREST_AMOUNT"))));
        pMap.put("EXRT", normalizeAmount(safe(asText(pSt, "EXCH_RATE"))));
    }

    /**
     * Maps customer and portfolio fields from SEC_TRADE.
     */
    private static void mapStCustomerInfo(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("CUNO", safe(asText(pSt, "CUSTOMER_NO")));
        pMap.put("PFNO", safe(asText(pSt, "PORTFOLIO_NO")));
    }

    /**
     * Maps amount fields from SEC_TRADE.
     */
    private static void mapStAmounts(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("NOML", normalizeAmount(safe(asText(pSt, "NORMINAL"))));
        pMap.put("PRCE", normalizeAmount(safe(asText(pSt, "PRICE"))));
        pMap.put("COST", normalizeAmount(safe(asText(pSt, "COST"))));
        pMap.put("GAMT", normalizeAmount(safe(asText(pSt, "GROSS_AMT"))));
        pMap.put("NAMT", normalizeAmount(safe(asText(pSt, "NET_AMOUNT"))));
    }

    /**
     * Maps charge fields from SEC_TRADE.
     */
    private static void mapStCharges(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("CHCD", safe(asText(pSt, "CHARGE_CODE")));
        pMap.put("CHAM", normalizeAmount(safe(asText(pSt, "CHARGE_AMOUNT"))));
    }

    /**
     * Maps account fields from SEC_TRADE.
     */
    private static void mapStAccounts(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("CUAC", safe(asText(pSt, "CU_ACCOUNT_NO")));
        pMap.put("INAC", safe(asText(pSt, "INT_ACCT_NO")));
        pMap.put("PDAC", safe(asText(pSt, "PREM_DISC_ACCT")));
    }

    /**
     * Maps broker information fields from SEC_TRADE.
     */
    private static void mapStBrokerInfo(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("BRNO", safe(asText(pSt, "BROKER_NO")));
        pMap.put("DBAC", safe(asText(pSt, "DEPO_BR_ACCOUNT_NO")));
    }

    /**
     * Maps description/narration field from SEC_TRADE.
     */
    private static void mapStDescription(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("DESC", safe(asText(pSt, "DESCRIPTION")));
    }
}
