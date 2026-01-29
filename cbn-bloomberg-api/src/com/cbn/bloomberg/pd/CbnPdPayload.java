package com.cbn.bloomberg.pd;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Title: CbnPdPayload.java Author: CSD Development Team Date Created: 2025-11-06
 *
 * Purpose: Handles JSON response payload construction for Bloomberg PD transactions. Builds
 * standardized response messages for successful and failed transactions.
 *
 * Usage: Used by CbnPdService to construct response payloads before publishing.
 */
public class CbnPdPayload {

    private static final Logger LOGGER = Logger.getLogger(CbnPdPayload.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'HH:mm:ss");
    private final ObjectMapper objectMapper;

    public CbnPdPayload(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a JSON response payload for PD transaction processing.
     *
     * @param status Status of the transaction ("success" or "failure")
     * @param message Descriptive message about the transaction result
     * @param transactionRef T24 transaction reference (or error code)
     * @param originalItem Original JSON item from Bloomberg (optional, for context)
     * @return JSON string representation of the response
     */
    public String buildResponse(String status, String message, String transactionRef,
            JsonNode originalItem) {
        try {
            ObjectNode response = objectMapper.createObjectNode();

            // Core response fields
            response.put("status", status);
            response.put("message", message);
            response.put("placementId", transactionRef);
            response.put("receivedAt", LocalDateTime.now().format(TIMESTAMP_FMT));
            response.put("module", "PLACEMENTS");

            // Include original item context if available
            if (originalItem != null) {
                response.put("customerno", originalItem.path("CUSTOMER_NO").asText(""));
                response.put("currency", originalItem.path("CURRENCY").asText(""));
                response.put("principal", originalItem.path("PRINCIPAL").asText(""));
                response.put("dealdate", originalItem.path("DEAL_DATE").asText(""));
            }

            String jsonResponse = objectMapper.writeValueAsString(response);
            LOGGER.log(Level.FINE, "[CbnPdPayload] Built response: {0}", jsonResponse);

            return jsonResponse;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CbnPdPayload] Error building response", e);
            return buildFallbackResponse(status, message, transactionRef);
        }
    }

    /**
     * Builds a minimal fallback response if JSON construction fails.
     */
    private String buildFallbackResponse(String status, String message, String transactionRef) {
        return String.format(
                "{\"status\":\"%s\",\"message\":\"%s\",\"placementId\":\"%s\",\"receivedAt\":\"%s\"}",
                status, message, transactionRef, LocalDateTime.now().format(TIMESTAMP_FMT));
    }
}