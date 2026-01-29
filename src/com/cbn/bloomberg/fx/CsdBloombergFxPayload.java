package com.cbn.bloomberg.fx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * @title: CsdBloombergFxPayload.java
 * @author: CSD Development Team Date
 * @created: 2025-11-06
 * 
 * @purpose:
 *           - Handles JSON response payload construction for Bloomberg FX transactions.
 *           - Builds standardized response messages for successful and failed transactions.
 *
 * @usage:
 *         Used by CsdBloombergFxService to construct response payloads before
 *         publishing.
 */
public class CsdBloombergFxPayload {

    private static final Logger yLogger = Logger.getLogger(CsdBloombergFxPayload.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'HH:mm:ss");

    private final ObjectMapper objectMapper;

    public CsdBloombergFxPayload(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a JSON response payload for FX transaction processing.
     *
     * @param status Status of the transaction ("success" or "failure")
     * @param message Descriptive message about the transaction result
     * @param transactionRef T24 transaction reference (or error code)
     * @param originalItem Original JSON item from Bloomberg (optional, for
     *        context)
     * @return JSON string representation of the response
     */
    public String buildResponse(String status, String message, String transactionRef,
            JsonNode originalItem) {
        try {
            ObjectNode response = objectMapper.createObjectNode();

            // Core response fields
            response.put("status", status);
            response.put("transactionRef", transactionRef);
            response.put("message", message);
            response.put("received_at", LocalDateTime.now().format(TIMESTAMP_FMT));
            response.put("module", "FOREX");

            String jsonResponse = objectMapper.writeValueAsString(response);
            yLogger.log(Level.FINE, "[CsdBloombergFxPayload] Built response: {0}", jsonResponse);
            return jsonResponse;

        } catch (Exception e) {
            yLogger.log(Level.SEVERE, "[CsdBloombergFxPayload] Error building response", e);
            return buildFallbackResponse(status, message, transactionRef);
        }
    }

    /**
     * Builds a minimal fallback response if JSON construction fails.
     */
    private String buildFallbackResponse(String status, String message, String transactionRef) {
        return String.format(
                "{\"status\":\"%s\",\"message\":\"%s\",\"transactionRef\":\"%s\",\"timestamp\":\"%s\"}",
                status, message, transactionRef, LocalDateTime.now().format(TIMESTAMP_FMT));
    }
}