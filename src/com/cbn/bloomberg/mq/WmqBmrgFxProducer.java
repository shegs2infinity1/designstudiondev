package com.cbn.bloomberg.mq;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

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
 * Configuration is loaded from bloomberg.properties in the resources directory.
 */
public class WmqBmrgFxProducer {

    private static final String PROPERTIES_FILE = "fx-bloomberg.properties";
    private static final String FILE_GLOB = "*.json";

    public static void main(String[] args) {
        System.out.println("=== Bloomberg MQ Producer (FX Module) ===");
        System.out.println();

        // Load properties
        Properties props = loadProperties();
        if (props == null) {
            System.err.println("❌ ERROR: Failed to load properties file: " + PROPERTIES_FILE);
            System.err.println(
                    "   Make sure the file is in your resources directory (src/main/resources or src/test/resources)");
            return;
        }

        // Extract MQ configuration
        String mqHost = props.getProperty("fx.wmq.host", "172.22.105.46");
        int mqPort = Integer.parseInt(props.getProperty("fx.wmq.port", "1414"));
        String mqChannel = props.getProperty("fx.wmq.channel", "DEV.APP.SVRCONN");
        String mqQmgr = props.getProperty("fx.wmq.manager", "QM_BLOOMBERG");
        String mqQueue = props.getProperty("fx.wmq.inbound.queue", "FX.INBOUND.QUEUE");
        String mqUser = props.getProperty("fx.wmq.username", "");
        String mqPass = props.getProperty("fx.wmq.password", "");

        // Expand placeholders for outbound directory
        String rawDir = props.getProperty("fx.nfs.outbound.dir", "${bloomberg.base.dir}\\OUT\\FX");
        String expandedDir = expandPlaceholders(rawDir, props);
        expandedDir = expandedDir.replace("\\", java.io.File.separator);
        Path outwardDir = Paths.get(expandedDir);

        // Display configuration
        System.out.println("📋 Configuration loaded from: " + PROPERTIES_FILE);
        System.out.println("   MQ Host: " + mqHost + ":" + mqPort);
        System.out.println("   Queue Manager: " + mqQmgr);
        System.out.println("   Channel: " + mqChannel);
        System.out.println("   Target Queue: " + mqQueue);
        System.out.println(
                "   Auth Mode: " + (mqUser.isEmpty() ? "No Auth (MCAUSER)" : "User/Password"));
        System.out.println("   Scanning Directory: " + outwardDir);
        System.out.println();

        if (!Files.isDirectory(outwardDir)) {
            System.err.println("❌ ERROR: Directory not found: " + outwardDir);
            System.err.println("   You can configure this in " + PROPERTIES_FILE
                    + " using property: fx.nfs.outbound.dir");
            return;
        }

        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            // Create MQ connection factory
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, mqHost);
            factory.setIntProperty(WMQConstants.WMQ_PORT, mqPort);
            factory.setStringProperty(WMQConstants.WMQ_CHANNEL, mqChannel);
            factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, mqQmgr);

            // Optional authentication
            if (!mqUser.isEmpty()) {
                factory.setStringProperty(WMQConstants.USERID, mqUser);
                factory.setStringProperty(WMQConstants.PASSWORD, mqPass);
                factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            }

            // Connect to MQ
            System.out.println("🔌 Connecting to MQ...");
            if (mqUser.isEmpty()) {
                connection = factory.createConnection();
            } else {
                connection = factory.createConnection(mqUser, mqPass);
            }
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("queue:///" + mqQueue);
            producer = session.createProducer(queue);

            System.out.println("✅ Connected to MQ successfully");
            System.out.println();

            // Scan and send files
            int sentCount = 0;
            int errorCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outwardDir, FILE_GLOB)) {
                for (Path file : stream) {
                    try {
                        String content = new String(Files.readAllBytes(file),
                                StandardCharsets.UTF_8);
                        if (content.trim().isEmpty()) {
                            System.out.println("⚠️  Skipped empty file: " + file.getFileName());
                            continue;
                        }
                        TextMessage message = session.createTextMessage(content);
                        producer.send(message);
                        sentCount++;
                        System.out.println("✅ Sent: " + file.getFileName() + " (msgId="
                                + message.getJMSMessageID() + ")");
                    } catch (IOException e) {
                        errorCount++;
                        System.err.println("❌ Error reading file " + file.getFileName() + ": "
                                + e.getMessage());
                    } catch (JMSException e) {
                        errorCount++;
                        System.err.println("❌ Error sending file " + file.getFileName() + ": "
                                + e.getMessage());
                    }
                }
            }

            System.out.println();
            System.out.println("=== Summary ===");
            System.out.println("✉️  Messages sent: " + sentCount);
            System.out.println("❌ Errors: " + errorCount);

        } catch (JMSException e) {
            System.err.println("❌ MQ Connection Error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("❌ Directory Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeQuietly(producer);
            closeQuietly(session);
            closeQuietly(connection);
        }

        System.out.println();
        System.out.println("✅ Done.");
    }

    /**
     * Expands ${property} placeholders recursively using loaded properties,
     * environment variables, or system properties.
     */
    private static String expandPlaceholders(String value, Properties props) {
        if (value == null) return null;
        String prev;
        do {
            prev = value;
            int start = value.indexOf("${");
            while (start >= 0) {
                int end = value.indexOf('}', start + 2);
                if (end < 0) break;
                String key = value.substring(start + 2, end);
                String repl = props.getProperty(key, System.getProperty(key, System.getenv(key)));
                if (repl == null) repl = "";
                value = value.substring(0, start) + repl + value.substring(end + 1);
                start = value.indexOf("${", start + repl.length());
            }
        } while (!value.equals(prev));
        return value;
    }

    /**
     * Loads properties from the classpath.
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    PROPERTIES_FILE);
            if (is == null)
                is = WmqBmrgFxProducer.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
            if (is == null) is = WmqBmrgFxProducer.class.getResourceAsStream("/" + PROPERTIES_FILE);
            if (is == null) {
                System.err.println(
                        "⚠️  Properties file not found in classpath: " + PROPERTIES_FILE);
                return null;
            }
            props.load(is);
            System.out.println("✅ Loaded properties from: " + PROPERTIES_FILE);
            return props;
        } catch (IOException e) {
            System.err.println("❌ Error loading properties: " + e.getMessage());
            return null;
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }

    /** Quietly closes JMS resources. */
    private static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignore) {
            }
        }
    }
}
