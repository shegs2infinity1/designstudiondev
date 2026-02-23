package com.cbn.bloomberg.sc;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import com.ibm.mq.jms.MQQueue;
import com.ibm.msg.client.wmq.WMQConstants;


/**
 * Title: CsdBloombergScConsumer.java
 * 
 * Author: CSD Development Team Date Created: 2025-10-11
 * 
 * Purpose: MQ Consumer utility class for Bloomberg integration. Provides pure JMS-based message
 * operations without Spring dependencies.
 * 
 * Usage: This class provides static methods for: - Creating JMS text messages with proper encoding
 * - Consuming messages from MQ queues - Processing messages via callback interface
 * (MessageProcessor)
 * 
 * Modification Details: ---- 11/10/25 - Initial version Pure JMS implementation without Spring
 * dependencies
 */

public final class CsdBloombergScConsumer {

    // ==== STATIC CONSTANTS ====
    private static final Logger LOGGER = Logger.getLogger(CsdBloombergScConsumer.class.getName());

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CsdBloombergScConsumer() {
        // Utility class - no instances allowed
    }

    /**
     * Callback interface for processing consumed MQ messages.
     */
    public interface MessageProcessor {

        /**
         * Processes a single message.
         * 
         * @param pMessageId JMS message ID
         * @param pMessageBody Message body text
         * @throws Exception if processing fails
         */
        void process(String pMessageId, String pMessageBody) throws Exception;
    }

    /**
     * Utility method to create an MQ queue object with proper configuration.
     * 
     * @param pQueueName Name of the queue
     * @return Configured MQQueue object
     * @throws JMSException if queue creation fails
     */
    public static MQQueue createQueue(String pQueueName) throws JMSException {
        if (pQueueName == null || pQueueName.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }

        MQQueue queue = new MQQueue(pQueueName);
        queue.setTargetClient(WMQConstants.WMQ_CLIENT_NONJMS_MQ);

        LOGGER.log(Level.FINE, "[CsdBloombergScConsumer] Created MQ queue: {0}", pQueueName);
        return queue;
    }

    /**
     * Sends a text message to an MQ queue.
     * 
     * @param pConnection JMS connection
     * @param pQueueName Destination queue name
     * @param pMessageText Message text to send
     * @throws JMSException if send operation fails
     */
    public static void sendTextMessage(Connection pConnection, String pQueueName, String pMessageText)
            throws JMSException {
        if (pConnection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (pQueueName == null || pQueueName.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }
        if (pMessageText == null) {
            throw new IllegalArgumentException("Message text cannot be null");
        }

        Session session = null;
        MessageProducer producer = null;

        try {
            session = pConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("queue:///" + pQueueName);
            producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage(pMessageText);

            // Set IBM MQ specific properties for UTF-8 encoding
            try {
                message.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208); // UTF-8
                message.setIntProperty(WMQConstants.JMS_IBM_ENCODING, WMQConstants.WMQ_ENCODING_NATIVE);
            } catch (JMSException e) {
                LOGGER.log(Level.WARNING, "[CsdBloombergScConsumer] Failed to set encoding properties", e);
            }

            producer.send(message);
            LOGGER.log(Level.INFO, "[CsdBloombergScConsumer] Sent message to queue: {0}", pQueueName);

        } finally {
            closeQuietly(producer);
            closeQuietly(session);
        }
    }

    /**
     * Consumes messages from an MQ queue and processes them via callback.
     * 
     * @param pConnection JMS connection
     * @param pQueueName Queue name to consume from
     * @param pProcessor Callback for processing messages
     * @param pMaxMessages Maximum number of messages to consume (0 = unlimited)
     * @param pReceiveTimeout Timeout in milliseconds for each receive operation
     * @return Number of messages consumed
     * @throws JMSException if consumption fails
     */
    public static int consumeMessages(Connection pConnection, String pQueueName, MessageProcessor pProcessor,
            int pMaxMessages, long pReceiveTimeout) throws JMSException {
        if (pConnection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (pQueueName == null || pQueueName.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }
        if (pProcessor == null) {
            throw new IllegalArgumentException("MessageProcessor cannot be null");
        }

        LOGGER.log(Level.INFO, "[CsdBloombergScConsumer] Starting consumption from queue: {0}", pQueueName);

        Session session = null;
        MessageConsumer consumer = null;
        int consumedCount = 0;

        try {
            session = pConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Queue queue = session.createQueue("queue:///" + pQueueName);
            consumer = session.createConsumer(queue);

            boolean continueConsuming = true;
            while (continueConsuming) {
                // Check if max messages reached
                if (pMaxMessages > 0 && consumedCount >= pMaxMessages) {
                    LOGGER.log(Level.INFO, "[CsdBloombergScConsumer] Max messages reached: {0}",
                            pMaxMessages);
                    break;
                }

                // Receive message with timeout
                Message message = consumer.receive(pReceiveTimeout);

                if (message == null) {
                    LOGGER.log(Level.INFO, "[CsdBloombergScConsumer] No more messages available (timeout)");
                    break;
                }

                // Process message
                try {
                    String messageId = message.getJMSMessageID();
                    String messageBody = extractMessageBody(message);

                    LOGGER.log(Level.FINE, "[CsdBloombergScConsumer] Processing message: {0}", messageId);
                    pProcessor.process(messageId, messageBody);

                    // Acknowledge message after successful processing
                    message.acknowledge();

                    consumedCount++;
                    LOGGER.log(Level.FINE, "[CsdBloombergScConsumer] Successfully processed message {0}/{1}",
                            new Object[] { consumedCount, pMaxMessages > 0 ? pMaxMessages : "unlimited" });

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "[CsdBloombergScConsumer] Error processing message", e);
                    // Continue consuming despite processing error
                }
            }

            LOGGER.log(Level.INFO, "[CsdBloombergScConsumer] Consumption complete. Total messages: {0}",
                    consumedCount);

        } finally {
            closeQuietly(consumer);
            closeQuietly(session);
        }

        return consumedCount;
    }

    /**
     * Extracts the message body from a JMS message.
     * 
     * @param pMessage JMS message
     * @return Message body as string
     * @throws JMSException if extraction fails
     */
    private static String extractMessageBody(Message pMessage) throws JMSException {
        if (pMessage instanceof TextMessage) {
            return ((TextMessage) pMessage).getText();
        } else {
            LOGGER.log(Level.WARNING, "[CsdBloombergScConsumer] Non-text message received: {0}",
                    pMessage.getClass().getName());
            return "";
        }
    }

    /**
     * Closes a MessageProducer quietly (suppressing exceptions).
     * 
     * @param pProducer MessageProducer to close
     */
    private static void closeQuietly(MessageProducer pProducer) {
        if (pProducer != null) {
            try {
                pProducer.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergScConsumer] Error closing producer (ignored)", e);
            }
        }
    }

    /**
     * Closes a MessageConsumer quietly (suppressing exceptions).
     * 
     * @param pConsumer MessageConsumer to close
     */
    private static void closeQuietly(MessageConsumer pConsumer) {
        if (pConsumer != null) {
            try {
                pConsumer.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergScConsumer] Error closing consumer (ignored)", e);
            }
        }
    }

    /**
     * Closes a Session quietly (suppressing exceptions).
     * 
     * @param pSession Session to close
     */
    private static void closeQuietly(Session pSession) {
        if (pSession != null) {
            try {
                pSession.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergScConsumer] Error closing session (ignored)", e);
            }
        }
    }
}