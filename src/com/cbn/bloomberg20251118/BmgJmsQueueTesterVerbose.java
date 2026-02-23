package com.cbn.bloomberg;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import javax.jms.Queue;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Queue testing utility for IBM MQ Non-destructive inspection of queued
 * messages. Reads connection details from application.properties
 * 
 * ONLY for unit/integration testing.
 */
public class BmgJmsQueueTesterVerbose {

    private String host;
    private int port;
    private String queueManager;
    private String queueName;
    private String channel;
    private String user;
    private String password;

    /**
     * Constructor that loads MQ settings from application.properties
     */
    public BmgJmsQueueTesterVerbose() {
        loadPropertiesFromFile();
    }

    /**
     * Manual constructor (for override if needed)
     */
    public BmgJmsQueueTesterVerbose(String host, int port, String qm, String queueName, String channel, String user,
            String password) {
        this.host = host;
        this.port = port;
        this.queueManager = qm;
        this.queueName = queueName;
        this.channel = channel;
        this.user = user;
        this.password = password;
    }

    /**
     * Load MQ connection properties from application.properties file
     */
    private void loadPropertiesFromFile() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            props.load(fis);

            this.host = props.getProperty("mq.host", "localhost");
            this.port = Integer.parseInt(props.getProperty("mq.port", "1414"));
            this.queueManager = props.getProperty("mq.qmgr", "QM1");
            this.queueName = props.getProperty("mq.queue", "TEST.QUEUE");
            this.channel = props.getProperty("mq.channel", "DEV.APP.SVRCONN");
            this.user = props.getProperty("mq.user", "");
            this.password = props.getProperty("mq.password", "");

            System.out.println("[QUEUE TEST] Loaded properties: " + host + ":" + port + " | QM=" + queueManager
                    + " | Queue=" + queueName + " | Channel=" + channel);

        } catch (IOException e) {
            System.err.println("[QUEUE TEST] Could not load application.properties: " + e.getMessage());
            System.err.println("[QUEUE TEST] Using default values...");
            // Fallback to your current values
            this.host = "172.105.249.157";
            this.port = 1414;
            this.queueManager = "QM_BLOOMBERG";
            this.queueName = "TEST.QUEUE";
            this.channel = "DEV.APP.SVRCONN";
            this.user = "";
            this.password = "";
        }
    }

    /**
     * Message info DTO
     */
    public static class MessageInfo {
        public final int index;
        public final String timestamp;
        public final String correlationId;
        public final String messageId;

        public MessageInfo(int index, String timestamp, String correlationId, String messageId) {
            this.index = index;
            this.timestamp = timestamp;
            this.correlationId = correlationId;
            this.messageId = messageId;
        }

        @Override
        public String toString() {
            return "Msg#" + index + " [Timestamp=" + timestamp + ", CorrelationID=" + correlationId + ", MessageID="
                    + messageId + "]";
        }
    }

    /**
     * Inspect the queue without consuming messages.
     * 
     * @return list of MessageInfo with verbose details
     */
    public List<MessageInfo> inspectQueue() {
        JMSContext context = null;
        QueueBrowser browser = null;
        List<MessageInfo> results = new ArrayList<>();

        try {
            System.out.println("[QUEUE TEST] Connecting to MQ: " + host + ":" + port + " | QM=" + queueManager
                    + " | Channel=" + channel);

            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
            factory.setIntProperty(WMQConstants.WMQ_PORT, port);
            factory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
            factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManager);

            // Handle no-auth mode (empty user/password)
            if (user == null || user.trim().isEmpty()) {
                context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
                System.out.println("[QUEUE TEST] Connected in no-auth mode (using MCAUSER)");
            } else {
                context = factory.createContext(user, password, JMSContext.AUTO_ACKNOWLEDGE);
                System.out.println("[QUEUE TEST] Connected with user: " + user);
            }

            Queue queue = context.createQueue("queue:///" + queueName);
            browser = context.createBrowser(queue);
            Enumeration<?> msgs = browser.getEnumeration();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getDefault());

            int index = 0;
            while (msgs.hasMoreElements()) {
                Message message = (Message) msgs.nextElement();
                index++;

                String ts = sdf.format(new Date(message.getJMSTimestamp()));
                String corrId = message.getJMSCorrelationID();
                String msgId = message.getJMSMessageID();

                results.add(new MessageInfo(index, ts, corrId, msgId));
            }

            System.out.println("[QUEUE TEST] ✅ Queue " + queueName + " has " + results.size() + " messages.");
            if (results.size() > 0) {
                System.out.println("[QUEUE TEST] Message details:");
                results.forEach(info -> System.out.println("  " + info));
            } else {
                System.out.println("[QUEUE TEST] Queue is empty - no messages to display.");
            }

        } catch (Exception e) {
            System.err.println("[QUEUE TEST] ❌ Connection failed: " + e.getMessage());
            System.err.println("[QUEUE TEST] Troubleshooting checklist:");
            System.err.println("  1. MQ server running on " + host + ":" + port + "?");
            System.err.println("  2. Queue manager '" + queueManager + "' started?");
            System.err.println("  3. Channel '" + channel + "' configured and running?");
            System.err.println("  4. Queue '" + queueName + "' exists?");
            System.err.println("  5. Firewall allows port " + port + "?");
            e.printStackTrace();
        } finally {
            if (browser != null)
                try {
                    browser.close();
                } catch (Exception ignore) {
                }
            if (context != null)
                context.close();
        }

        return results;
    }

    /**
     * Quick connectivity test (just connect/disconnect)
     */
    public boolean testConnection() {
        JMSContext context = null;
        try {
            System.out.println("[QUEUE TEST] Testing connection to " + host + ":" + port + "...");

            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
            factory.setIntProperty(WMQConstants.WMQ_PORT, port);
            factory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
            factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManager);

            if (user == null || user.trim().isEmpty()) {
                context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
            } else {
                context = factory.createContext(user, password, JMSContext.AUTO_ACKNOWLEDGE);
            }

            System.out.println("[QUEUE TEST] ✅ Connection successful!");
            return true;

        } catch (Exception e) {
            System.err.println("[QUEUE TEST] ❌ Connection failed: " + e.getMessage());
            return false;
        } finally {
            if (context != null)
                context.close();
        }
    }

    public static void main(String[] args) {
        BmgJmsQueueTesterVerbose tester = new BmgJmsQueueTesterVerbose();

        // Test connection first
        if (tester.testConnection()) {
            // If connection works, inspect the queue
            List<MessageInfo> infos = tester.inspectQueue();
            System.out.println("\n[QUEUE TEST] Found " + infos.size() + " messages in queue.");
        }
    }
}