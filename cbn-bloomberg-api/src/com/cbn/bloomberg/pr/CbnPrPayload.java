package com.cbn.bloomberg.pr;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Title: CsdBloombergRoPayload.java Author: CSD Development Team Date Created:
 * 2025-11-06
 *
 * Purpose: Handles JSON response payload construction for Bloomberg RO
 * transactions. Builds standardized response messages for successful and failed
 * transactions.
 *
 * Usage: Used by CsdBloombergFtService to construct response payloads before
 * publishing.
 */
public class CbnPrPayload {

    private static final Logger LOGGER = Logger.getLogger(CbnPrPayload.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final ObjectMapper objectMapper;
    public CbnPrPayload(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a JSON response payload for RO transaction processing.
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
            response.put("module", "REPO");
        
            // Include original item context if available
            if (originalItem != null) {
                response.put("counterparty", originalItem.path("COUNTERPARTY").asText(""));
                response.put("currency", originalItem.path("CURRENCY").asText(""));
                response.put("tradedate", originalItem.path("TRADE_DATE").asText(""));
                response.put("principalamt1", originalItem.path("PRINCIPAL_AMOUNT.1").asText(""));
            }

            String jsonResponse = objectMapper.writeValueAsString(response);
            LOGGER.log(Level.FINE, "[CsdBloombergRoPayload] Built response: {0}", jsonResponse);

            return jsonResponse;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoPayload] Error building response", e);
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