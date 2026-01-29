package com.cbn.bloomberg.pd;

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
 * Title: CsdBloombergPdDserialize.java Author: CSD Development Team Date Created: 2025-10-11
 *
 * Purpose: Deserialization layer for Bloomberg PD feed (JSON → POJO). Contains no T24 APIs - only
 * Jackson-based deserialization helpers. Supports both FILE and WMQ (IBM MQ) adapters.
 *
 * Usage: This class provides static utility methods for: - Reading JSON files or strings into typed
 * POJOs - Extracting transaction IDs from directories - Defining POJO classes for Bloomberg payload
 * structure
 *
 * Modification Details: ---- 11/10/25 - Initial version Bloomberg PD deserialization with POJO
 * support ---- 09/11/25 - Updated to handle single PLACEMENTS object (not array) Compliant with CSD
 * Java Programming Standards r2022
 */
public final class CsdBloombergPdDserialize {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CsdBloombergPdDserialize.class.getName());

    /**
    * Private constructor to prevent instantiation of utility class.
    */
    private CsdBloombergPdDserialize() {
        // Utility class - no instances allowed
    }

    /**
    * Reads a Bloomberg JSON file into a typed payload. Used by FILE adapter.
    *
    * @param pFile Path to the JSON file
    * @param pObjMap ObjectMapper for deserialization
    * @return BloombergPayload object
    * @throws IOException if file read or parse fails 
    */
    public static BloombergPayload readPayload(Path pFile, ObjectMapper pObjMap)
            throws IOException {
        String content = new String(Files.readAllBytes(pFile), StandardCharsets.UTF_8);
        if (content.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(content, pObjMap);
    }

    /**
    * Parses a Bloomberg JSON string into a typed payload. Used by WMQ adapter. NOW HANDLES SINGLE
    * PLACEMENTS OBJECT (not array)
    *
    * @param pJson JSON string
    * @param pObjMap ObjectMapper for deserialization
    * @return BloombergPayload object
    * @throws IOException if parse fails
    */
    public static BloombergPayload readPayload(String pJson, ObjectMapper pObjMap)
            throws IOException {
        if (pJson == null || pJson.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(pJson, pObjMap);
    }

    /**
    * Internal method to parse JSON and handle both single object and array formats. Client
    * requirement: Now expects single PLACEMENTS object only.
    *
    * @param pJson JSON string
    * @param pObjMap ObjectMapper for deserialization
    * @return BloombergPayload object
    * @throws IOException if parse fails
    */
    private static BloombergPayload parsePayload(String pJson, ObjectMapper pObjMap)
            throws IOException {
        LOGGER.log(Level.INFO,
                "[CsdBloombergPdDserialize] Starting deserialization of JSON message");

        // Parse the JSON
        JsonNode rootNode = pObjMap.readTree(pJson);
        LOGGER.log(Level.FINE, "[CsdBloombergPdDserialize] JSON parsed successfully");

        // Check if PLACEMENTS exists
        if (!rootNode.has("PLACEMENTS")) {
            LOGGER.log(Level.SEVERE,
                    "[CsdBloombergPdDserialize] PLACEMENTS field not found in message");
            throw new IOException("PLACEMENTS field not found in JSON");
        }

        JsonNode repoNode = rootNode.get("PLACEMENTS");

        // Validate it's not null
        if (repoNode == null || repoNode.isNull()) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdDserialize] PLACEMENTS is null");
            throw new IOException("PLACEMENTS is null");
        }

        BloombergPayload payload = new BloombergPayload();

        // Handle single object format (NEW CLIENT REQUIREMENT)
        if (repoNode.isObject()) {
            LOGGER.log(Level.INFO,
                    "[CsdBloombergPdDserialize] PLACEMENTS found as single object - proceeding with deserialization");

            Placements transaction = pObjMap.treeToValue(repoNode, Placements.class);

            if (transaction != null) {
                List<Placements> transactions = new ArrayList<>();
                transactions.add(transaction);
                payload.setPlacements(transactions);
            } else {
                LOGGER.log(Level.SEVERE,
                        "[CsdBloombergPdDserialize] Deserialization returned null transaction");
                throw new IOException("Failed to deserialize PLACEMENTS object");
            }

        } else if (repoNode.isArray()) {
            // Reject array format as per new client requirement
            LOGGER.log(Level.SEVERE,
                    "[CsdBloombergPdDserialize] PLACEMENTS is an array - expected single object only");
            throw new IOException("PLACEMENTS must be a single object, not an array");

        } else {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdDserialize] PLACEMENTS is not a valid object");
            throw new IOException("PLACEMENTS is not a valid JSON object");
        }

        return payload;
    }

    /**
    * Enumerates synthetic IDs from a directory by counting PLACEMENTS. Used by FILE adapter.
    *
    * @param pDir Directory to scan
    * @param pGlob File glob pattern (e.g., "*.json")
    * @param pObjMap ObjectMapper for deserialization
    * @return List of synthetic transaction IDs
    */
    public static List<String> extractIdsFromDirectory(Path pDir, String pGlob,
            ObjectMapper pObjMap) {
        List<String> ids = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pDir, pGlob)) {
            for (Path file : stream) {
                try {
                    BloombergPayload payload = readPayload(file, pObjMap);
                    int size = payload.getPlacements() == null ? 0 : payload.getPlacements().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    LOGGER.log(Level.INFO,
                            "[CsdBloombergPdDserialize] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[CsdBloombergPdDserialize] FILE: Skipping file due to read/parse error: {0}",
                            new Object[] { file });
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergPdDserialize] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====

/**
 * Root payload object representing the Bloomberg JSON structure. Contains a list of Placements.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {

    @JsonProperty("PLACEMENTS")
    private List<Placements> mPlacements = new ArrayList<>();

    /**
    * Gets the list of Placements.
    *
    * @return List of Placements objects
    */
    public List<Placements> getPlacements() {
        return mPlacements;
    }

    /**
    * Sets the list of Placements.
    *
    * @param pPlacements List of Placements objects
    */
    public void setPlacements(List<Placements> pPlacements) {
        this.mPlacements = pPlacements;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (mPlacements == null ? 0 : mPlacements.size()) + "}";
    }
}

/**
 * POJO representing a single Placements. Maps to the PLACEMENTS object in the Bloomberg JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Placements {

    @JsonProperty("CUSTOMER_NO")
    private String mCustomerNo;

    @JsonProperty("CURRENCY")
    private String mCurrency;

    @JsonProperty("PRINCIPAL")
    private String mPrincipal;

    @JsonProperty("EXCH_RATE")
    private String mExchRate;

    @JsonProperty("LCY_AMOUNT")
    private String mLcyAmount;

    @JsonProperty("DEAL_DATE")
    private String mDealDate;

    @JsonProperty("VALUE_DATE")
    private String mValueDate;

    @JsonProperty("MATURITY_DATE")
    private String mMaturityDate;

    @JsonProperty("CATEGORY")
    private String mCategory;

    @JsonProperty("INT_RATE")
    private String mIntRate;

    @JsonProperty("INTEREST_BASIS")
    private String mInterestBasis;

    @JsonProperty("TOT_INTEREST_AMT")
    private String mTotInterestAmt;

    @JsonProperty("INT_DUE_DATE")
    private String mIntDueDate;

    @JsonProperty("LIQ_DEFER_INTEREST")
    private String mLiqDeferInterest;

    @JsonProperty("REMARKS")
    private String mRemarks;

    @JsonProperty("DRAWDOWN_ACCOUNT")
    private String mDrawdownAccount;

    @JsonProperty("PR_LIQUID_ACCT")
    private String mPrLiquidAcct;

    @JsonProperty("INT_LIQUID_ACCT")
    private String mIntLiquidAcct;

    @JsonProperty("CHARGE_ACCOUNT")
    private String mChargeAccount;

    @JsonProperty("CHARGE_CODE")
    private String mChargeCode;

    @JsonProperty("FGN_FED_ACCT")
    private String mFgnFedAcct;

    @JsonProperty("LIAB_ACCOUNT")
    private String mLiabAccount;

    @JsonProperty("MATURE_AT_SOD")
    private String mMatureAtSod;

    @JsonProperty("PRIN_INCR_DECR")
    private String mPrinIncrDecr;

    @JsonProperty("INCR_DECR_EFF_DATE")
    private String mIncrDecEffDate;

    @JsonProperty("ROLLOVER_IND")
    private String mRolloverInd;

    @JsonProperty("ROLLOVER_INSTR")
    private String mRolloverInstr;

    @JsonProperty("NEW_INT_RATE")
    private String mNewInterestRate;

    @JsonProperty("CAPITALISATION")
    private String mCapitalization;

    @JsonProperty("PREV_PRIN_AMOUNT")
    private String mPrevPrinAmount;

    /**
    * Gets the the CustomerNo.
    *
    * @return Debit account number
    */
    public String getCustomerNo() {
        return mCustomerNo;
    }

    /**
    * Sets the CustomerNo.
    *
    * @param p_debitAcctNo Debit account number
    */
    public void setCustomerNo(String pCustomerNo) {
        this.mCustomerNo = pCustomerNo;
    }

    /**
    * Gets the currency.
    *
    * @return currency code
    */
    public String getCurrency() {
        return mCurrency;
    }

    /**
    * Sets the currency.
    *
    * @param p_debitCurrency Debit currency code
    */
    public void setCurrency(String pCurrency) {
        this.mCurrency = pCurrency;
    }

    /**
    * Gets the Principal.
    *
    * @return Principal as double
    */
    public String getPrincipal() {
        return mPrincipal;
    }

    /**
    * Sets the Principal.
    *
    * @param pPrincipal Debit amount as string
    */
    public void setPrincipal(String pPrincipal) {
        this.mPrincipal = pPrincipal;
    }

    /**
    * Gets the Exch Rate.
    *
    * @return Exch Rate as double
    */
    public String getExchRate() {
        return mExchRate;
    }

    /**
    * Sets the Exch Rate.
    *
    * @param pExchRate Exch Rate
    */
    public void getExchRate(String pExchRate) {
        this.mExchRate = pExchRate;
    }

    /**
    * Gets the LcyAmount value .
    *
    * @return LcyAmount
    */
    public String getLcyAmount() {
        return mLcyAmount;
    }

    /**
    * Sets the LcyAmount.
    *
    * @param pLcyAmount LcyAmount
    */
    public void setLcyAmount(String pLcyAmount) {
        this.mLcyAmount = pLcyAmount;
    }

    /**
    * Gets the DealDate.
    *
    * @return DealDate
    */
    public String getDealDate() {
        return mDealDate;
    }

    /**
    * Sets the DealDate.
    *
    * @param pDealDate value date
    */
    public void setDealDate(String pDealDate) {
        this.mDealDate = pDealDate;
    }

    /**
    * Gets the value date.
    *
    * @return Value date
    */
    public String getValueDate() {
        return mValueDate;
    }

    /**
    * Sets the value date.
    *
    * @param pValueDate value date
    */
    public void setValueDate(String pValueDate) {
        this.mValueDate = pValueDate;
    }

    /**
    * Gets the Maturity Date.
    *
    * @return Maturity Date
    */
    public String getMaturityDate() {
        return mMaturityDate;
    }

    /**
    * Sets the Maturity Date.
    *
    * @param pMaturityDate Maturity Date
    */
    public void setMaturityDate(String pMaturityDate) {
        this.mMaturityDate = pMaturityDate;
    }

    /**
    * Gets the Category.
    *
    * @return Category
    */
    public String getCategory() {
        return mCategory;
    }

    /**
    * Sets the Category.
    *
    * @param pCategory value date
    */
    public void setCategory(String pCategory) {
        this.mCategory = pCategory;
    }

    /**
    * Gets the IntRate.
    *
    * @return IntRate
    */
    public String getIntRate() {
        return mIntRate;
    }

    /**
    * Sets the IntRate.
    *
    * @param pIntRate value date
    */
    public void setIntRate(String pIntRate) {
        this.mIntRate = pIntRate;
    }

    /**
    * Gets the InterestBasis.
    *
    * @return InterestBasis
    */
    public String getInterestBasis() {
        return mInterestBasis;
    }

    /**
    * Sets the InterestBasis.
    *
    * @param pInterestBasis value date
    */
    public void setInterestBasis(String pInterestBasis) {
        this.mInterestBasis = pInterestBasis;
    }

    /**
    * Gets the TotInterestAmt.
    *
    * @return TotInterestAmt
    */
    public String getTotInterestAmt() {
        return mTotInterestAmt;
    }

    /**
    * Sets the TotInterestAmt.
    *
    * @param pTotInterestAmt value date
    */
    public void setTotInterestAmt(String pTotInterestAmt) {
        this.mTotInterestAmt = pTotInterestAmt;
    }

    /**
    * Gets the IntDueDate.
    *
    * @return IntDueDate
    */
    public String getIntDueDate() {
        return mIntDueDate;
    }

    /**
    * Sets the IntDueDate.
    *
    * @param pIntDueDate value date
    */
    public void setIntDueDate(String pIntDueDate) {
        this.mIntDueDate = pIntDueDate;
    }

    /**
    * Gets the LiqDeferInterest.
    *
    * @return LiqDeferInterest
    */
    public String getLiqDeferInterest() {
        return mLiqDeferInterest;
    }

    /**
    * Sets the LiqDeferInterest.
    *
    * @param pLiqDeferInterest value date
    */
    public void setLiqDeferInterest(String pLiqDeferInterest) {
        this.mLiqDeferInterest = pLiqDeferInterest;
    }

    /**
    * Gets the Remarks.
    *
    * @return Remarks
    */
    public String getRemarks() {
        return mRemarks;
    }

    /**
    * Sets the Remarks.
    *
    * @param pRemarks value date
    */
    public void setRemarks(String pRemarks) {
        this.mRemarks = pRemarks;
    }

    /**
    * Gets the DrawdownAccount.
    *
    * @return DrawdownAccount
    */
    public String getDrawdownAccount() {
        return mDrawdownAccount;
    }

    /**
    * Sets the DrawdownAccount.
    *
    * @param pDrawdownAccount value date
    */
    public void setDrawdownAccount(String pDrawdownAccount) {
        this.mDrawdownAccount = pDrawdownAccount;
    }

    /**
    * Gets the PrLiquidAcct.
    *
    * @return PrLiquidAcct
    */
    public String getPrLiquidAcct() {
        return mPrLiquidAcct;
    }

    /**
    * Sets the PrLiquidAcct.
    *
    * @param pPrLiquidAcct value date
    */
    public void setPrLiquidAcct(String pPrLiquidAcct) {
        this.mPrLiquidAcct = pPrLiquidAcct;
    }

    /**
    * Gets the IntLiquidAcct.
    *
    * @return IntLiquidAcct
    */
    public String getIntLiquidAcct() {
        return mIntLiquidAcct;
    }

    /**
    * Sets the IntLiquidAcct.
    *
    * @param pIntLiquidAcct value date
    */
    public void setIntLiquidAcct(String pIntLiquidAcct) {
        this.mIntLiquidAcct = pIntLiquidAcct;
    }

    /**
    * Gets the ChargeAccount.
    *
    * @return ChargeAccount
    */
    public String getChargeAccount() {
        return mChargeAccount;
    }

    /**
    * Sets the ChargeAccount.
    *
    * @param pChargeAccount value date
    */
    public void setChargeAccount(String pChargeAccount) {
        this.mChargeAccount = pChargeAccount;
    }

    /**
    * Gets the ChargeCode.
    *
    * @return ChargeCode
    */
    public String getChargeCode() {
        return mChargeCode;
    }

    /**
    * Sets the ChargeCode.
    *
    * @param pChargeCode value date
    */
    public void setChargeCode(String pChargeCode) {
        this.mChargeCode = pChargeCode;
    }

    /**
    * Gets the FgnFedAcct.
    *
    * @return FgnFedAcct
    */
    public String getFgnFedAcct() {
        return mFgnFedAcct;
    }

    /**
    * Sets the FgnFedAcct.
    *
    * @param pFgnFedAcct value date
    */
    public void setFgnFedAcct(String pFgnFedAcct) {
        this.mFgnFedAcct = pFgnFedAcct;
    }

    /**
    * Gets the LiabAccount.
    *
    * @return LiabAccount
    */
    public String getLiabAccount() {
        return mLiabAccount;
    }

    /**
    * Sets the LiabAccount.
    *
    * @param pLiabAccount value date
    */
    public void setLiabAccount(String pLiabAccount) {
        this.mLiabAccount = pLiabAccount;
    }

    /**
    * Gets the MatureAtSod.
    *
    * @return MatureAtSod
    */
    public String getMatureAtSod() {
        return mMatureAtSod;
    }

    /**
    * Sets the MatureAtSod.
    *
    * @param pMatureAtSod value date
    */
    public void setMatureAtSod(String pMatureAtSod) {
        this.mMatureAtSod = pMatureAtSod;
    }

    /**
    * Gets the PrinIncrDecr.
    *
    * @return PrinIncrDecr
    */
    public String getPrinIncrDecr() {
        return mPrinIncrDecr;
    }

    /**
    * Sets the PrinIncrDecr.
    *
    * @param pPrinIncrDecr value date
    */
    public void setPrinIncrDecr(String pPrinIncrDecr) {
        this.mPrinIncrDecr = pPrinIncrDecr;
    }

    /**
    * Gets the IncrDecEffDate.
    *
    * @return IncrDecEffDate
    */
    public String getIncrDecEffDate() {
        return mIncrDecEffDate;
    }

    /**
    * Sets the IncrDecEffDate.
    *
    * @param pIncrDecEffDate value date
    */
    public void setIncrDecEffDate(String pIncrDecEffDate) {
        this.mIncrDecEffDate = pIncrDecEffDate;
    }

    /**
    * Gets the RolloverInd.
    *
    * @return RolloverInd
    */
    public String getRolloverInd() {
        return mRolloverInd;
    }

    /**
    * Sets the RolloverInd.
    *
    * @param pRolloverInd value date
    */
    public void setRolloverInd(String pRolloverInd) {
        this.mRolloverInd = pRolloverInd;
    }

    /**
    * Gets the RolloverInstr.
    *
    * @return RolloverInstr
    */
    public String getRolloverInstr() {
        return mRolloverInstr;
    }

    /**
    * Sets the RolloverInstr.
    *
    * @param pRolloverInstr value date
    */
    public void setRolloverInstr(String pRolloverInstr) {
        this.mRolloverInstr = pRolloverInstr;
    }

    /**
    * Gets the NewInterestRate.
    *
    * @return NewInterestRate
    */
    public String getNewInterestRate() {
        return mNewInterestRate;
    }

    /**
    * Sets the NewInterestRate.
    *
    * @param pNewInterestRate value date
    */
    public void setNewInterestRate(String pNewInterestRate) {
        this.mNewInterestRate = pNewInterestRate;
    }

    /**
    * Gets the Capitalization.
    *
    * @return Capitalization
    */
    public String getCapitalization() {
        return mCapitalization;
    }

    /**
    * Sets the Capitalization.
    *
    * @param pCapitalization value date
    */
    public void setCapitalization(String pCapitalization) {
        this.mCapitalization = pCapitalization;
    }

    /**
    * Gets the PrevPrinAmount.
    *
    * @return PrevPrinAmount
    */
    public String getPrevPrinAmount() {
        return mPrevPrinAmount;
    }

    /**
    * Sets the PrevPrinAmount.
    *
    * @param pPrevPrinAmount value date
    */
    public void setPrevPrinAmount(String pPrevPrinAmount) {
        this.mPrevPrinAmount = pPrevPrinAmount;
    }

    @Override
    public String toString() {
        return "Placements{CustomerNo=" + mCustomerNo + ", Currency=" + mCurrency + ", Principal="
                + mPrincipal + ", ValueDate=" + mValueDate + "}";
    }
}
