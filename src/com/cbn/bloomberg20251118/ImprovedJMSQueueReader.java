package com.cbn.bloomberg;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Improved JMS Queue Reader with better error handling and configuration
 * 
 * @author shegs
 */
public class ImprovedJMSQueueReader {
    
    private static final Logger logger = Logger.getLogger(ImprovedJMSQueueReader.class.getName());
    
    // Configuration - consider moving to properties file
    private final String HOST = "196.216.200.214";
    private final int PORT = 1415;
    private final String CHANNEL = "DEV.APP.SVRCONN";
    private final String QMGR = "QM_BLOOMBERG";
    private final String QUEUE_NAME = "BLOOMBERG.TO.T24.SFEED";
    private final String QUSER = "CBN";
    private final String QPASS = "CBNPASS";
    
    private MQConnectionFactory connectionFactory;
    
    public ImprovedJMSQueueReader() {
        initializeConnectionFactory();
    }
    
    /**
     * Initialize MQ connection factory with proper configuration
     */
    private void initializeConnectionFactory() {
        try {
            connectionFactory = new MQConnectionFactory();
            connectionFactory.setHostName(HOST);
            connectionFactory.setPort(PORT);
            connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            connectionFactory.setQueueManager(QMGR);
            connectionFactory.setChannel(CHANNEL);
            
            // Additional properties for better connection handling
            connectionFactory.setIntProperty(WMQConstants.JMS_IBM_ENCODING, 273);
            connectionFactory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208);
            connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            
        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Failed to initialize connection factory", e);
            throw new RuntimeException("Failed to initialize MQ connection factory", e);
        }
    }

    /**
     * Read messages from queue with improved error handling
     * 
     * @return List of message contents as strings
     */
    public List<String> readMessagesFromQueue() {
        return readMessagesFromQueue(30000); // 30 second timeout
    }
    
    /**
     * Read messages from queue with configurable timeout
     * 
     * @param timeoutMs Timeout in milliseconds
     * @return List of message contents as strings
     */
    public List<String> readMessagesFromQueue(long timeoutMs) {
        List<String> messages = new ArrayList<>();
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;

        try {
            logger.info("Connecting to MQ to read messages...");
            
            // Create connection and session
            connection = connectionFactory.createConnection(QUSER, QPASS);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            consumer = session.createConsumer(queue);

            // Start connection
            connection.start();
            logger.info("Connected to queue: " + QUEUE_NAME);

            // Read messages with timeout
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                Message message = consumer.receive(1000); // 1 sec receive timeout
                if (message == null) {
                    continue; // No message available, continue waiting
                }

                String messageContent = extractMessageContent(message);
                if (messageContent != null && !messageContent.trim().isEmpty()) {
                    messages.add(messageContent);
                    logger.info("Received message: " + messageContent.substring(0, Math.min(100, messageContent.length())) + "...");
                }
            }
            
            logger.info("Read " + messages.size() + " messages from queue");

        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Error reading messages from queue", e);
            throw new RuntimeException("Failed to read messages from queue", e);
        } finally {
            closeResources(consumer, session, connection);
        }

        return messages;
    }
    
    /**
     * Read a single message from queue (non-blocking)
     * 
     * @return Message content or null if no message available
     */
    public String readSingleMessage() {
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;

        try {
            connection = connectionFactory.createConnection(QUSER, QPASS);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            consumer = session.createConsumer(queue);

            connection.start();

            Message message = consumer.receive(5000); // 5 sec timeout
            if (message != null) {
                return extractMessageContent(message);
            }

        } catch (JMSException e) {
            logger.log(Level.WARNING, "Error reading single message", e);
        } finally {
            closeResources(consumer, session, connection);
        }

        return null;
    }

    /**
     * Extract content from JMS message
     * 
     * @param message JMS message
     * @return Message content as string
     */
    private String extractMessageContent(Message message) {
        try {
            if (message instanceof TextMessage) {
                return ((TextMessage) message).getText();
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] data = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(data);
                return new String(data, "UTF-8");
            } else {
                logger.warning("Unsupported message type: " + message.getClass().getName());
                return message.toString();
            }
        } catch (JMSException e) {
            logger.log(Level.WARNING, "Error extracting message content", e);
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected error extracting message content", e);
            return null;
        }
    }

    /**
     * Check if queue is available and accessible
     * 
     * @return true if queue is accessible
     */
    public boolean isQueueAccessible() {
        Connection connection = null;
        Session session = null;
        
        try {
            connection = connectionFactory.createConnection(QUSER, QPASS);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            
            // Try to create a consumer to test queue access
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.close();
            
            return true;
            
        } catch (JMSException e) {
            logger.log(Level.WARNING, "Queue not accessible", e);
            return false;
        } finally {
            closeResources(null, session, connection);
        }
    }

    /**
     * Close JMS resources safely
     */
    private void closeResources(MessageConsumer consumer, Session session, Connection connection) {
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException e) {
                logger.log(Level.WARNING, "Error closing consumer", e);
            }
        }
        
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                logger.log(Level.WARNING, "Error closing session", e);
            }
        }
        
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                logger.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }
}