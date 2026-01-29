package com.cbn.bloomberg.ft;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Title: CsdBloombergFtPayload.java Author: CSD Development Team Date Created:
 * 2025-11-05
 * 
 * Purpose: Handles payload construction for Bloomberg FT (Funds Transfer)
 * integration. Builds JSON response payloads matching FUNDS_MOVEMENT_RESPONSE
 * format.
 * 
 * Usage: CsdBloombergFtPayload payload = new CsdBloombergFtPayload(); String
 * json = payload.buildSuccessResponse("FT123456", originalItem);
 * 
 * Modification Details: ---- 05/11/25 - Initial version Extracted payload
 * handling from CsdBloombergFtService Compliant with CSD Java Programming
 * Standards r2022
 */
public class CsdBloombergFtPayload {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtPayload.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final ObjectMapper objectMapper;

    /**
     * Default constructor with default ObjectMapper.
     */
    public CsdBloombergFtPayload() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor with custom ObjectMapper.
     * 
     * @param p_objectMapper Custom ObjectMapper instance
     */
    public CsdBloombergFtPayload(ObjectMapper p_objectMapper) {
        this.objectMapper = p_objectMapper != null ? p_objectMapper : new ObjectMapper();
    }

    /**
     * Builds a success response payload.
     * 
     * @param p_transactionRef Transaction reference from T24
     * @param p_originalItem   Original request item (optional)
     * @return JSON response string
     */
    public String buildSuccessResponse(String p_transactionRef, JsonNode p_originalItem) {
        return buildResponse("success", "Funds Transfer submitted successfully", p_transactionRef, p_originalItem);
    }

    /**
     * Builds a failure response payload.
     * 
     * @param p_errorMessage Error message
     * @param p_originalItem Original request item (optional)
     * @return JSON response string
     */
    public String buildFailureResponse(String p_errorMessage, JsonNode p_originalItem) {
        return buildResponse("failure", p_errorMessage, null, p_originalItem);
    }

    /**
     * Builds a generic response payload.
     * 
     * @param p_status         Status (success/failure)
     * @param p_message        Message text
     * @param p_transactionRef Transaction reference (optional)
     * @param p_originalItem   Original request item (optional)
     * @return JSON response string
     */
    public String buildResponse(String p_status, String p_message, String p_transactionRef, JsonNode p_originalItem) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode responseNode = objectMapper.createObjectNode();

            responseNode.put("status", p_status != null ? p_status : "failure");
            responseNode.put("message", p_message != null ? p_message : "Unknown error");
            responseNode.put("transactionRef",
                    p_transactionRef != null && !p_transactionRef.isEmpty() ? p_transactionRef : "N/A");
            responseNode.put("received_at", LocalDateTime.now().format(ISO_FORMATTER));

            // Add original request details if available
            if (p_originalItem != null) {
                ObjectNode requestDetails = objectMapper.createObjectNode();
                requestDetails.put("debit_account", p_originalItem.path("DEBIT_ACCT_NO").asText(""));
                requestDetails.put("credit_account", p_originalItem.path("CREDIT_ACCT_NO").asText(""));
                requestDetails.put("amount", p_originalItem.path("DEBIT_AMOUNT").asText(""));
                requestDetails.put("currency", p_originalItem.path("DEBIT_CURRENCY").asText(""));
                responseNode.set("request_details", requestDetails);
            }

            root.set("FUNDS_MOVEMENT_RESPONSE", responseNode);

            String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            LOGGER.log(Level.FINE, "[CsdBloombergFtPayload] Built response: {0}", jsonResponse);

            return jsonResponse;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtPayload] Error building response", e);
            return buildFallbackResponse(p_status, p_message);
        }
    }

    /**
     * Builds a minimal fallback response when JSON processing fails.
     * 
     * @param p_status  Status
     * @param p_message Message
     * @return Simple JSON response string
     */
    private String buildFallbackResponse(String p_status, String p_message) {
        return String.format(
                "{\"FUNDS_MOVEMENT_RESPONSE\":{\"status\":\"%s\",\"message\":\"%s\",\"transactionRef\":\"N/A\",\"received_at\":\"%s\"}}",
                p_status != null ? p_status : "failure",
                p_message != null ? p_message.replace("\"", "\\\"") : "Unknown error",
                LocalDateTime.now().format(ISO_FORMATTER));
    }

    /**
     * Validates if a JSON response is well-formed.
     * 
     * @param p_jsonResponse JSON response string
     * @return true if valid, false otherwise
     */
    public boolean isValidResponse(String p_jsonResponse) {
        if (p_jsonResponse == null || p_jsonResponse.trim().isEmpty()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(p_jsonResponse);
            return root.has("FUNDS_MOVEMENT_RESPONSE");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtPayload] Invalid response format", e);
            return false;
        }
    }
}