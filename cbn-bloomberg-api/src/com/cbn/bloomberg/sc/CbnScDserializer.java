package com.cbn.bloomberg.sc;

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
 * Title: CbnScDserializer.java Author: CSD Development Team Date Created: 2025-10-11
 *
 * Purpose: Deserialization layer for Bloomberg SC feed (JSON → POJO). Contains no T24 APIs - only
 * Jackson-based deserialization helpers. Supports both FILE and WMQ (IBM MQ) adapters.
 *
 * Usage: This class provides static utility methods for: - Reading JSON files or strings into typed
 * POJOs - Extracting transaction IDs from directories - Defining POJO classes for Bloomberg payload
 * structure
 *
 * Modification Details: ---- 11/10/25 - Initial version Bloomberg SC deserialization with POJO
 * support ---- 09/11/25 - Updated to handle single SECURITY_MASTER object (not array) Compliant
 * with CSD Java Programming Standards r2022
 */
public final class CbnScDserializer {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CbnScDserializer.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CbnScDserializer() {
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
     * SECURITY_MASTER OBJECT (not array)
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
     * requirement: Now expects single SECURITY_MASTER object only.
     *
     * @param pJson JSON string
     * @param pObjMap ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    private static BloombergPayload parsePayload(String pJson, ObjectMapper pObjMap)
            throws IOException {
        LOGGER.log(Level.INFO, "[CbnScDserializer] Starting deserialization of JSON message");

        // Parse the JSON
        JsonNode rootNode = pObjMap.readTree(pJson);
        LOGGER.log(Level.FINE, "[CbnScDserializer] JSON parsed successfully");

        // Check if SECURITY_MASTER exists
        if (!rootNode.has("SECURITY_MASTER")) {
            LOGGER.log(Level.SEVERE,
                    "[CbnScDserializer] SECURITY_MASTER field not found in message");
            throw new IOException("SECURITY_MASTER field not found in JSON");
        }

        JsonNode repoNode = rootNode.get("SECURITY_MASTER");

        // Validate it's not null
        if (repoNode == null || repoNode.isNull()) {
            LOGGER.log(Level.SEVERE, "[CbnScDserializer] SECURITY_MASTER is null");
            throw new IOException("SECURITY_MASTER is null");
        }

        BloombergPayload payload = new BloombergPayload();

        // Handle single object format (NEW CLIENT REQUIREMENT)
        if (repoNode.isObject()) {
            LOGGER.log(Level.INFO,
                    "[CbnScDserializer] SECURITY_MASTER found as single object - proceeding with deserialization");

            SecurityMaster transaction = pObjMap.treeToValue(repoNode, SecurityMaster.class);

            if (transaction != null) {
                List<SecurityMaster> transactions = new ArrayList<>();
                transactions.add(transaction);
                payload.setSecurityMaster(transactions);
            } else {
                LOGGER.log(Level.SEVERE,
                        "[CbnScDserializer] Deserialization returned null transaction");
                throw new IOException("Failed to deserialize SECURITY_MASTER object");
            }

        } else if (repoNode.isArray()) {
            // Reject array format as per new client requirement
            LOGGER.log(Level.SEVERE,
                    "[CbnScDserializer] SECURITY_MASTER is an array - expected single object only");
            throw new IOException("SECURITY_MASTER must be a single object, not an array");

        } else {
            LOGGER.log(Level.SEVERE, "[CbnScDserializer] SECURITY_MASTER is not a valid object");
            throw new IOException("SECURITY_MASTER is not a valid JSON object");
        }

        return payload;
    }

    /**
     * Enumerates synthetic IDs from a directory by counting SECURITY_MASTER. Used by FILE adapter.
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
                    int size = payload.getSecurityMaster() == null ? 0
                            : payload.getSecurityMaster().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    LOGGER.log(Level.INFO, "[CbnScDserializer] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[CbnScDserializer] FILE: Skipping file due to read/parse error: {0}",
                            new Object[] { file });
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CbnScDserializer] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====

/**
 * Root payload object representing the Bloomberg JSON structure. Contains a list of
 * SECURITY_MASTER.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {

    @JsonProperty("SECURITY_MASTER")
    private List<SecurityMaster> mSecurityMaster = new ArrayList<>();

    /**
     * Gets the list of SECURITY_MASTER.
     *
     * @return List of SECURITY_MASTER objects
     */
    public List<SecurityMaster> getSecurityMaster() {
        return mSecurityMaster;
    }

