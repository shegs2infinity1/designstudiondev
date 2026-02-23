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
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Title: BloombergDserialFt.java Author: CSD Development Team Date Created:
 * 2025-10-11
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
 * deserialization with POJO support Compliant with CSD Java Programming
 * Standards r2022
 */
public final class CsdBloombergFtDserializer {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtDserializer.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CsdBloombergFtDserializer() {
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
        if (content == null || content.trim().isEmpty()) {
            return new BloombergPayload();
        }
        return p_om.readValue(content, BloombergPayload.class);
    }
    /**
     * Parses a Bloomberg JSON string into a typed payload. Used by WMQ adapter.
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
        return p_om.readValue(p_json, BloombergPayload.class);
    }

    /**
     * Enumerates synthetic IDs from a directory by counting FUNDS_MOVEMENTS. Used
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
                    int size = payload.getFundsMovements() == null ? 0 : payload.getFundsMovements().size();

                    for (int i = 0; i < size; i++) {
                        ids.add("FILE|" + file.toAbsolutePath() + "|FM|" + i);
                    }

                    LOGGER.log(Level.INFO, "[BloombergDserialFt] FILE: extracted {0} IDs from {1}",
                            new Object[] { size, file.getFileName() });

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "[BloombergDserialFt] FILE: Skipping file due to read/parse error: " + file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[BloombergDserialFt] FILE: Directory scan error", e);
        }

        return ids;
    }
}

// ==== TOP-LEVEL POJO CLASSES ====

/**
 * Root payload object representing the Bloomberg JSON structure. Contains a
 * list of funds movements.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BloombergPayload {
    @JsonProperty("FUNDS_MOVEMENTS")
    private List<FundsMovement> m_fundsMovements = new ArrayList<>();

    /**
     * Gets the list of funds movements.
     * 
     * @return List of FundsMovement objects
     */
    public List<FundsMovement> getFundsMovements() {
        return m_fundsMovements;
    }

    /**
     * Sets the list of funds movements.
     * 
     * @param p_fundsMovements List of FundsMovement objects
     */
    public void setFundsMovements(List<FundsMovement> p_fundsMovements) {
        this.m_fundsMovements = p_fundsMovements;
    }

    @Override
    public String toString() {
        return "BloombergPayload{size=" + (m_fundsMovements == null ? 0 : m_fundsMovements.size()) + "}";
    }
}

/**
 * POJO representing a single funds movement transaction. Maps to the
 * FUNDS_MOVEMENTS array elements in the Bloomberg JSON.
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