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
 * Title: CsdBloombergFxDserializer.java Author: CSD Development Team Date Created: 2025-10-11
 *
 * Purpose: Deserialization layer for Bloomberg FX feed (JSON → POJO). Contains no T24 APIs - only
 * Jackson-based deserialization helpers. Supports both FILE and WMQ (IBM MQ) adapters.
 * 
 * Usage: This class provides static utility methods for: - Reading JSON files or strings into typed
 * POJOs - Extracting transaction IDs from directories - Defining POJO classes for Bloomberg payload
 * structure
 * 
 * Modification Details: ---- 11/10/25 - Initial version Bloomberg FX deserialization with POJO
 * support ---- 09/11/25 - Updated to handle single FOREX_TRANSACTION object (not array) Compliant
 * with CSD Java Programming Standards r2022
 */
public final class CbnFxDserializer {

    // ==== STATIC CONSTANTS ====
    private static final Logger yLogger = Logger.getLogger(CbnFxDserializer.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CbnFxDserializer() {
        // Utility class - no instances allowed
    }

    /**
     * Reads a Bloomberg JSON file into a typed payload. Used by FILE adapter.
     * 
     * @param pFile Path to the JSON file
     * @param pObjMapper ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if file read or parse fails
     */
    public static BloombergPayload readPayload(Path pFile, ObjectMapper pObjMapper)
            throws IOException {
        String content = new String(Files.readAllBytes(pFile), StandardCharsets.UTF_8);
        if (content.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(content, pObjMapper);
    }

    /**
     * Parses a Bloomberg JSON string into a typed payload. Used by WMQ adapter. NOW HANDLES SINGLE
     * FOREX_TRANSACTION OBJECT (not array)
     * 
     * @param pJson JSON string
     * @param pObjMapper ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    public static BloombergPayload readPayload(String pJson, ObjectMapper pObjMapper)
            throws IOException {
        if (pJson == null || pJson.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(pJson, pObjMapper);
    }

    /**
     * Internal method to parse JSON and handle both single object and array formats. Client
     * requirement: Now expects single FOREX_TRANSACTION object only.
     * 
     * @param pJson JSON string
     * @param pObjMapper ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    private static BloombergPayload parsePayload(String pJson, ObjectMapper pObjMapper)
            throws IOException {
        yLogger.log(Level.INFO,
                "[CsdBloombergFxDserializer] Starting deserialization of JSON message");

        // Parse the JSON
        JsonNode rootNode = pObjMapper.readTree(pJson);
        yLogger.log(Level.FINE, "[CsdBloombergFxDserializer] JSON parsed successfully");

        // Check if FOREX_TRANSACTION exists
        if (!rootNode.has("FOREX_TRANSACTION")) {
            yLogger.log(Level.SEVERE,
                    "[CsdBloombergFxDserializer] FOREX_TRANSACTION field not found in message");
            throw new IOException("FOREX_TRANSACTION field not found in JSON");
        }

        JsonNode forexNode = rootNode.get("FOREX_TRANSACTION");

        // Validate it's not null
        if (forexNode == null || forexNode.isNull()) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxDserializer] FOREX_TRANSACTION is null");
            throw new IOException("FOREX_TRANSACTION is null");
        }

        BloombergPayload payload = new BloombergPayload();

        // Handle single object format (NEW CLIENT REQUIREMENT)
        if (forexNode.isObject()) {
            yLogger.log(Level.INFO,
                    "[CsdBloombergFxDserializer] FOREX_TRANSACTION found as single object - proceeding with deserialization");

            ForexTransact transaction = pObjMapper.treeToValue(forexNode, ForexTransact.class);

            if (transaction != null) {
                List<ForexTransact> transactions = new ArrayList<>();
                transactions.add(transaction);
                payload.setForexTransact(transactions);

                yLogger.log(Level.INFO,
                        "[CsdBloombergFxDserializer] Deserialization successful - Counterparty: {0}, Deal Type: {1}, Currency Bought: {2}",
                        new Object[] { transaction.getCounterparty(), transaction.getDealType(),
                                transaction.getCurrencyBought() });
            } else {
                yLogger.log(Level.SEVERE,
                        "[CsdBloombergFxDserializer] Deserialization returned null transaction");
                throw new IOException("Failed to deserialize FOREX_TRANSACTION object");
            }

        } else if (forexNode.isArray()) {
            // Reject array format as per new client requirement
            yLogger.log(Level.SEVERE,
                    "[CsdBloombergFxDserializer] FOREX_TRANSACTION is an array - expected single object only");
            throw new IOException("FOREX_TRANSACTION must be a single object, not an array");

        } else {
            yLogger.log(Level.SEVERE,
                    "[CsdBloombergFxDserializer] FOREX_TRANSACTION is not a valid object");
            throw new IOException("FOREX_TRANSACTION is not a valid JSON object");
        }

        return payload;
    }

    /**
     * Enumerates synthetic IDs from a directory by counting FOREX_TRANSACTION. Used by FILE
     * adapter.
     * 
     * @param pDirectory Directory to scan
     * @param pDirGlob File glob pattern (e.g., "*.json")
     * @param pObjMapper ObjectMapper for deserialization
     * @return List of synthetic transaction IDs
     */
    public static List<String> scanDirectoryIds(Path pDirectory, String pDirGlob,
            ObjectMapper pObjMapper) {
        List<String> ids = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pDirectory, pDirGlob)) {
            for (Path file : stream) {
                try {
                    BloombergPayload payload = readPayload(file, pObjMapper);
                    int size = payload.getForexTransact() == null ? 0
                            : payload.getForexTransact().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    yLogger.log(Level.INFO,
                            "[CsdBloombergFxDserializer] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    yLogger.log(Level.WARNING,
                            "[CsdBloombergFxDserializer] FILE: Skipping file due to read/parse error: {0}",
                            new Object[] { file });
                }
            }
        } catch (IOException e) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxDserializer] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====
/**
 * Root payload object representing the Bloomberg JSON structure. Contains a list of forex
 * transactions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {

    @JsonProperty("FOREX_TRANSACTION")
    private List<ForexTransact> mForexTransact = new ArrayList<>();

    /**
     * Gets the list of forex transactions.
     * 
     * @return List of ForexTransact objects
     */
    public List<ForexTransact> getForexTransact() {
        return mForexTransact;
    }

