package com.cbn.bloomberg;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone test class to verify MQ connection and queue operations
 * 
 * @author shegs
 */
public class QueueConnectionTest {

    // Configuration constants
    private static final String HOST = "172.24.48.214";
    private static final int PORT = 1414;
    private static final String CHANNEL = "BLOOMBERG_FROM_T24";
    private static final String QMGR = "QM_BLOOMBERG";
    private static final String QUEUE_NAME = "BLOOMBERG_MQ";
    private static final String QUSER = "CBN";
    private static final String QPASS = "CBNPASS";

    public static void main(String[] args) {
        QueueConnectionTest test = new QueueConnectionTest();
        
        System.out.println("=== Bloomberg Queue Connection Test ===");
        
        // Test connection
        if (test.testConnection()) {
            System.out.println("✓ Connection test passed");
            
            // Test reading messages
            test.testReadMessages();
            
            // Test sending a sample message
            test.testSendMessage();
        } else {
            System.out.println("✗ Connection test failed");
        }
    }

    /**
     * Test basic connection to MQ
     */
    public boolean testConnection() {
        Connection connection = null;
        try {
            System.out.println("Testing connection to MQ...");
            System.out.println("Host: " + HOST + ":" + PORT);
            System.out.println("Queue Manager: " + QMGR);
            System.out.println("Channel: " + CHANNEL);
            
            MQConnectionFactory factory = createConnectionFactory();
            connection = factory.createConnection(QUSER, QPASS);
            connection.start();
            
            System.out.println("✓ Successfully connected to MQ");
            return true;
            
        } catch (JMSException e) {
            System.err.println("✗ Connection failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * Test reading messages from queue
     */
    public void testReadMessages() {
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        
        try {
            System.out.println("\nTesting message reading...");
            
            MQConnectionFactory factory = createConnectionFactory();
            connection = factory.createConnection(QUSER, QPASS);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            consumer = session.createConsumer(queue);
            
            connection.start();
            
            int messageCount = 0;
            while (true) {
                Message message = consumer.receive(2000); // 2 sec timeout
                if (message == null) break;
                
                messageCount++;
                System.out.println("Message " + messageCount + ": " + getMessageContent(message));
                
                // Limit to first 5 messages for testing
                if (messageCount >= 5) break;
            }
            
            if (messageCount == 0) {
                System.out.println("No messages found in queue");
            } else {
                System.out.println("✓ Successfully read " + messageCount + " messages");
            }
            
        } catch (JMSException e) {
            System.err.println("✗ Message reading failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(consumer, session, connection);
        }
    }

    /**
     * Test sending a sample message to verify queue write access
     */
    public void testSendMessage() {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        
        try {
            System.out.println("\nTesting message sending...");
            
            MQConnectionFactory factory = createConnectionFactory();
            connection = factory.createConnection(QUSER, QPASS);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(QUEUE_NAME);
            producer = session.createProducer(queue);
            
            // Create test message
            String testJson = createTestSecurityMasterJson();
            TextMessage message = session.createTextMessage(testJson);
            message.setStringProperty("MessageType", "SECURITY_MASTER");
            message.setStringProperty("Source", "TEST");
            
            producer.send(message);
            System.out.println("✓ Successfully sent test message");
            
        } catch (JMSException e) {
            System.err.println("✗ Message sending failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(producer, session, connection);
        }
    }

    /**
     * Create MQ connection factory with proper configuration
     */
    private MQConnectionFactory createConnectionFactory() throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(HOST);
        factory.setPort(PORT);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setQueueManager(QMGR);
        factory.setChannel(CHANNEL);
        
        // Additional connection properties for reliability
        factory.setIntProperty(WMQConstants.JMS_IBM_ENCODING, 273);
        factory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208);
        
        return factory;
    }

    /**
     * Extract message content safely
     */
    private String getMessageContent(Message message) {
        try {
            if (message instanceof TextMessage) {
                return ((TextMessage) message).getText();
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] data = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(data);
                return new String(data);
            } else {
                return message.toString();
            }
        } catch (JMSException e) {
            return "Error reading message: " + e.getMessage();
        }
    }

    /**
     * Create a test security master JSON message
     */
    private String createTestSecurityMasterJson() {
        return "{\n" +
               "  \"DESCRIPTION\": \"TEST SECURITY\",\n" +
               "  \"SHORT.NAME\": \"TEST\",\n" +
               "  \"MNEMONIC\": \"TST001\",\n" +
               "  \"SECURITY.DOMICILE\": \"NG\",\n" +
               "  \"BOND.OR.SHARE\": \"BOND\",\n" +
               "  \"PRICE.CURRENCY\": \"NGN\",\n" +
               "  \"PRICE.TYPE\": \"CLEAN\",\n" +
               "  \"LAST.PRICE\": \"100.00\",\n" +
               "  \"INTEREST.RATE\": \"5.50\",\n" +
               "  \"ISSUE.DATE\": \"20240101\",\n" +
               "  \"MATURITY.DATE\": \"20291231\",\n" +
               "  \"NO.OF.PAYMENT\": \"10\",\n" +
               "  \"ACCRUAL.START.DATE\": \"20240101\",\n" +
               "  \"INT.PAYMENT.DATE\": \"20240630\",\n" +
               "  \"FIRST.CPN.DATE\": \"20240630\",\n" +
               "  \"ISIN\": \"NGTEST123456\",\n" +
               "  \"SETUP.DATE\": \"20240101\"\n" +
               "}";
    }

    /**
     * Close connection safely
     */
    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Close all JMS resources safely
     */
    private void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("Error closing resource: " + e.getMessage());
                }
            }
        }
    }
}