    /**
     * Sets the list of SECURITY_MASTER.
     *
     * @param pSECURITY_MASTER List of SECURITY_MASTER objects
     */
    public void setSecurityMaster(List<SecurityMaster> pSecurityMaster) {
        this.mSecurityMaster = pSecurityMaster;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (mSecurityMaster == null ? 0 : mSecurityMaster.size())
                + "}";
    }
}

/**
 * POJO representing a single SECURITY_MASTER. Maps to the SECURITY_MASTER object in the Bloomberg
 * JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class SecurityMaster {

    @JsonProperty("COMPANY_NAME")
    private String mCompanyName;

    @JsonProperty("DESCRIPTION")
    private String mDescription;

    @JsonProperty("SHORT_NAME")
    private String mShortName;

    @JsonProperty("MNEMONIC")
    private String mMnemonic;

    @JsonProperty("COMPANY_DOMICILE")
    private String mCompanyDomicile;

    @JsonProperty("SECURITY_DOMICILE")
    private String mSecurityDomicile;

    @JsonProperty("SECURITY_CURRENCY")
    private String mSecurityCurrency;

    @JsonProperty("BOND_OR_SHARE")
    private String mBondOrShare;

    @JsonProperty("SUB_ASSET_TYPE")
    private String mSubAssetType;

    @JsonProperty("PRICE_CURRENCY")
    private String mPriceCurrency;

    @JsonProperty("PRICE_TYPE")
    private String mPriceType;

    @JsonProperty("LAST_PRICE")
    private String mLastPrice;

    @JsonProperty("PRICE_UPDATE_CODE")
    private String mPriceUpdateCode;

    @JsonProperty("INDUSTRY_CODE")
    private String mIndustryCode;

    @JsonProperty("STOCK_EXCHANGE")
    private String mStockExchange;

    @JsonProperty("COUPON_TAX_CODE")
    private String mCouponTaxCode;

    @JsonProperty("INTEREST_DAY_BASIS")
    private String mInterestBasis;

    @JsonProperty("INTEREST_RATE")
    private String mIntRate;

    @JsonProperty("ISSUE_DATE")
    private String mIssueDate;

    @JsonProperty("MATURITY_DATE")
    private String mMaturityDate;

    @JsonProperty("NO_OF_PAYMENT")
    private String mNoOfPayments;

    @JsonProperty("ACCRUAL_START_DATE")
    private String mAccrualStartDate;

    @JsonProperty("INT_PAYMENT_DATE")
    private String mIntPaymentDate;

    @JsonProperty("FIRST_CPN_DATE")
    private String mFirstCpnDate;

    @JsonProperty("ISIN")
    private String mIsint;

    @JsonProperty("SETUP_DATE")
    private String mSetupDate;

    /**
     * Gets the the CompanyName.
     *
     * @return CompanyName
     */
    public String getCompanyName() {
        return mCompanyName;
    }

    /**
     * Sets the CompanyName.
     *
     * @param pCompanyName
     */
    public void setCompanyName(String pCompanyName) {
        this.mCompanyName = pCompanyName;
    }

    /**
     * Gets the the Description.
     *
     * @return Description
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Sets the Description.
     *
     * @param pDescription
     */
    public void setDescription(String pDescription) {
        this.mDescription = pDescription;
    }

    /**
     * Gets the currency.
     *
     * @return currency code
     */
    public String getShortName() {
        return mShortName;
    }

    /**
     * Sets the currency.
     *
     * @param p_debitShortName Debit currency code
     */
    public void setShortName(String pShortName) {
        this.mShortName = pShortName;
    }

    /**
     * Gets the Mnemonic.
     *
     * @return Mnemonic as double
     */
    public String getMnemonic() {
        return mMnemonic;
    }

