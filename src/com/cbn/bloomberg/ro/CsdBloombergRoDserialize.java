package com.cbn.bloomberg.ro;

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
 * Title: CsdBloombergRoDserialize.java Author: CSD Development Team Date
 * Created: 2025-10-11
 *
 * Purpose: Deserialization layer for Bloomberg RO feed (JSON → POJO). Contains
 * no T24 APIs - only Jackson-based deserialization helpers. Supports both FILE
 * and WMQ (IBM MQ) adapters.
 * 
 * Usage: This class provides static utility methods for: - Reading JSON files
 * or strings into typed POJOs - Extracting transaction IDs from directories -
 * Defining POJO classes for Bloomberg payload structure
 * 
 * Modification Details: ---- 11/10/25 - Initial version Bloomberg RO
 * deserialization with POJO support ---- 09/11/25 - Updated to handle single
 * REPO object (not array) Compliant with CSD Java Programming Standards r2022
 */
public final class CsdBloombergRoDserialize {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CsdBloombergRoDserialize.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CsdBloombergRoDserialize() {
        // Utility class - no instances allowed
    }

    /**
     * Reads a Bloomberg JSON file into a typed payload. Used by FILE adapter.
     * 
     * @param pFile Path to the JSON file
     * @param pObjMap   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if file read or parse fails
     */
    public static BloombergPayload readPayload(Path pFile, ObjectMapper pObjMap) throws IOException {
        String content = new String(Files.readAllBytes(pFile), StandardCharsets.UTF_8);
        if (content.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(content, pObjMap);
    }

    /**
     * Parses a Bloomberg JSON string into a typed payload. Used by WMQ adapter. NOW
     * HANDLES SINGLE REPO OBJECT (not array)
     * 
     * @param pJson JSON string
     * @param pObjMap   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    public static BloombergPayload readPayload(String pJson, ObjectMapper pObjMap) throws IOException {
        if (pJson == null || pJson.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return parsePayload(pJson, pObjMap);
    }

    /**
     * Internal method to parse JSON and handle both single object and array
     * formats. Client requirement: Now expects single REPO object only.
     * 
     * @param pJson JSON string
     * @param pObjMap   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    private static BloombergPayload parsePayload(String pJson, ObjectMapper pObjMap) throws IOException {
        LOGGER.log(Level.INFO, "[CsdBloombergRoDserialize] Starting deserialization of JSON message");

        // Parse the JSON
        JsonNode rootNode = pObjMap.readTree(pJson);
        LOGGER.log(Level.FINE, "[CsdBloombergRoDserialize] JSON parsed successfully");

        // Check if REPO exists
        if (!rootNode.has("REPO")) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoDserialize] REPO field not found in message");
            throw new IOException("REPO field not found in JSON");
        }

        JsonNode repoNode = rootNode.get("REPO");

        // Validate it's not null
        if (repoNode == null || repoNode.isNull()) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoDserialize] REPO is null");
            throw new IOException("REPO is null");
        }

        BloombergPayload payload = new BloombergPayload();

        // Handle single object format (NEW CLIENT REQUIREMENT)
        if (repoNode.isObject()) {
            LOGGER.log(Level.INFO,
                    "[CsdBloombergRoDserialize] REPO found as single object - proceeding with deserialization");

            Repository transaction = pObjMap.treeToValue(repoNode, Repository.class);

            if (transaction != null) {
                List<Repository> transactions = new ArrayList<>();
                transactions.add(transaction);
                payload.setRepository(transactions);
            } else {
                LOGGER.log(Level.SEVERE, "[CsdBloombergRoDserialize] Deserialization returned null transaction");
                throw new IOException("Failed to deserialize REPO object");
            }

        } else if (repoNode.isArray()) {
            // Reject array format as per new client requirement
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoDserialize] REPO is an array - expected single object only");
            throw new IOException("REPO must be a single object, not an array");

        } else {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoDserialize] REPO is not a valid object");
            throw new IOException("REPO is not a valid JSON object");
        }

        return payload;
    }

    /**
     * Enumerates synthetic IDs from a directory by counting REPO. Used by FILE
     * adapter.
     * 
     * @param pDir  Directory to scan
     * @param pGlob File glob pattern (e.g., "*.json")
     * @param pObjMap   ObjectMapper for deserialization
     * @return List of synthetic transaction IDs
     */
    public static List<String> extractIdsFromDirectory(Path pDir, String pGlob, ObjectMapper pObjMap) {
        List<String> ids = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pDir, pGlob)) {
            for (Path file : stream) {
                try {
                    BloombergPayload payload = readPayload(file, pObjMap);
                    int size = payload.getRepository() == null ? 0 : payload.getRepository().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    LOGGER.log(Level.INFO, "[CsdBloombergRoDserialize] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[CsdBloombergRoDserialize] FILE: Skipping file due to read/parse error: {0}",
                            new Object[] { file });
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoDserialize] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====

/**
 * Root payload object representing the Bloomberg JSON structure. Contains a
 * list of Repository.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {
    @JsonProperty("REPO")
    private List<Repository> mRepository = new ArrayList<>();

    /**
     * Gets the list of Repository.
     * 
     * @return List of Repository objects
     */
    public List<Repository> getRepository() {
        return mRepository;
    }

    /**
     * Sets the list of Repository.
     * 
     * @param pRepository List of Repository objects
     */
    public void setRepository(List<Repository> pRepository) {
        this.mRepository = pRepository;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (mRepository == null ? 0 : mRepository.size()) + "}";
    }
}

/**
 * POJO representing a single Repository. Maps to the REPO object in the
 * Bloomberg JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Repository {
    @JsonProperty("COUNTERPARTY")
    private String mCounterParty;

    @JsonProperty("CURRENCY")
    private String mCurrency;

    @JsonProperty("TRADE_DATE")
    private String mTradeDate;

    @JsonProperty("VALUE_DATE")
    private String mValueDate;

    @JsonProperty("MATURITY_DATE")
    private String mMaturityDate;

    @JsonProperty("PRINCIPAL_AMOUNT.1")
    private String mPrincipalAmount1;

    @JsonProperty("PRINCIPAL_AMOUNT.2")
    private String mPrincipalAmount2;

    @JsonProperty("REPO_RATE")
    private String mRepositoryRate;

    @JsonProperty("PRODUCT")
    private String mProduct;

    @JsonProperty("DRAWDOWN_ACCOUNT")
    private String mDrawDownAccount;

    @JsonProperty("DRAWIN_ACCOUNT")
    private String mDrawInAccount;

    @JsonProperty("TOTAL_INTEREST_AMT")
    private String mTotInterestRate;

    /**
     * Gets the the CounterParty.
     * 
     * @return Debit account number
     */
    public String getCounterParty() {
        return mCounterParty;
    }

    /**
     * Sets the CounterParty.
     * 
     * @param p_debitAcctNo Debit account number
     */
    public void setCounterParty(String pCounterParty) {
        this.mCounterParty = pCounterParty;
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
     * Gets the Trade Date.
     * 
     * @return Trade Date as double
     */
    public String getTradeDate() {
        return mTradeDate;
    }

    /**
     * Sets the Trade Date.
     * 
     * @param pTradeDate Debit amount as string
     */
    public void setTradeDate(String pTradeDate) {
        this.mTradeDate = pTradeDate;
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
     * Gets the principal amount 1.
     * 
     * @return Principal Amount 1
     */
    public String getPrincipalAmount1() {
        return mPrincipalAmount1;
    }

    /**
     * Sets the principal amount 1.
     * 
     * @param pPrincipalAmount1 Principal Amount
     */
    public void setPrincipalAmount1(String pPrincipalAmount1) {
        this.mPrincipalAmount1 = pPrincipalAmount1;
    }

    /**
     * Gets the principal amount 2.
     * 
     * @return Principal Amount 2
     */
    public String getPrincipalAmount2() {
        return mPrincipalAmount2;
    }

    /**
     * Sets the principal amount 2.
     * 
     * @param pPrincipalAmount2 Principal Amount 2
     */
    public void setPrincipalAmount2(String pPrincipalAmount2) {
        this.mPrincipalAmount2 = pPrincipalAmount2;
    }

    /**
     * Gets the Repository Rate.
     * 
     * @return Repository Rate as double
     */
    public String getRepositoryRate() {
        return mRepositoryRate;
    }

    /**
     * Sets the Repository Rate.
     * 
     * @param pRepositoryRate Repository Rate
     */
    public void setRepositoryRate(String pRepositoryRate) {
        this.mRepositoryRate = pRepositoryRate;
    }

    /**
     * Gets the Product.
     * 
     * @return Product
     */
    public String getProduct() {
        return mProduct;
    }

    /**
     * Sets the Product.
     * 
     * @param pProduct Product
     */
    public void setProduct(String pProduct) {
        this.mProduct = pProduct;
    }

    /**
     * Gets the Drawdown Account
     * 
     * @return Drawdown Account
     */
    public String getDrawDownAccount() {
        return mDrawDownAccount;
    }

    /**
     * Sets the Drawdown Account.
     * 
     * @param pDrawdownAccount Drawdown Account text
     */
    public void setDrawDownAccount(String pDrawdownAccount) {
        this.mDrawDownAccount = pDrawdownAccount;
    }

    /**
     * Gets the Drawdown Account
     * 
     * @return Drawdown Account
     */
    public String getDrawInAccount() {
        return mDrawInAccount;
    }

    /**
     * Sets the DrawIn Account.
     * 
     * @param pDrawInAccount DrawIn Account
     */
    public void setDrawInAccount(String pDrawInAccount) {
        this.mDrawInAccount = pDrawInAccount;
    }

    /**
     * Gets the Total Interest Rate.
     * 
     * @return Total Interest Rate
     */
    public String getTotInterestRate() {
        return mTotInterestRate;
    }

    /**
     * Sets the Total Interest Rate.
     * 
     * @param pTotInterestRate Total Interest Rate
     */
    public void setTotInterestRate(String pTotInterestRate) {
        this.mTotInterestRate = pTotInterestRate;
    }

    @Override
    public String toString() {
        return "Repository{dAcc=" + mCounterParty + ", dCcy=" + mCurrency + ", rPrd=" + mProduct + ", vDte="
                + mValueDate + "}";
    }
}
