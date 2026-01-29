package com.cbn.bloomberg.ft;

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
 * Title: CsdBloombergFtDserializer.java Author: CSD Development Team Date
 * Created: 2025-10-11
 *
 * Purpose: Deserialization layer for Bloomberg FT feed (JSON → POJO). Contains
 * no T24 APIs - only Jackson-based deserialization helpers. Supports both FILE
 * and WMQ (IBM MQ) adapters.
 * 
 * Usage: This class provides static utility methods for: - Reading JSON files
 * or strings into typed POJOs - Extracting transaction IDs from directories -
 * Defining POJO classes for Bloomberg payload structure
 * 
 * Modification Details: ---- 11/10/25 - Initial version Bloomberg FT
 * deserialization with POJO support ---- 09/11/25 - Updated to handle single
 * FUNDS_MOVEMENT object (not array) Compliant with CSD Java Programming
 * Standards r2022
 */
public final class CbnFtDserializer {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CbnFtDserializer.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CbnFtDserializer() {
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
     * HANDLES SINGLE FUNDS_MOVEMENT OBJECT (not array)
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
     * formats. Client requirement: Now expects single FUNDS_MOVEMENT object only.
     * 
     * @param p_json JSON string
     * @param p_om   ObjectMapper for deserialization
     * @return BloombergPayload object
     * @throws IOException if parse fails
     */
    private static BloombergPayload parsePayload(String p_json, ObjectMapper p_om) throws IOException {
        LOGGER.log(Level.INFO, "[CsdBloombergFtDserializer] Starting deserialization of JSON message");

        // Parse the JSON
        JsonNode rootNode = p_om.readTree(p_json);
        LOGGER.log(Level.FINE, "[CsdBloombergFtDserializer] JSON parsed successfully");

        // Check if FUNDS_MOVEMENT exists
        if (!rootNode.has("FUNDS_MOVEMENT")) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtDserializer] FUNDS_MOVEMENT field not found in message");
            throw new IOException("FUNDS_MOVEMENT field not found in JSON");
        }

        JsonNode fundsNode = rootNode.get("FUNDS_MOVEMENT");

        // Validate it's not null
        if (fundsNode == null || fundsNode.isNull()) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtDserializer] FUNDS_MOVEMENT is null");
            throw new IOException("FUNDS_MOVEMENT is null");
        }

        BloombergPayload payload = new BloombergPayload();

        // Handle single object format (NEW CLIENT REQUIREMENT)
        if (fundsNode.isObject()) {
            LOGGER.log(Level.INFO,
                    "[CsdBloombergFtDserializer] FUNDS_MOVEMENT found as single object - proceeding with deserialization");

            FundsMovement transaction = p_om.treeToValue(fundsNode, FundsMovement.class);

            if (transaction != null) {
                List<FundsMovement> transactions = new ArrayList<>();
                transactions.add(transaction);
                payload.setFundsMovement(transactions);
            } else {
                LOGGER.log(Level.SEVERE, "[CsdBloombergFtDserializer] Deserialization returned null transaction");
                throw new IOException("Failed to deserialize FUNDS_MOVEMENT object");
            }

        } else if (fundsNode.isArray()) {
            // Reject array format as per new client requirement
            LOGGER.log(Level.SEVERE,
                    "[CsdBloombergFtDserializer] FUNDS_MOVEMENT is an array - expected single object only");
            throw new IOException("FUNDS_MOVEMENT must be a single object, not an array");

        } else {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtDserializer] FUNDS_MOVEMENT is not a valid object");
            throw new IOException("FUNDS_MOVEMENT is not a valid JSON object");
        }

        return payload;
    }

    /**
     * Enumerates synthetic IDs from a directory by counting FUNDS_MOVEMENT. Used by
     * FILE adapter.
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
                    int size = payload.getFundsMovement() == null ? 0 : payload.getFundsMovement().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    LOGGER.log(Level.INFO, "[CsdBloombergFtDserializer] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[CsdBloombergFtDserializer] FILE: Skipping file due to read/parse error: {0}",
                            new Object[] { file });
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtDserializer] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====

/**
 * Root payload object representing the Bloomberg JSON structure. Contains a
 * list of funds movement.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {
    @JsonProperty("FUNDS_MOVEMENT")
    private List<FundsMovement> m_FundsMovement = new ArrayList<>();

    /**
     * Gets the list of funds movement.
     * 
     * @return List of FundsMovement objects
     */
    public List<FundsMovement> getFundsMovement() {
        return m_FundsMovement;
    }

    /**
     * Sets the list of funds movement.
     * 
     * @param p_FundsMovement List of FundsMovement objects
     */
    public void setFundsMovement(List<FundsMovement> p_FundsMovement) {
        this.m_FundsMovement = p_FundsMovement;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (m_FundsMovement == null ? 0 : m_FundsMovement.size()) + "}";
    }
}

