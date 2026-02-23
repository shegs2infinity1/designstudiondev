package com.cbn.bloomberg.fx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Title: CsdBloombergFxDserializer.java Author: CSD Development Team Date
 * Created: 2025-10-11
 *
 * Purpose: Deserialization layer for Bloomberg FX feed (JSON → POJO). Contains
 * no T24 APIs - only Jackson-based deserialization helpers. Supports both FILE
 * and WMQ (IBM MQ) adapters.
 * 
 * Usage: This class provides static utility methods for: - Reading JSON files
 * or strings into typed POJOs - Extracting transaction IDs from directories -
 * Defining POJO classes for Bloomberg payload structure
 * 
 * Modification Details: ---- 11/10/25 - Initial version Bloomberg FX
 * deserialization with POJO support ---- 09/11/25 - Updated to handle single
 * FOREX_TRANSACTION object (not array) Compliant with CSD Java Programming
 * Standards r2022
 */
public final class CsdBloombergFxDserialize {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFxDserialize.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CsdBloombergFxDserialize() {
        // Utility class - no instances allowed
    }

    /**
     * Reads a Bloomberg JSON file into a typed payload. Used by FILE adapter.
     * 
     * @param p_file Path to the JSON file
     * @param p_om   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if file read or parse fails
     */
    public static BloombergPayload readPayload(Path p_file, ObjectMapper p_om) throws IOException {
        String content = new String(Files.readAllBytes(p_file), StandardCharsets.UTF_8);
        if (content.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(content, p_om);
    }

    /**
     * Parses a Bloomberg JSON string into a typed payload. Used by WMQ adapter. NOW
     * HANDLES SINGLE FOREX_TRANSACTION OBJECT (not array)
     * 
     * @param p_json JSON string
     * @param p_om   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    public static BloombergPayload readPayload(String p_json, ObjectMapper p_om) throws IOException {
        if (p_json == null || p_json.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(p_json, p_om);
    }

    /**
     * Internal method to parse JSON and handle both single object and array
     * formats. Client requirement: Now expects single FOREX_TRANSACTION object
     * only.
     * 
     * @param p_json JSON string
     * @param p_om   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    private static BloombergPayload parsePayload(String p_json, ObjectMapper p_om) throws IOException {
        LOGGER.log(Level.INFO, "[CsdBloombergFxDserializer] Starting deserialization of JSON message");

        // Parse the JSON
        JsonNode rootNode = p_om.readTree(p_json);
        LOGGER.log(Level.FINE, "[CsdBloombergFxDserializer] JSON parsed successfully");

        // Check if FOREX_TRANSACTION exists
        if (!rootNode.has("FOREX_TRANSACTION")) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxDserializer] FOREX_TRANSACTION field not found in message");
            throw new IOException("FOREX_TRANSACTION field not found in JSON");
        }

        JsonNode forexNode = rootNode.get("FOREX_TRANSACTION");

        // Validate it's not null
        if (forexNode == null || forexNode.isNull()) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxDserializer] FOREX_TRANSACTION is null");
            throw new IOException("FOREX_TRANSACTION is null");
        }

        BloombergPayload payload = new BloombergPayload();

        // Handle single object format (NEW CLIENT REQUIREMENT)
        if (forexNode.isObject()) {
            LOGGER.log(Level.INFO,
                    "[CsdBloombergFxDserializer] FOREX_TRANSACTION found as single object - proceeding with deserialization");

            ForexTransact transaction = p_om.treeToValue(forexNode, ForexTransact.class);

            if (transaction != null) {
                List<ForexTransact> transactions = new ArrayList<>();
                transactions.add(transaction);
                payload.setForexTransact(transactions);

                LOGGER.log(Level.INFO,
                        "[CsdBloombergFxDserializer] Deserialization successful - Counterparty: {0}, Deal Type: {1}, Currency Bought: {2}",
                        new Object[] { transaction.getCounterparty(), transaction.getDealType(),
                                transaction.getCurrencyBought() });
            } else {
                LOGGER.log(Level.SEVERE, "[CsdBloombergFxDserializer] Deserialization returned null transaction");
                throw new IOException("Failed to deserialize FOREX_TRANSACTION object");
            }

        } else if (forexNode.isArray()) {
            // Reject array format as per new client requirement
            LOGGER.log(Level.SEVERE,
                    "[CsdBloombergFxDserializer] FOREX_TRANSACTION is an array - expected single object only");
            throw new IOException("FOREX_TRANSACTION must be a single object, not an array");

        } else {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxDserializer] FOREX_TRANSACTION is not a valid object");
            throw new IOException("FOREX_TRANSACTION is not a valid JSON object");
        }

        return payload;
    }

    /**
     * Enumerates synthetic IDs from a directory by counting FOREX_TRANSACTION. Used
     * by FILE adapter.
     * 
     * @param p_dir  Directory to scan
     * @param p_glob File glob pattern (e.g., "*.json")
     * @param p_om   ObjectMapper for deserialization
     * @return List of synthetic transaction IDs
     */
    public static List<String> extractIdsFromDirectory(Path p_dir, String p_glob, ObjectMapper p_om) {
        List<String> ids = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p_dir, p_glob)) {
            for (Path file : stream) {
                try {
                    BloombergPayload payload = readPayload(file, p_om);
                    int size = payload.getForexTransact() == null ? 0 : payload.getForexTransact().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    LOGGER.log(Level.INFO, "[CsdBloombergFxDserializer] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[CsdBloombergFxDserializer] FILE: Skipping file due to read/parse error: {0}",
                            new Object[] { file });
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFxDserializer] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====

/**
 * Root payload object representing the Bloomberg JSON structure. Contains a
 * list of forex transactions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {
    @JsonProperty("FOREX_TRANSACTION")
    private List<ForexTransact> m_ForexTransact = new ArrayList<>();

    /**
     * Gets the list of forex transactions.
     * 
     * @return List of ForexTransact objects
     */
    public List<ForexTransact> getForexTransact() {
        return m_ForexTransact;
    }

    /**
     * Sets the list of forex transactions.
     * 
     * @param p_ForexTransact List of ForexTransact objects
     */
    public void setForexTransact(List<ForexTransact> p_ForexTransact) {
        this.m_ForexTransact = p_ForexTransact;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (m_ForexTransact == null ? 0 : m_ForexTransact.size()) + "}";
    }
}

/**
 * POJO representing a single forex transaction. Maps to the FOREX_TRANSACTION
 * object in the Bloomberg JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ForexTransact {

    @JsonProperty("COUNTERPARTY")
    private String m_counterparty;

    @JsonProperty("DEAL_TYPE")
    private String m_dealType;

    @JsonProperty("DEALER_DESK")
    private String m_dealerDesk;

    @JsonProperty("DEAL_DATE")
    private String m_dealDate;

    @JsonProperty("CURRENCY_BOUGHT")
    private String m_currencyBought;

    @JsonProperty("BUY_AMOUNT")
    private String m_buyAmount;

    @JsonProperty("VALUE_DATE_BUY")
    private String m_valueDateBuy;

    @JsonProperty("CURRENCY_SOLD")
    private String m_currencySold;

    @JsonProperty("SELL_AMOUNT")
    private String m_sellAmount;

    @JsonProperty("VALUE_DATE_SELL")
    private String m_valueDateSell;

    @JsonProperty("SPOT_RATE")
    private String m_spotRate;

    @JsonProperty("SPOT_DATE")
    private String m_spotDate;

    @JsonProperty("SWAP_BASE_CCY")
    private String m_swapBaseCcy;

    @JsonProperty("BASE_CCY")
    private String m_baseCcy;

    @JsonProperty("BROKER")
    private String m_broker;

    @JsonProperty("DEALER_NOTES")
    private String m_dealerNotes;

    @JsonProperty("OUR_ACCOUNT_PAY")
    private String m_ourAccountPay;

    @JsonProperty("CPARTY_CORR_NO")
    private String m_cpartyCorrNo;

    @JsonProperty("CPY_CORR_ADD")
    private String m_cpyCorrAdd;

    @JsonProperty("CPARTY_BANK_ACC")
    private String m_cpartyBankAcc;

    @JsonProperty("BK_TO_BK_INF")
    private String m_bkToBkInf;

    @JsonProperty("INT_RATE_BUY")
    private String m_intRateBuy;

    @JsonProperty("INT_RATE_SELL")
    private String m_intRateSell;

    // Forward deal specific fields
    @JsonProperty("FORWARD_RATE")
    private String m_forwardRate;

    // Swap deal specific fields
    @JsonProperty("LEG1_FWD_RATE")
    private String m_leg1FwdRate;

    @JsonProperty("FORWARD_VALUE_DATE_BUY")
    private String m_forwardValueDateBuy;

    @JsonProperty("FORWARD_VALUE_DATE_SELL")
    private String m_forwardValueDateSell;

    @JsonProperty("FWD_FWD_SWAP")
    private String m_fwdFwdSwap;

    @JsonProperty("UNEVEN_SWAP")
    private String m_unevenSwap;

    @JsonProperty("SWAP_REF_NO")
    private List<String> m_swapRefNo;

    @JsonProperty("OUR_ACCOUNT_REC")
    private String m_ourAccountRec;

    // ==== GETTERS AND SETTERS ====

    public String getCounterparty() {
        return m_counterparty;
    }

    public void setCounterparty(String p_counterparty) {
        this.m_counterparty = p_counterparty;
    }

    public String getDealType() {
        return m_dealType;
    }

    public void setDealType(String p_dealType) {
        this.m_dealType = p_dealType;
    }

    public String getDealerDesk() {
        return m_dealerDesk;
    }

    public void setDealerDesk(String p_dealerDesk) {
        this.m_dealerDesk = p_dealerDesk;
    }

    public String getDealDate() {
        return m_dealDate;
    }

    public void setDealDate(String p_dealDate) {
        this.m_dealDate = p_dealDate;
    }

    public String getCurrencyBought() {
        return m_currencyBought;
    }

    public void setCurrencyBought(String p_currencyBought) {
        this.m_currencyBought = p_currencyBought;
    }

    public String getBuyAmount() {
        return m_buyAmount;
    }

    public void setBuyAmount(String p_buyAmount) {
        this.m_buyAmount = p_buyAmount;
    }

    public String getValueDateBuy() {
        return m_valueDateBuy;
    }

    public void setValueDateBuy(String p_valueDateBuy) {
        this.m_valueDateBuy = p_valueDateBuy;
    }

    public String getCurrencySold() {
        return m_currencySold;
    }

    public void setCurrencySold(String p_currencySold) {
        this.m_currencySold = p_currencySold;
    }

    public String getSellAmount() {
        return m_sellAmount;
    }

    public void setSellAmount(String p_sellAmount) {
        this.m_sellAmount = p_sellAmount;
    }

    public String getValueDateSell() {
        return m_valueDateSell;
    }

    public void setValueDateSell(String p_valueDateSell) {
        this.m_valueDateSell = p_valueDateSell;
    }

    public String getSpotRate() {
        return m_spotRate;
    }

    public void setSpotRate(String p_spotRate) {
        this.m_spotRate = p_spotRate;
    }

    public String getSpotDate() {
        return m_spotDate;
    }

    public void setSpotDate(String p_spotDate) {
        this.m_spotDate = p_spotDate;
    }

    public String getSwapBaseCcy() {
        return m_swapBaseCcy;
    }

    public void setSwapBaseCcy(String p_swapBaseCcy) {
        this.m_swapBaseCcy = p_swapBaseCcy;
    }

    public String getBaseCcy() {
        return m_baseCcy;
    }

    public void setBaseCcy(String p_baseCcy) {
        this.m_baseCcy = p_baseCcy;
    }

    public String getBroker() {
        return m_broker;
    }

    public void setBroker(String p_broker) {
        this.m_broker = p_broker;
    }

    public String getDealerNotes() {
        return m_dealerNotes;
    }

    public void setDealerNotes(String p_dealerNotes) {
        this.m_dealerNotes = p_dealerNotes;
    }

    public String getOurAccountPay() {
        return m_ourAccountPay;
    }

    public void setOurAccountPay(String p_ourAccountPay) {
        this.m_ourAccountPay = p_ourAccountPay;
    }

    public String getCpartyCorrNo() {
        return m_cpartyCorrNo;
    }

    public void setCpartyCorrNo(String p_cpartyCorrNo) {
        this.m_cpartyCorrNo = p_cpartyCorrNo;
    }

    public String getCpyCorrAdd() {
        return m_cpyCorrAdd;
    }

    public void setCpyCorrAdd(String p_cpyCorrAdd) {
        this.m_cpyCorrAdd = p_cpyCorrAdd;
    }

    public String getCpartyBankAcc() {
        return m_cpartyBankAcc;
    }

    public void setCpartyBankAcc(String p_cpartyBankAcc) {
        this.m_cpartyBankAcc = p_cpartyBankAcc;
    }

    public String getBkToBkInf() {
        return m_bkToBkInf;
    }

    public void setBkToBkInf(String p_bkToBkInf) {
        this.m_bkToBkInf = p_bkToBkInf;
    }

    public String getIntRateBuy() {
        return m_intRateBuy;
    }

    public void setIntRateBuy(String p_intRateBuy) {
        this.m_intRateBuy = p_intRateBuy;
    }

    public String getIntRateSell() {
        return m_intRateSell;
    }

    public void setIntRateSell(String p_intRateSell) {
        this.m_intRateSell = p_intRateSell;
    }

    public String getForwardRate() {
        return m_forwardRate;
    }

    public void setForwardRate(String p_forwardRate) {
        this.m_forwardRate = p_forwardRate;
    }

    public String getLeg1FwdRate() {
        return m_leg1FwdRate;
    }

    public void setLeg1FwdRate(String p_leg1FwdRate) {
        this.m_leg1FwdRate = p_leg1FwdRate;
    }

    public String getForwardValueDateBuy() {
        return m_forwardValueDateBuy;
    }

    public void setForwardValueDateBuy(String p_forwardValueDateBuy) {
        this.m_forwardValueDateBuy = p_forwardValueDateBuy;
    }

    public String getForwardValueDateSell() {
        return m_forwardValueDateSell;
    }

    public void setForwardValueDateSell(String p_forwardValueDateSell) {
        this.m_forwardValueDateSell = p_forwardValueDateSell;
    }

    public String getFwdFwdSwap() {
        return m_fwdFwdSwap;
    }

    public void setFwdFwdSwap(String p_fwdFwdSwap) {
        this.m_fwdFwdSwap = p_fwdFwdSwap;
    }

    public String getUnevenSwap() {
        return m_unevenSwap;
    }

    public void setUnevenSwap(String p_unevenSwap) {
        this.m_unevenSwap = p_unevenSwap;
    }

    public List<String> getSwapRefNo() {
        return m_swapRefNo;
    }

    public void setSwapRefNo(List<String> p_swapRefNo) {
        this.m_swapRefNo = p_swapRefNo;
    }

    public String getOurAccountRec() {
        return m_ourAccountRec;
    }

    public void setOurAccountRec(String p_ourAccountRec) {
        this.m_ourAccountRec = p_ourAccountRec;
    }

    @Override
    public String toString() {
        return "ForexTransact{" + "counterparty='" + m_counterparty + '\'' + ", dealType='" + m_dealType + '\''
                + ", currencyBought='" + m_currencyBought + '\'' + ", currencySold='" + m_currencySold + '\''
                + ", buyAmount='" + m_buyAmount + '\'' + ", sellAmount='" + m_sellAmount + '\'' + '}';
    }
}