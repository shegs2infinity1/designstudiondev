package com.cbn.bloomberg;

import java.util.logging.Logger;
import java.util.logging.Level;

public class BloombergResponseSender {
    private static final Logger logger = Logger.getLogger(BloombergResponseSender.class.getName());

    private final String RESPONSE_QUEUE = "T24.TO.BLOOMBERG.RESPONSE";
    private final ImprovedJMSQueueSender queueSender;

    public BloombergResponseSender() {
        this.queueSender = new ImprovedJMSQueueSender();
    }

    public void sendResponse(BloombergResponse response) {
        try {
            String responseJson = response.toJson();
            boolean sent = queueSender.sendMessage(RESPONSE_QUEUE, responseJson);

            if (sent) {
                logger.info("Response sent to Bloomberg: " + response.getMessageId());
            } else {
                logger.warning("Failed to send response to Bloomberg: " + response.getMessageId());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending response to Bloomberg", e);
        }
    }
}