/**
 * POJO representing a single funds movement. Maps to the FUNDS_MOVEMENT object
 * in the Bloomberg JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class FundsMovement {
    @JsonProperty("DEBIT_ACCT_NO")
    private String m_debitAcctNo;

    @JsonProperty("DEBIT_CURRENCY")
    private String m_debitCurrency;

    @JsonProperty("DEBIT_AMOUNT")
    private String m_debitAmount;

    @JsonProperty("DEBIT_VALUE_DATE")
    private String m_debitValueDate;

    @JsonProperty("CREDIT_ACCT_NO")
    private String m_creditAcctNo;

    @JsonProperty("CREDIT_CURRENCY")
    private String m_creditCurrency;

    @JsonProperty("CREDIT_AMOUNT")
    private String m_creditAmount;

    @JsonProperty("CREDIT_VALUE_DATE")
    private String m_creditValueDate;

    @JsonProperty("PAYMENT_DETAILS")
    private String m_paymentDetails;

    @JsonProperty("ORDERING_CUST")
    private String m_orderingCust;

    /**
     * Gets the debit account number.
     * 
     * @return Debit account number
     */
    public String getDebitAcctNo() {
        return m_debitAcctNo;
    }

    /**
     * Sets the debit account number.
     * 
     * @param p_debitAcctNo Debit account number
     */
    public void setDebitAcctNo(String p_debitAcctNo) {
        this.m_debitAcctNo = p_debitAcctNo;
    }

    /**
     * Gets the debit currency.
     * 
     * @return Debit currency code
     */
    public String getDebitCurrency() {
        return m_debitCurrency;
    }

    /**
     * Sets the debit currency.
     * 
     * @param p_debitCurrency Debit currency code
     */
    public void setDebitCurrency(String p_debitCurrency) {
        this.m_debitCurrency = p_debitCurrency;
    }

    /**
     * Gets the debit amount.
     * 
     * @return Debit amount as string
     */
    public String getDebitAmount() {
        return m_debitAmount;
    }

    /**
     * Sets the debit amount.
     * 
     * @param p_debitAmount Debit amount as string
     */
    public void setDebitAmount(String p_debitAmount) {
        this.m_debitAmount = p_debitAmount;
    }

    /**
     * Gets the debit value date.
     * 
     * @return Debit value date
     */
    public String getDebitValueDate() {
        return m_debitValueDate;
    }

    /**
     * Sets the debit value date.
     * 
     * @param p_debitValueDate Debit value date
     */
    public void setDebitValueDate(String p_debitValueDate) {
        this.m_debitValueDate = p_debitValueDate;
    }

    /**
     * Gets the credit account number.
     * 
     * @return Credit account number
     */
    public String getCreditAcctNo() {
        return m_creditAcctNo;
    }

    /**
     * Sets the credit account number.
     * 
     * @param p_creditAcctNo Credit account number
     */
    public void setCreditAcctNo(String p_creditAcctNo) {
        this.m_creditAcctNo = p_creditAcctNo;
    }

    /**
     * Gets the credit currency.
     * 
     * @return Credit currency code
     */
    public String getCreditCurrency() {
        return m_creditCurrency;
    }

    /**
     * Sets the credit currency.
     * 
     * @param p_creditCurrency Credit currency code
     */
    public void setCreditCurrency(String p_creditCurrency) {
        this.m_creditCurrency = p_creditCurrency;
    }

    /**
     * Gets the credit amount.
     * 
     * @return Credit amount as string
     */
    public String getCreditAmount() {
        return m_creditAmount;
    }

    /**
     * Sets the credit amount.
     * 
     * @param p_creditAmount Credit amount as string
     */
    public void setCreditAmount(String p_creditAmount) {
        this.m_creditAmount = p_creditAmount;
    }

    /**
     * Gets the credit value date.
     * 
     * @return Credit value date
     */
    public String getCreditValueDate() {
        return m_creditValueDate;
    }

    /**
     * Sets the credit value date.
     * 
     * @param p_creditValueDate Credit value date
     */
    public void setCreditValueDate(String p_creditValueDate) {
        this.m_creditValueDate = p_creditValueDate;
    }

    /**
     * Gets the payment details.
     * 
     * @return Payment details text
     */
    public String getPaymentDetails() {
        return m_paymentDetails;
    }

    /**
     * Sets the payment details.
     * 
     * @param p_paymentDetails Payment details text
     */
    public void setPaymentDetails(String p_paymentDetails) {
        this.m_paymentDetails = p_paymentDetails;
    }

    /**
     * Gets the ordering customer.
     * 
     * @return Ordering customer information
     */
    public String getOrderingCust() {
        return m_orderingCust;
    }

    /**
     * Sets the ordering customer.
     * 
     * @param p_orderingCust Ordering customer information
     */
    public void setOrderingCust(String p_orderingCust) {
        this.m_orderingCust = p_orderingCust;
    }

    @Override
    public String toString() {
        return "FundsMovement{dAcc=" + m_debitAcctNo + ", cAcc=" + m_creditAcctNo + ", dAmt=" + m_debitAmount
                + ", cAmt=" + m_creditAmount + "}";
    }
}