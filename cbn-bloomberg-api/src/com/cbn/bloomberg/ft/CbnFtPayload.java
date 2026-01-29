package com.cbn.bloomberg.ft;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Title: CbnFtPayload.java Author: CSD Development Team Date Created:
 * 2025-11-06
 *
 * Purpose: Handles JSON response payload construction for Bloomberg FT
 * transactions. Builds standardized response messages for successful and failed
 * transactions.
 *
 * Usage: Used by CbnFtService to construct response payloads before
 * publishing.
 */
public class CbnFtPayload {

    private static final Logger LOGGER = Logger.getLogger(CbnFtPayload.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final ObjectMapper objectMapper;
    public CbnFtPayload(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a JSON response payload for FT transaction processing.
     *
     * @param status         Status of the transaction ("success" or "failure")
     * @param message        Descriptive message about the transaction result
     * @param transactionRef T24 transaction reference (or error code)
     * @param originalItem   Original JSON item from Bloomberg (optional, for
     *                       context)
     * @return JSON string representation of the response
     */
    public String buildResponse(String status, String message, String transactionRef, JsonNode originalItem) {
        try {
            ObjectNode response = objectMapper.createObjectNode();

            // Core response fields
            response.put("status", status);
            response.put("message", message);
            response.put("transactionRef", transactionRef);
            response.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FMT));
            response.put("module", "FUNDS.TRANSFER");

            // Include original item context if available
            if (originalItem != null) {
                response.put("debitaccount", originalItem.path("DEBIT_ACCT_NO").asText(""));
                response.put("creditAccount", originalItem.path("CREDIT_ACCT_NO").asText(""));
                response.put("debitAmount", originalItem.path("DEBIT_AMOUNT").asText(""));
                response.put("currency", originalItem.path("DEBIT_CURRENCY").asText(""));
            }

            String jsonResponse = objectMapper.writeValueAsString(response);
            LOGGER.log(Level.FINE, "[CbnFtPayload] Built response: {0}", jsonResponse);

            return jsonResponse;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CbnFtPayload] Error building response", e);
            return buildFallbackResponse(status, message, transactionRef);
        }
    }

    /**
     * Builds a minimal fallback response if JSON construction fails.
     */
    private String buildFallbackResponse(String status, String message, String transactionRef) {
        return String.format("{\"status\":\"%s\",\"message\":\"%s\",\"transactionRef\":\"%s\",\"timestamp\":\"%s\"}",
                status, message, transactionRef, LocalDateTime.now().format(TIMESTAMP_FMT));
    }
}