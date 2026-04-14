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
 * Last Modified: 2026-03-15
 * =============================================================================
 *
 * PURPOSE: Transformer utilities for Bloomberg SC flow.
 * Provides JSON normalization, field mapping, and business logic transformations.
 *
 * CURRENT TARGETS (after 2026 refactoring):
 * - SECURITY_MASTER → EB.CBN.INSTRUMENT.DETAIL.INT
 * - SEC_TRADE     → EB.CBN.BOND.TRADE.INT
 *
 * MODIFICATION HISTORY:
 * - 2026-01-07 | Initial creation for SECURITY_MASTER
 * - 2026-02-03 | Added SEC_TRADE support
 * - 2026-03-15 | Refactored for new target tables EB.CBN.INSTRUMENT.DETAIL.INT & EB.CBN.BOND.TRADE.INT
 *                Added NDIC_EURO_INVSTMENT_CASHACCT handling support
 * =============================================================================
 */
public final class CbnScMapper {

    private static final Logger LOGGER = Logger.getLogger(CbnScMapper.class.getName());
    private static final DateTimeFormatter INPUT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter OUTPUT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // JSON root node constants
    private static final String ROOT_SEC_TRADE   = "SEC_TRADE";
    private static final String ROOT_SEC_MASTER  = "SECURITY_MASTER";

    private CbnScMapper() {
        // utility class - no instances
    }

    // ========================================================================
    // MESSAGE TYPE DETECTION
    // ========================================================================

    public enum ScMessageType {
        SECURITY_MASTER, SEC_TRADE, UNKNOWN
    }

