package com.cbn.bloomberg.mq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * Test utility to push JSON files from OUTWARD directory to IBM MQ queue.
 * 
 * Usage: Run this class as a standalone Java application. It will scan
 * D:\Temenos\R24\bnk\UD\BLOOMBERG\OUTWARD for *.json files and send each file's
 * content as a TextMessage to TEST.QUEUE.
 */
public class WmqBmrgFtProducer2 {

    // MQ Configuration (matches your service settings- mqdev)
    private static final String MQ_HOST = "172.105.249.157";
    private static final int MQ_PORT = 1414;
    private static final String MQ_CHAN = "DEV.APP.SVRCONN";
    private static final String MQ_QMGR = "QM_BLOOMBERG"; 
    private static final String MQ_QUEUE = "TEST.QUEUE";
    private static final String MQ_USER = ""; // Empty = no auth
    private static final String MQ_PASS = "";

    // File Configuration
    private static final Path OUTWARD_DIR = Paths.get("D:", "Temenos", "R24", "bnk", "UD", "BLOOMBERG", "OUTWARD",
            "FT");
    // private static final Path OUTWARD_DIR = Paths.get("D:", "Temenos", "R24",
    // "bnk", "UD", "BLOOMBERG", "OUTWARD", "FX");
    // private static final Path OUTWARD_DIR = Paths.get("D:", "Temenos", "R24",
    // "bnk", "UD", "BLOOMBERG", "OUTWARD", "PC");
    // private static final Path OUTWARD_DIR = Paths.get("D:", "Temenos", "R24",
    // "bnk", "UD", "BLOOMBERG", "OUTWARD", "RO");
    // private static final Path OUTWARD_DIR = Paths.get("D:", "Temenos", "R24",
    // "bnk", "UD", "BLOOMBERG", "OUTWARD", "SC");

    private static final String FILE_GLOB = "*.json"; 

    public static void main(String[] args) {
        System.out.println("=== Bloomberg MQ Producer ===");
        System.out.println("Scanning directory: " + OUTWARD_DIR);
        System.out.println("Target queue: " + MQ_QUEUE + " on " + MQ_QMGR);
        System.out.println();

        if (!Files.isDirectory(OUTWARD_DIR)) {
            System.err.println("ERROR: Directory not found: " + OUTWARD_DIR);
            return;
        }

        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            // Create MQ connection factory
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, MQ_HOST);
            factory.setIntProperty(WMQConstants.WMQ_PORT, MQ_PORT);
            factory.setStringProperty(WMQConstants.WMQ_CHANNEL, MQ_CHAN);
            factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, MQ_QMGR);

            // Optional authentication
            if (!MQ_USER.isEmpty()) {
                factory.setStringProperty(WMQConstants.USERID, MQ_USER);
                factory.setStringProperty(WMQConstants.PASSWORD, MQ_PASS);
                factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            }

            // Connect to MQ
            System.out.println("Connecting to MQ...");
            if (MQ_USER.isEmpty()) { 
                connection = factory.createConnection();
            } else {
                connection = factory.createConnection(MQ_USER, MQ_PASS);
            }
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("queue:///" + MQ_QUEUE);
            producer = session.createProducer(queue);

            System.out.println("✅ Connected to MQ successfully");
            System.out.println();

            // Scan and send files
            int sentCount = 0;
            int errorCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(OUTWARD_DIR, FILE_GLOB)) {
                for (Path file : stream) {
                    try {
                        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

                        if (content.trim().isEmpty()) {
                            System.out.println("⚠️  Skipped empty file: " + file.getFileName());
                            continue;
                        }

                        // Create and send text message
                        TextMessage message = session.createTextMessage(content);
                        producer.send(message);

                        sentCount++;
                        System.out.println(
                                "✅ Sent: " + file.getFileName() + " (msgId=" + message.getJMSMessageID() + ")");

                    } catch (IOException e) {
                        errorCount++;
                        System.err.println("❌ Error reading file " + file.getFileName() + ": " + e.getMessage());
                    } catch (JMSException e) {
                        errorCount++;
                        System.err.println("❌ Error sending file " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            System.out.println();
            System.out.println("=== Summary ===");
            System.out.println("Messages sent: " + sentCount);
            System.out.println("Errors: " + errorCount);

        } catch (JMSException e) {
            System.err.println("❌ MQ Connection Error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) { 
            System.err.println("❌ Directory Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup 
            try {
                if (producer != null)
                    producer.close();
            } catch (Exception ignore) {
            }
            try {
                if (session != null)
                    session.close();
            } catch (Exception ignore) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (Exception ignore) {
            }
        }

        System.out.println();
        System.out.println("Done.");
    }
}