    /**
     * Sets the Mnemonic.
     *
     * @param pMnemonic Debit amount as string
     */
    public void setMnemonic(String pMnemonic) {
        this.mMnemonic = pMnemonic;
    }

    /**
     * Gets the CompanyDomicile.
     *
     * @return CompanyDomicile
     */
    public String getCompanyDomicile() {
        return mCompanyDomicile;
    }

    /**
     * Sets the CompanyDomicile.
     *
     * @param pCompanyDomicile
     */
    public void setCompanyDomicile(String pCompanyDomicile) {
        this.mCompanyDomicile = pCompanyDomicile;
    }

    /**
     * Gets the SecurityDomicile.
     *
     * @return SecurityDomicile
     */
    public String getSecurityDomicile() {
        return mSecurityDomicile;
    }

    /**
     * Sets the SecurityDomicile.
     *
     * @param pSecurityDomicile
     */
    public void setSecurityDomicile(String pSecurityDomicile) {
        this.mSecurityDomicile = pSecurityDomicile;
    }

    /**
     * Gets the SecurityCurrency value .
     *
     * @return SecurityCurrency
     */
    public String getSecurityCurrency() {
        return mSecurityCurrency;
    }

    /**
     * Sets the SecurityCurrency.
     *
     * @param pSecurityCurrency SecurityCurrency
     */
    public void setSecurityCurrency(String pSecurityCurrency) {
        this.mSecurityCurrency = pSecurityCurrency;
    }

    /**
     * Gets the BondOrShare.
     *
     * @return BondOrShare
     */
    public String getBondOrShare() {
        return mBondOrShare;
    }

    /**
     * Sets the BondOrShare.
     *
     * @param pBondOrShare value date
     */
    public void setBondOrShare(String pBondOrShare) {
        this.mBondOrShare = pBondOrShare;
    }

    /**
     * Gets the SubAssetType.
     *
     * @return SubAssetType
     */
    public String getSubAssetType() {
        return mSubAssetType;
    }

    /**
     * Sets the SubAssetType.
     *
     * @param pSubAssetType value date
     */
    public void setSubAssetType(String pSubAssetType) {
        this.mSubAssetType = pSubAssetType;
    }

    /**
     * Gets the value date.
     *
     * @return Value date
     */
    public String getPriceCurrency() {
        return mPriceCurrency;
    }

    /**
     * Sets the value date.
     *
     * @param pPriceCurrency value date
     */
    public void setPriceCurrency(String pPriceCurrency) {
        this.mPriceCurrency = pPriceCurrency;
    }

    /**
     * Gets the PriceType.
     *
     * @return PriceType.
     */
    public String getPriceType() {
        return mPriceType;
    }

    /**
     * Sets the PriceType.
     *
     * @param pPriceType
     */
    public void setPriceType(String pPriceType) {
        this.mPriceType = pPriceType;
    }

    /**
     * Gets the LastPrice.
     *
     * @return LastPrice
     */
    public String getLastPrice() {
        return mLastPrice;
    }

    /**
     * Sets the LastPrice.
     *
     * @param pLastPrice value date
     */
    public void setLastPrice(String pLastPrice) {
        this.mLastPrice = pLastPrice;
    }

    /**
     * Gets the PriceUpdateCode.
     *
     * @return PriceUpdateCode
     */
    public String getPriceUpdateCode() {
        return mPriceUpdateCode;
    }

    /**
     * Sets the PriceUpdateCode.
     *
     * @param pPriceUpdateCode
     */
    public void setPriceUpdateCode(String pPriceUpdateCode) {
        this.mPriceUpdateCode = pPriceUpdateCode;
    }

    /**
     * Gets the IndustryCode.
     *
     * @return IndustryCode
     */
    public String getIndustryCode() {
        return mIndustryCode;
    }

    /**
     * Sets the IndustryCode.
     *
     * @param pIndustryCode
     */
    public void setIndustryCode(String pIndustryCode) {
        this.mIndustryCode = pIndustryCode;
    }

    /**
     * Gets the StockExchange.
     *
     * @return StockExchange
     */
    public String getStockExchange() {
        return mStockExchange;
    }