    public static ScMessageType getMessageType(JsonNode pRoot) {
        if (pRoot == null) return ScMessageType.UNKNOWN;

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

    public static String normalizeDateT24(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return dateStr;
        if (dateStr.matches("\\d{8}")) return dateStr; // already in T24 format

        try {
            return LocalDate.parse(dateStr, INPUT_FMT).format(OUTPUT_FMT);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Date normalization failed for: {0}", dateStr);
            return dateStr;
        }
    }

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
    // SECURITY_MASTER MAPPING (→ EB.CBN.INSTRUMENT.DETAIL.INT)
    // ========================================================================

    public static boolean hasSecurityMaster(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode fm = pRoot.get(ROOT_SEC_MASTER);
        return fm != null && !fm.isNull() && ((fm.isArray() && fm.size() > 0) || fm.isObject());
    }

    public static JsonNode getSecurityMasterAt(JsonNode pRoot, int pIndex) {
        JsonNode fm = pRoot.get(ROOT_SEC_MASTER);
        if (fm == null) return null;
        return fm.isArray() ? fm.get(pIndex) : (fm.isObject() ? fm : null);
    }

    public static Map<String, String> mapSecurityMasterToInstrumentDetail(JsonNode pFm) {
        LOGGER.log(Level.INFO, "[CbnScMapper] Mapping SECURITY_MASTER → EB.CBN.INSTRUMENT.DETAIL.INT");

        Map<String, String> map = new HashMap<>();

        map.put("CNME", safe(asText(pFm, "COMPANY_NAME")));
        map.put("SDES", safe(asText(pFm, "DESCRIPTION")));
        map.put("SNME", safe(asText(pFm, "SHORT_NAME")));
        map.put("MMNE", safe(asText(pFm, "MNEMONIC")));
        map.put("CDOM", safe(asText(pFm, "COMPANY_DOMICILE")));
        map.put("SDOM", safe(asText(pFm, "SECURITY_DOMICILE")));
        map.put("SCCY", safe(asText(pFm, "SECURITY_CURRENCY")));
        map.put("BOSH", safe(asText(pFm, "BOND_OR_SHARE")));
        map.put("SAST", safe(asText(pFm, "SUB_ASSET_TYPE")));
        map.put("PCCY", safe(asText(pFm, "PRICE_CURRENCY")));
        map.put("PTYP", safe(asText(pFm, "PRICE_TYPE")));
        map.put("LPRC", normalizeAmount(safe(asText(pFm, "LAST_PRICE"))));
        map.put("PCDE", safe(asText(pFm, "PRICE_UPDATE_CODE")));
        map.put("ICDE", safe(asText(pFm, "INDUSTRY_CODE")));
        map.put("SEXC", safe(asText(pFm, "STOCK_EXCHANGE")));
        map.put("CTAX", safe(asText(pFm, "COUPON_TAX_CODE")));
        map.put("BINT", safe(asText(pFm, "INTEREST_DAY_BASIS")));
        map.put("IRTE", normalizeAmount(safe(asText(pFm, "INTEREST_RATE"))));
        map.put("IDTE", normalizeDateT24(safe(asText(pFm, "ISSUE_DATE"))));
        map.put("MDTE", normalizeDateT24(safe(asText(pFm, "MATURITY_DATE"))));
        map.put("NPAY", safe(asText(pFm, "NO_OF_PAYMENT")));
        map.put("ADTE", normalizeDateT24(safe(asText(pFm, "ACCRUAL_START_DATE"))));
        map.put("PDTE", normalizeDateT24(safe(asText(pFm, "INT_PAYMENT_DATE"))));
        map.put("CDTE", normalizeDateT24(safe(asText(pFm, "FIRST_CPN_DATE"))));
        map.put("ISIN", safe(asText(pFm, "ISIN")));
        map.put("SDTE", normalizeDateT24(safe(asText(pFm, "SETUP_DATE"))));
        map.put("BLOOMBERG_ID", safe(asText(pFm, "BLOOMBERG_ID")));
        map.put("NDIC", safe(asText(pFm, "NDIC_INDICATOR")));

        LOGGER.log(Level.INFO, "[CbnScMapper] SECURITY_MASTER mapping complete: {0} fields", map.size());
        LOGGER.log(Level.FINE, "Mapped fields for SECURITY_MASTER: {0}", map);
        return map;
    }

    // ========================================================================
    // SEC_TRADE MAPPING (→ EB.CBN.BOND.TRADE.INT)
    // ========================================================================

    public static boolean hasSecTrade(JsonNode pRoot) {
        if (pRoot == null) return false;
        JsonNode st = pRoot.get(ROOT_SEC_TRADE);
        return st != null && !st.isNull() && ((st.isArray() && st.size() > 0) || st.isObject());
    }

    public static JsonNode getSecTradeAt(JsonNode pRoot, int pIndex) {
        JsonNode st = pRoot.get(ROOT_SEC_TRADE);
        if (st == null) return null;
        return st.isArray() ? st.get(pIndex) : (st.isObject() ? st : null);
    }

    public static Map<String, String> mapSecTradeToBondTradeInt(JsonNode pSt) {
        LOGGER.log(Level.INFO, "[CbnScMapper] Mapping SEC_TRADE → EB.CBN.BOND.TRADE.INT");

        Map<String, String> map = new HashMap<>();

        mapStTradeIdentifiers(pSt, map);
        mapStTradeDates(pSt, map);
        mapStCurrencyAndRates(pSt, map);
        mapStCustomerInfo(pSt, map);
        mapStAmounts(pSt, map);
        mapStCharges(pSt, map);
        mapStAccounts(pSt, map);
        mapStBrokerInfo(pSt, map);
        mapStDescription(pSt, map);
        mapStBloombergId(pSt, map);

        // Explicitly map the new NDIC cash account fields
        map.put("NDICINVCA", safe(asText(pSt, "NDIC_EURO_INVSTMENT_CASHACCT")));
        map.put("NDICLIBCA", safe(asText(pSt, "NDIC_EURO_LIAB_CASHACCT")));

        LOGGER.log(Level.INFO, "[CbnScMapper] SEC_TRADE mapping complete: {0} fields", map.size());
        return map;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helper mapping methods (unchanged except minor logging / null-safety)
    // ────────────────────────────────────────────────────────────────────────

    private static void mapStTradeIdentifiers(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("TTYP", safe(asText(pSt, "TRANS_TYPE")));
        pMap.put("PMKT", safe(asText(pSt, "PRIM_SEC_MKT")));
        pMap.put("SENO", safe(asText(pSt, "SECURITY_NO"))); 
        pMap.put("DEPO", safe(asText(pSt, "DEPOSITORY")));
    }

    private static void mapStTradeDates(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("TDDT", safe(asText(pSt, "TRADE_DATE")));
        pMap.put("VLDT", safe(asText(pSt, "VALUE_DATE")));
        pMap.put("ISDT", safe(asText(pSt, "ISSUE_DATE")));
        pMap.put("MTDT", safe(asText(pSt, "MATURITY_DATE")));
        pMap.put("IPDT", safe(asText(pSt, "INT_PAYMENT_DATE")));
    }

    private static void mapStCurrencyAndRates(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("TCCY", safe(asText(pSt, "TRADE_CCY")));
        pMap.put("IRTE", normalizeAmount(safe(asText(pSt, "INTEREST_RATE"))));
        pMap.put("IDYS", safe(asText(pSt, "INTEREST_DAYS")));
        pMap.put("IAMT", normalizeAmount(safe(asText(pSt, "INTEREST_AMOUNT"))));
        pMap.put("EXRT", normalizeAmount(safe(asText(pSt, "EXCH_RATE"))));
    }

    private static void mapStCustomerInfo(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("CUNO", safe(asText(pSt, "CUSTOMER_NO")));
        pMap.put("PFNO", safe(asText(pSt, "PORTFOLIO_NO")));
        
    }

    private static void mapStAmounts(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("NOML", normalizeAmount(safe(asText(pSt, "NORMINAL"))));
        pMap.put("PRCE", normalizeAmount(safe(asText(pSt, "PRICE"))));
        pMap.put("COST", normalizeAmount(safe(asText(pSt, "COST"))));
        pMap.put("GAMT", normalizeAmount(safe(asText(pSt, "GROSS_AMT"))));
        pMap.put("NAMT", normalizeAmount(safe(asText(pSt, "NET_AMOUNT"))));
    }

    private static void mapStCharges(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("CHCD", safe(asText(pSt, "CHARGE_CODE")));
        pMap.put("CHAM", normalizeAmount(safe(asText(pSt, "CHARGE_AMOUNT"))));
    }

    private static void mapStAccounts(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("CUAC", safe(asText(pSt, "CU_ACCOUNT_NO")));
        pMap.put("INAC", safe(asText(pSt, "INT_ACCT_NO")));
        pMap.put("PDAC", safe(asText(pSt, "PREM_DISC_ACCT")));
        // pMap.put("CSHACT", safe(asText(pSt, "CASH_ACCOUNT"))); // uncomment if needed
    }

    private static void mapStBrokerInfo(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("BRNO", safe(asText(pSt, "BROKER_NO")));
        pMap.put("DBAC", safe(asText(pSt, "DEPO_BR_ACCOUNT_NO")));
        pMap.put("NDINV",  safe(asText(pSt, "NDIC_EURO_INVSTMENT")));
        pMap.put("NDLIAB", safe(asText(pSt, "NDIC_EURO_LIAB")));
    }

    private static void mapStDescription(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("DESC",   safe(asText(pSt, "DESCRIPTION")));
        pMap.put("TXNDETS", safe(asText(pSt, "TRANSACTION_DETAILS")));
    }

    private static void mapStBloombergId(JsonNode pSt, Map<String, String> pMap) {
        pMap.put("BLOOMBERG_ID", safe(asText(pSt, "BLOOMBERG_ID")));
    }

    // ========================================================================
    // NDIC VERSION DECISION SUPPORT (to be used later in CbnScService)
    // ========================================================================

    /**
     * Determines whether the NDIC-specific version (NDIC.TRADDE) should be used
     * for SEC_TRADE messages.
     * 
     * Note: The condition check is prepared here but will be called from CbnScService later.
     */
    public static boolean shouldUseNdicTradeVersion(JsonNode secTradeNode) {
        if (secTradeNode == null) return false;

        String cashAcct = asText(secTradeNode, "NDIC_EURO_INVSTMENT_CASHACCT");

        boolean isPopulated = cashAcct != null && !cashAcct.trim().isEmpty();

        LOGGER.log(Level.FINE,
                "[CbnScMapper] NDIC_EURO_INVSTMENT_CASHACCT check: {0} → {1}",
                new Object[] { 
                    cashAcct, 
                    isPopulated ? "populated (NDIC.TRADDE)" : "empty (CBN.TRADDE)" 
                });

        return isPopulated;
    }
}