    /**
     * Sets the list of forex transactions.
     * 
     * @param pForexTransact List of ForexTransact objects
     */
    public void setForexTransact(List<ForexTransact> pForexTransact) {
        this.mForexTransact = pForexTransact;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (mForexTransact == null ? 0 : mForexTransact.size())
                + "}";
    }
}

/**
 * POJO representing a single forex transaction. Maps to the FOREX_TRANSACTION object in the
 * Bloomberg JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ForexTransact {

    // FX deal specific fields
    @JsonProperty("COUNTERPARTY")
    private String mCounterparty;

    @JsonProperty("DEAL_TYPE")
    private String mDealType;

    @JsonProperty("DEALER_DESK")
    private String mDealerDesk;

    @JsonProperty("DEAL_DATE")
    private String mDealDate;

    @JsonProperty("CURRENCY_BOUGHT")
    private String mCurrencyBought;

    @JsonProperty("BUY_AMOUNT")
    private String mBuyAmount;

    @JsonProperty("VALUE_DATE_BUY")
    private String mValueDateBuy;

    @JsonProperty("CURRENCY_SOLD")
    private String mCurrencySold;

    @JsonProperty("SELL_AMOUNT")
    private String mSellAmount;

    @JsonProperty("VALUE_DATE_SELL")
    private String mValueDateSell;

    @JsonProperty("SPOT_RATE")
    private String mSpotRate;

    @JsonProperty("SPOT_DATE")
    private String mSpotDate;

    @JsonProperty("BASE_CCY")
    private String mBaseCcy;

    @JsonProperty("BROKER")
    private String mBroker;

    @JsonProperty("DEALER_NOTES")
    private String mDealerNotes;

    @JsonProperty("OUR_ACCOUNT_PAY")
    private String mOurAccountPay;

    @JsonProperty("OUR_ACCOUNT_REC")
    private String mOurAccountRec;

    @JsonProperty("CPARTY_CORR_NO")
    private String mCpartyCorrNo;

    @JsonProperty("CPY_CORR_ADD")
    private String mCpyCorrAdd;

    @JsonProperty("CPARTY_BANK_ACC")
    private String mCpartyBankAcc;

    @JsonProperty("BK_TO_BK_INF")
    private String mBkToBkInf;

    @JsonProperty("INT_RATE_BUY")
    private String mIntRateBuy;

    @JsonProperty("INT_RATE_SELL")
    private String mIntRateSell;

    // Forward deal specific fields
    @JsonProperty("FORWARD_RATE")
    private String mForwardRate;

    // SW deal specific fields
    @JsonProperty("SWAP_BASE_CCY")
    private String mSwapBaseCcy;

    @JsonProperty("LEG1_FWD_RATE")
    private String mLeg1FwdRate;

    @JsonProperty("FORWARD_VALUE_DATE_BUY")
    private String mFwValueDateBuy;

    @JsonProperty("FORWARD_VALUE_DATE_SELL")
    private String mFwValueDateSell;

    @JsonProperty("FWD_FWD_SWAP")
    private String mFwdFwdSwap;

    @JsonProperty("UNEVEN_SWAP")
    private String mUnevenSwap;

    @JsonProperty("SWAP_REF_NO")
    private List<String> mSwapRefNo;

    // SD deal specific fields
    @JsonProperty("RATE")
    private String mRate;

    @JsonProperty("PAY_REC_ACCOUNT")
    private String mPayRecAccount;

    @JsonProperty("STATUS")
    private String mStatus;

    @JsonProperty("RTGS_ID")
    private String mRtgsId;

    @JsonProperty("COMPLETED_ID")
    private String mCompletedId;

    @JsonProperty("BUY_RATE")
    private String mBuyRate;

    @JsonProperty("SELL_RATE")
    private String mSellRate;

    @JsonProperty("MID_RATE")
    private String mMidRate;

    @JsonProperty("MDC_ERROR")
    private String mMdcError;

    // ==== GETTERS AND SETTERS ====
    public String getCounterparty() {
        return mCounterparty;
    }

    public void setCounterparty(String pCounterParty) {
        this.mCounterparty = pCounterParty;
    }

    public String getDealType() {
        return mDealType;
    }

    public void setDealType(String pDealType) {
        this.mDealType = pDealType;
    }

    public String getDealerDesk() {
        return mDealerDesk;
    }

    public void setDealerDesk(String pDealerDesk) {
        this.mDealerDesk = pDealerDesk;
    }

    public String getDealDate() {
        return mDealDate;
    }

    public void setDealDate(String pDealDate) {
        this.mDealDate = pDealDate;
    }

    public String getCurrencyBought() {
        return mCurrencyBought;
    }

    public void setCurrencyBought(String pCurrencyBought) {
        this.mCurrencyBought = pCurrencyBought;
    }

    public String getBuyAmount() {
        return mBuyAmount;
    }

    public void setBuyAmount(String pBuyAmount) {
        this.mBuyAmount = pBuyAmount;
    }

    public String getValueDateBuy() {
        return mValueDateBuy;
    }

    public void setValueDateBuy(String pValueDateBuy) {
        this.mValueDateBuy = pValueDateBuy;
    }

    public String getCurrencySold() {
        return mCurrencySold;
    }

    public void setCurrencySold(String pCurrencySold) {
        this.mCurrencySold = pCurrencySold;
    }

    public String getSellAmount() {
        return mSellAmount;
    }

    public void setSellAmount(String pSellAmount) {
        this.mSellAmount = pSellAmount;
    }

    public String getValueDateSell() {
        return mValueDateSell;
    }

    public void setValueDateSell(String pValueDateSell) {
        this.mValueDateSell = pValueDateSell;
    }

    public String getSpotRate() {
        return mSpotRate;
    }

    public void setSpotRate(String pSpotRate) {
        this.mSpotRate = pSpotRate;
    }

    public String getSpotDate() {
        return mSpotDate;
    }

    public void setSpotDate(String pSpotDate) {
        this.mSpotDate = pSpotDate;
    }

    public String getSwapBaseCcy() {
        return mSwapBaseCcy;
    }

    public void setSwapBaseCcy(String pSwapBaseCcy) {
        this.mSwapBaseCcy = pSwapBaseCcy;
    }

    public String getBaseCcy() {
        return mBaseCcy;
    }

    public void setBaseCcy(String pBaseCcy) {
        this.mBaseCcy = pBaseCcy;
    }

    public String getBroker() {
        return mBroker;
    }

    public void setBroker(String pBroker) {
        this.mBroker = pBroker;
    }

    public String getDealerNotes() {
        return mDealerNotes;
    }

    public void setDealerNotes(String pDealerNotes) {
        this.mDealerNotes = pDealerNotes;
    }

    public String getOurAccountPay() {
        return mOurAccountPay;
    }

    public void setOurAccountPay(String pOurAccountPay) {
        this.mOurAccountPay = pOurAccountPay;
    }

    public String getOurAccountRec() {
        return mOurAccountRec;
    }

    public void setOurAccountRec(String pOurAccountRec) {
        this.mOurAccountRec = pOurAccountRec;
    }

    public String getCpartyCorrNo() {
        return mCpartyCorrNo;
    }

    public void setCpartyCorrNo(String pCpartyCorrNo) {
        this.mCpartyCorrNo = pCpartyCorrNo;
    }

    public String getCpyCorrAdd() {
        return mCpyCorrAdd;
    }

    public void setCpyCorrAdd(String pCpyCorrAdd) {
        this.mCpyCorrAdd = pCpyCorrAdd;
    }

    public String getCpartyBankAcc() {
        return mCpartyBankAcc;
    }

    public void setCpartyBankAcc(String pCpartyBankAcc) {
        this.mCpartyBankAcc = pCpartyBankAcc;
    }

    public String getBkToBkInf() {
        return mBkToBkInf;
    }

    public void setBkToBkInf(String pBkToBkInf) {
        this.mBkToBkInf = pBkToBkInf;
    }

    public String getIntRateBuy() {
        return mIntRateBuy;
    }

    public void setIntRateBuy(String pIntRateBuy) {
        this.mIntRateBuy = pIntRateBuy;
    }

    public String getIntRateSell() {
        return mIntRateSell;
    }

    public void setIntRateSell(String pIntRateSell) {
        this.mIntRateSell = pIntRateSell;
    }

    public String getForwardRate() {
        return mForwardRate;
    }

    public void setForwardRate(String pForwardRate) {
        this.mForwardRate = pForwardRate;
    }

    public String getLeg1FwdRate() {
        return mLeg1FwdRate;
    }

    public void setLeg1FwdRate(String pLeg1FwdRate) {
        this.mLeg1FwdRate = pLeg1FwdRate;
    }

    public String getForwardValueDateBuy() {
        return mFwValueDateBuy;
    }

    public void setForwardValueDateBuy(String pFwValueDateBuy) {
        this.mFwValueDateBuy = pFwValueDateBuy;
    }

    public String getForwardValueDateSell() {
        return mFwValueDateSell;
    }

    public void setForwardValueDateSell(String pFwValueDateSell) {
        this.mFwValueDateSell = pFwValueDateSell;
    }

    public String getFwdFwdSwap() {
        return mFwdFwdSwap;
    }

    public void setFwdFwdSwap(String pFwdFwdSwap) {
        this.mFwdFwdSwap = pFwdFwdSwap;
    }

    public String getUnevenSwap() {
        return mUnevenSwap;
    }

    public void setUnevenSwap(String pUnevenSwap) {
        this.mUnevenSwap = pUnevenSwap;
    }

    public List<String> getSwapRefNo() {
        return mSwapRefNo;
    }

    public void setSwapRefNo(List<String> pSwapRefNo) {
        this.mSwapRefNo = pSwapRefNo;
    }

    // SD deal specific setters and getters
    public String getRate() {
        return mRate;
    }

    public void setRate(String pRate) {
        this.mRate = pRate;
    }

    public String getPayRecAccount() {
        return mPayRecAccount;
    }

    public void setPayRecAccount(String pPayRecAccount) {
        this.mPayRecAccount = pPayRecAccount;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String pStatus) {
        this.mStatus = pStatus;
    }

    public String getRtgsId() {
        return mRtgsId;
    }

    public void setRtgsId(String pRtgsId) {
        this.mRtgsId = pRtgsId;
    }

    public String getCompletedId() {
        return mCompletedId;
    }

    public void setCompletedId(String pCompletedId) {
        this.mCompletedId = pCompletedId;
    }

    public String getBuyRate() {
        return mBuyRate;
    }

    public void setBuyRate(String pBuyRate) {
        this.mBuyRate = pBuyRate;
    }

    public String getSellRate() {
        return mSellRate;
    }

    public void setSellRate(String pSellRate) {
        this.mSellRate = pSellRate;
    }

    public String getMidRate() {
        return mMidRate;
    }

    public void setMidRate(String pMidRate) {
        this.mMidRate = pMidRate;
    }

    public String getMdcError() {
        return mMdcError;
    }

    public void setMdcError(String pMdcError) {
        this.mMdcError = pMdcError;
    }

    @Override
    public String toString() {
        return "ForexTransact{" + "counterparty='" + mCounterparty + '\'' + ", dealType='"
                + mDealType + '\'' + ", currencyBought='" + mCurrencyBought + '\''
                + ", currencySold='" + mCurrencySold + '\'' + ", buyAmount='" + mBuyAmount + '\''
                + ", sellAmount='" + mSellAmount + '\'' + '}';
    }
}