package com.cbn.bloomberg;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;

/**
 * Improved Bloomberg Security Master Service with better error handling and T24 integration
 * 
 * @author shegs
 */
public class ImprovedSecurityMasterService extends ServiceLifecycle {
    
    private static final Logger logger = Logger.getLogger(ImprovedSecurityMasterService.class.getName());
    
    private final ImprovedJMSQueueReader jmsReader;
    private final SecurityMasterDeserialize deserializer;
    private final SecurityMasterT24Mapper t24Mapper;
    private final BloombergResponseSender responseSender;

    public ImprovedSecurityMasterService() {
        this.jmsReader = new ImprovedJMSQueueReader();
        this.deserializer = new SecurityMasterDeserialize();
        this.t24Mapper = new SecurityMasterT24Mapper();
        this.responseSender = new BloombergResponseSender();
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> messageIds = new ArrayList<>();
        
        try {
            logger.info("Fetching messages from Bloomberg queue...");
            
            // Check if queue is accessible first
            if (!jmsReader.isQueueAccessible()) {
                logger.warning("Bloomberg queue is not accessible");
                return messageIds;
            }
            
            // Read messages from queue
            List<String> messages = jmsReader.readMessagesFromQueue();
            
            if (messages.isEmpty()) {
                logger.info("No messages found in Bloomberg queue");
                return messageIds;
            }
            
            // Process each message and extract IDs
            for (int i = 0; i < messages.size(); i++) {
                String message = messages.get(i);
                try {
                    // Validate message format
                    if (isValidSecurityMasterMessage(message)) {
                        // Use message index as ID for T24 processing
                        String messageId = "BLB_SM_" + System.currentTimeMillis() + "_" + i;
                        messageIds.add(messageId);
                        
                        // Store message for later processing (cache)
                        MessageCache.getInstance().storeMessage(messageId, message);
                        
                        logger.info("Queued security master message with ID: " + messageId);
                    } else {
                        logger.warning("Invalid security master message format: " + 
                                     message.substring(0, Math.min(100, message.length())));
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing message " + i, e);
                }
            }
            
            logger.info("Successfully queued " + messageIds.size() + " security master messages for processing");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching messages from Bloomberg queue", e);
            // Don't throw exception - return empty list to allow T24 to continue
        }
        
        return messageIds;
    }
    
    @Override
    public void updateRecord(String messageId, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        try {
            logger.info("Processing security master message: " + messageId);
            
            // Retrieve message from cache
            String messageJson = MessageCache.getInstance().getMessage(messageId);
            if (messageJson == null) {
                logger.warning("Message not found in cache: " + messageId);
                return;
            }
            
            // Deserialize Bloomberg message
            SecurityMasterCBN securityMaster = deserializer.getSecurityMaster(messageJson);
            if (securityMaster == null) {
                logger.warning("Failed to deserialize security master message: " + messageId);
                sendErrorResponse(messageId, "DESERIALIZATION_ERROR", "Failed to parse message");
                return;
            }
            
            // Validate required fields
            ValidationResult validation = validateSecurityMaster(securityMaster);
            if (!validation.isValid()) {
                logger.warning("Security master validation failed: " + validation.getErrorMessage());
                sendErrorResponse(messageId, "VALIDATION_ERROR", validation.getErrorMessage());
                return;
            }
            
            // Map to T24 structure and update records
            boolean success = t24Mapper.mapToT24Records(securityMaster, records, transactionData);
            
            if (success) {
                logger.info("Successfully processed security master: " + securityMaster.mnemonic);
                sendSuccessResponse(messageId, securityMaster.mnemonic);
            } else {
                logger.warning("Failed to map security master to T24: " + messageId);
                sendErrorResponse(messageId, "MAPPING_ERROR", "Failed to map to T24 structure");
            }
            
            // Clean up cache
            MessageCache.getInstance().removeMessage(messageId);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing security master message: " + messageId, e);
            sendErrorResponse(messageId, "PROCESSING_ERROR", e.getMessage());
        }
        
        // Call parent implementation
        super.updateRecord(messageId, serviceData, controlItem, transactionControl, transactionData, records);
    }
    
    /**
     * Validate if message is a valid security master message
     */
    private boolean isValidSecurityMasterMessage(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return false;
            }
            
            // Basic JSON validation
            if (!message.trim().startsWith("{") || !message.trim().endsWith("}")) {
                return false;
            }
            
            // Try to deserialize to check structure
            SecurityMasterCBN sm = deserializer.getSecurityMaster(message);
            return sm != null && (sm.mnemonic != null || sm.isin != null);
            
        } catch (Exception e) {
            logger.log(Level.FINE, "Message validation failed", e);
            return false;
        }
    }
    
    /**
     * Validate security master data
     */
    private ValidationResult validateSecurityMaster(SecurityMasterCBN securityMaster) {
        List<String> errors = new ArrayList<>();
        
        // Required field validations
        if (securityMaster.mnemonic == null || securityMaster.mnemonic.trim().isEmpty()) {
            errors.add("Mnemonic is required");
        }
        
        if (securityMaster.description == null || securityMaster.description.trim().isEmpty()) {
            errors.add("Description is required");
        }
        
        if (securityMaster.priceCurrency == null || securityMaster.priceCurrency.trim().isEmpty()) {
            errors.add("Price currency is required");
        }
        
        // Business rule validations
        if (securityMaster.bondOrShare != null) {
            if (!"BOND".equals(securityMaster.bondOrShare) && !"SHARE".equals(securityMaster.bondOrShare)) {
                errors.add("Bond or Share must be either 'BOND' or 'SHARE'");
            }
        }
        
        // Date format validations (assuming YYYYMMDD format)
        if (securityMaster.issueDate != null && !isValidDate(securityMaster.issueDate)) {
            errors.add("Invalid issue date format");
        }
        
        if (securityMaster.maturityDate != null && !isValidDate(securityMaster.maturityDate)) {
            errors.add("Invalid maturity date format");
        }
        
        return new ValidationResult(errors.isEmpty(), String.join("; ", errors));
    }
    
    /**
     * Validate date format (YYYYMMDD)
     */
    private boolean isValidDate(String date) {
        if (date == null || date.length() != 8) {
            return false;
        }
        
        try {
            Integer.parseInt(date);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Send success response to Bloomberg
     */
    private void sendSuccessResponse(String messageId, String mnemonic) {
        try {
            BloombergResponse response = new BloombergResponse();
            response.setMessageId(messageId);
            response.setStatus("SUCCESS");
            response.setMnemonic(mnemonic);
            response.setMessage("Security master processed successfully");
            
            responseSender.sendResponse(response);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send success response for: " + messageId, e);
        }
    }
    
    /**
     * Send error response to Bloomberg
     */
    private void sendErrorResponse(String messageId, String errorCode, String errorMessage) {
        try {
            BloombergResponse response = new BloombergResponse();
            response.setMessageId(messageId);
            response.setStatus("ERROR");
            response.setErrorCode(errorCode);
            response.setMessage(errorMessage);
            
            responseSender.sendResponse(response);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send error response for: " + messageId, e);
        }
    }
    
    /**
     * Validation result helper class
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}