    /**
     * Sets the StockExchange.
     *
     * @param pStockExchange
     */
    public void setStockExchange(String pStockExchange) {
        this.mStockExchange = pStockExchange;
    }

    /**
     * Gets the CouponTaxCode.
     *
     * @return CouponTaxCode
     */
    public String getCouponTaxCode() {
        return mCouponTaxCode;
    }

    /**
     * Sets the CouponTaxCode.
     *
     * @param pCouponTaxCode
     */

    public void setCouponTaxCode(String pCouponTaxCode) {
        this.mCouponTaxCode = pCouponTaxCode;
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
     * @param pInterestBasis
     */

    public void setInterestBasis(String pInterestBasis) {
        this.mInterestBasis = pInterestBasis;
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
     * @param pIntRate Rate.
     */
    public void setIntRate(String pIntRate) {
        this.mIntRate = pIntRate;
    }

    /**
     * Gets the IssueDate.
     *
     * @return IssueDate
     */
    public String getIssueDate() {
        return mIssueDate;
    }

    /**
     * Sets the IssueDate.
     *
     * @param pIssueDate value date
     */
    public void setIssueDate(String pIssueDate) {
        this.mIssueDate = pIssueDate;
    }

    /**
     * Gets the MaturityDate.
     *
     * @return MaturityDate
     */
    public String getMaturityDate() {
        return mMaturityDate;
    }

    /**
     * Sets the MaturityDate.
     *
     * @param pMaturityDate
     */
    public void setMaturityDate(String pMaturityDate) {
        this.mMaturityDate = pMaturityDate;
    }

    /**
     * Gets the NoOfPayments.
     *
     * @return NoOfPayments
     */
    public String getNoOfPayments() {
        return mNoOfPayments;
    }

    /**
     * Sets the NoOfPayments.
     *
     * @param pNoOfPayments value date
     */
    public void setNoOfPayments(String pNoOfPayments) {
        this.mNoOfPayments = pNoOfPayments;
    }

    /**
     * Gets the AccrualStartDate.
     *
     * @return AccrualStartDate
     */
    public String getAccrualStartDate() {
        return mAccrualStartDate;
    }

    /**
     * Sets the AccrualStartDate.
     *
     * @param pAccrualStartDate value date
     */
    public void setAccrualStartDate(String pAccrualStartDate) {
        this.mAccrualStartDate = pAccrualStartDate;
    }

    /**
     * Gets the IntPaymentDate.
     *
     * @return IntPaymentDate
     */
    public String getIntPaymentDate() {
        return mIntPaymentDate;
    }

    /**
     * Sets the IntPaymentDate.
     *
     * @param pIntPaymentDate value date
     */
    public void setIntPaymentDate(String pIntPaymentDate) {
        this.mIntPaymentDate = pIntPaymentDate;
    }

    /**
     * Gets the FirstCpnDate.
     *
     * @return FirstCpnDate
     */
    public String getFirstCpnDate() {
        return mFirstCpnDate;
    }

    /**
     * Sets the FirstCpnDate.
     *
     * @param pFirstCpnDate value date
     */
    public void setFirstCpnDate(String pFirstCpnDate) {
        this.mFirstCpnDate = pFirstCpnDate;
    }

    /**
     * Gets the Isint.
     *
     * @return Isint
     */
    public String getIsint() {
        return mIsint;
    }

    /**
     * Sets the Isint.
     *
     * @param pIsint value date
     */
    public void setIsint(String pIsint) {
        this.mIsint = pIsint;
    }

    /**
     * Gets the SetupDate.
     *
     * @return SetupDate
     */
    public String getSetupDate() {
        return mSetupDate;
    }

    /**
     * Sets the SetupDate.
     *
     * @param pSetupDate value date
     */
    public void setSetupDate(String pSetupDate) {
        this.mSetupDate = pSetupDate;
    }

    @Override
    public String toString() {
        return "SECURITY_MASTER{CustomerNo=" + mDescription + ", ShortName=" + mShortName
                + ", Mnemonic=" + mMnemonic + ", PriceCurrency=" + mPriceCurrency + "}";
    }
}
