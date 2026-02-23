package com.cbn.bloomberg.mq;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Scanner;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TextMessage;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * Bloomberg MQ Producer - Interactive testing utility for IBM MQ operations.
 * 
 * Features: - Push JSON messages to queue - Clear all messages from queue -
 * Inspect queue depth and browse messages - Peek at message content
 * 
 * Package: com.cbn.bloomberg.mq
 */
public class BloombergMqManager {

    private static final String PROPERTIES_FILE = "bloomberg.properties";
    private static Properties mqProperties;
    private static MQConnectionFactory mqFactory;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Bloomberg MQ Producer - Test Utility");
        System.out.println("========================================\n");

        try {
            loadMqConfiguration();
            System.out.println("✅ MQ Configuration loaded successfully");
            System.out.println("   Host: " + mqProperties.getProperty("mq.host"));
            System.out.println("   Port: " + mqProperties.getProperty("mq.port"));
            System.out.println("   Queue Manager: " + mqProperties.getProperty("mq.qmgr"));
            System.out.println("   Channel: " + mqProperties.getProperty("mq.channel"));
            System.out.println("   Queue: " + mqProperties.getProperty("mq.queue"));
            System.out.println();

            runInteractiveMenu();

        } catch (Exception e) {
            System.err.println("❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n========================================");
            System.out.println("  MQ Operations Menu");
            System.out.println("========================================");
            System.out.println("1. Push JSON message to queue");
            System.out.println("2. Push multiple messages from directory");
            System.out.println("3. Inspect queue (count & browse)");
            System.out.println("4. Peek at first message");
            System.out.println("5. Clear queue (delete all messages)");
            System.out.println("6. Exit");
            System.out.println("========================================");
            System.out.print("Select option [1-6]: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                case "1":
                    pushSingleMessage(scanner);
                    break;
                case "2":
                    pushMultipleMessages(scanner);
                    break;
                case "3":
                    inspectQueue();
                    break;
                case "4":
                    peekFirstMessage();
                    break;
                case "5":
                    clearQueue(scanner);
                    break;
                case "6":
                    running = false;
                    System.out.println("\n👋 Exiting... Goodbye!");
                    break;
                default:
                    System.out.println("⚠️  Invalid option. Please select 1-6.");
                }
            } catch (Exception e) {
                System.err.println("❌ Operation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    // ========== OPERATION 1: PUSH SINGLE MESSAGE ==========
    private static void pushSingleMessage(Scanner scanner) throws Exception {
        System.out.print("\nEnter JSON file path (or press Enter for default '3018_FT_FTXN_20251010_151115.json'): ");
        String filePath = scanner.nextLine().trim();

        if (filePath.isEmpty()) {
            filePath = "3018_FT_FTXN_20251010_151115.json";
        }

        Path jsonFile = Paths.get(filePath);
        if (!Files.exists(jsonFile)) {
            System.out.println("❌ File not found: " + jsonFile.toAbsolutePath());
            return;
        }

        String jsonContent = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);

        System.out.println("\n📄 File content preview (first 200 chars):");
        System.out.println(jsonContent.substring(0, Math.min(200, jsonContent.length())) + "...\n");

        System.out.print("Confirm push to queue? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("y")) {
            System.out.println("⚠️  Operation cancelled.");
            return;
        }

        JMSContext context = null;
        try {
            context = createMqContext();
            String queueName = mqProperties.getProperty("mq.queue");
            Queue queue = context.createQueue("queue:///" + queueName);

            TextMessage message = context.createTextMessage(jsonContent);
            message.setStringProperty("SourceFile", jsonFile.getFileName().toString());
            message.setStringProperty("PushedBy", "BloombergMqProducer");
            message.setLongProperty("PushedTimestamp", System.currentTimeMillis());

            context.createProducer().send(queue, message);

            System.out.println("✅ Message pushed successfully!");
            System.out.println("   Message ID: " + message.getJMSMessageID());
            System.out.println("   Size: " + jsonContent.length() + " bytes");

        } finally {
            if (context != null)
                context.close();
        }
    }

    // ========== OPERATION 2: PUSH MULTIPLE MESSAGES ==========
    private static void pushMultipleMessages(Scanner scanner) throws Exception {
        System.out.print("\nEnter directory path containing JSON files: ");
        String dirPath = scanner.nextLine().trim();

        if (dirPath.isEmpty()) {
            System.out.println("❌ Directory path cannot be empty.");
            return;
        }

        Path directory = Paths.get(dirPath);
        if (!Files.isDirectory(directory)) {
            System.out.println("❌ Not a valid directory: " + directory.toAbsolutePath());
            return;
        }

        System.out.print("Enter file pattern (e.g., *.json): ");
        String pattern = scanner.nextLine().trim();
        if (pattern.isEmpty()) {
            pattern = "*.json";
        }

        JMSContext context = null;
        int successCount = 0;
        int failCount = 0;

        try {
            context = createMqContext();
            String queueName = mqProperties.getProperty("mq.queue");
            Queue queue = context.createQueue("queue:///" + queueName);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path file : stream) {
                    try {
                        String jsonContent = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                        TextMessage message = context.createTextMessage(jsonContent);
                        message.setStringProperty("SourceFile", file.getFileName().toString());
                        message.setStringProperty("PushedBy", "BloombergMqProducer");
                        message.setLongProperty("PushedTimestamp", System.currentTimeMillis());

                        context.createProducer().send(queue, message);
                        successCount++;
                        System.out.println("✅ Pushed: " + file.getFileName());

                    } catch (Exception e) {
                        failCount++;
                        System.err.println("❌ Failed: " + file.getFileName() + " - " + e.getMessage());
                    }
                }
            }

            System.out.println("\n📊 Summary:");
            System.out.println("   Success: " + successCount);
            System.out.println("   Failed: " + failCount);

        } finally {
            if (context != null)
                context.close();
        }
    }

    // ========== OPERATION 3: INSPECT QUEUE ==========
    private static void inspectQueue() throws Exception {
        System.out.println("\n🔍 Inspecting queue...");

        JMSContext context = null;
        QueueBrowser browser = null;

        try {
            context = createMqContext();
            String queueName = mqProperties.getProperty("mq.queue");
            Queue queue = context.createQueue("queue:///" + queueName);

            browser = context.createBrowser(queue);
            Enumeration<?> messages = browser.getEnumeration();

            int count = 0;
            System.out.println("\n📋 Messages in queue:");
            System.out.println("----------------------------------------");

            while (messages.hasMoreElements()) {
                count++;
                Message msg = (Message) messages.nextElement();

                System.out.println("\n[" + count + "] Message ID: " + msg.getJMSMessageID());
                System.out.println("    Timestamp: " + new java.util.Date(msg.getJMSTimestamp()));

                try {
                    String sourceFile = msg.getStringProperty("SourceFile");
                    if (sourceFile != null) {
                        System.out.println("    Source File: " + sourceFile);
                    }
                } catch (Exception e) {
                    // Property may not exist
                }

                if (msg instanceof TextMessage) {
                    String body = ((TextMessage) msg).getText();
                    System.out.println("    Size: " + body.length() + " bytes");
                    System.out.println("    Preview: " + body.substring(0, Math.min(100, body.length())) + "...");
                }
            }

            System.out.println("\n----------------------------------------");
            System.out.println("📊 Total messages in queue: " + count);

            if (browser != null)
                browser.close();

        } finally {
            if (context != null)
                context.close();
        }
    }

    // ========== OPERATION 4: PEEK FIRST MESSAGE ==========
    private static void peekFirstMessage() throws Exception {
        System.out.println("\n👀 Peeking at first message...");

        JMSContext context = null;
        QueueBrowser browser = null;

        try {
            context = createMqContext();
            String queueName = mqProperties.getProperty("mq.queue");
            Queue queue = context.createQueue("queue:///" + queueName);

            browser = context.createBrowser(queue);
            Enumeration<?> messages = browser.getEnumeration();

            if (!messages.hasMoreElements()) {
                System.out.println("⚠️  Queue is empty.");
                return;
            }

            Message msg = (Message) messages.nextElement();

            System.out.println("\n📄 First Message Details:");
            System.out.println("----------------------------------------");
            System.out.println("Message ID: " + msg.getJMSMessageID());
            System.out.println("Timestamp: " + new java.util.Date(msg.getJMSTimestamp()));
            System.out.println("Type: " + msg.getClass().getSimpleName());

            try {
                String sourceFile = msg.getStringProperty("SourceFile");
                if (sourceFile != null) {
                    System.out.println("Source File: " + sourceFile);
                }
            } catch (Exception e) {
                // Property may not exist
            }

            if (msg instanceof TextMessage) {
                String body = ((TextMessage) msg).getText();
                System.out.println("\n📝 Full Content:");
                System.out.println("----------------------------------------");
                System.out.println(body);
                System.out.println("----------------------------------------");
            }

            if (browser != null)
                browser.close();

        } finally {
            if (context != null)
                context.close();
        }
    }

    // ========== OPERATION 5: CLEAR QUEUE ==========
    private static void clearQueue(Scanner scanner) throws Exception {
        System.out.println("\n⚠️  WARNING: This will delete ALL messages from the queue!");
        System.out.print("Type 'CONFIRM' to proceed: ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equals("CONFIRM")) {
            System.out.println("⚠️  Operation cancelled.");
            return;
        }

        JMSContext context = null;
        int deletedCount = 0;

        try {
            context = createMqContext(JMSContext.CLIENT_ACKNOWLEDGE);
            String queueName = mqProperties.getProperty("mq.queue");
            Queue queue = context.createQueue("queue:///" + queueName);

            JMSConsumer consumer = context.createConsumer(queue);

            System.out.println("\n🗑️  Clearing queue...");

            Message msg;
            while ((msg = consumer.receiveNoWait()) != null) {
                msg.acknowledge();
                deletedCount++;
                System.out.print(".");
                if (deletedCount % 50 == 0) {
                    System.out.println(" [" + deletedCount + "]");
                }
            }

            consumer.close();

            System.out.println("\n✅ Queue cleared successfully!");
            System.out.println("   Deleted messages: " + deletedCount);

        } finally {
            if (context != null)
                context.close();
        }
    }

    // ========== MQ CONNECTION HELPERS ==========

    private static void loadMqConfiguration() throws IOException, JMSException {
        mqProperties = new Properties();

        // 1) Try to load from classpath (src/main/resources)
        InputStream in = BloombergMqManager.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (in != null) {
            try (InputStream cp = in) {
                mqProperties.load(cp);
                System.out.println("📦 Loaded config from classpath: /" + PROPERTIES_FILE);
            }
            initMqFactory();
            return;
        }

        // 2) Optional overrides for external deployment flexibility
        String sysPropPath = System.getProperty("bloomberg.config");
        if (sysPropPath != null && !sysPropPath.trim().isEmpty()) {
            try (FileInputStream fis = new FileInputStream(sysPropPath)) {
                mqProperties.load(fis);
                System.out.println("📦 Loaded config from -Dbloomberg.config=" + sysPropPath);
                initMqFactory();
                return;
            }
        }

        String envConfig = System.getenv("BLOOMBERG_CONFIG");
        if (envConfig != null && !envConfig.trim().isEmpty()) {
            try (FileInputStream fis = new FileInputStream(envConfig)) {
                mqProperties.load(fis);
                System.out.println("📦 Loaded config from BLOOMBERG_CONFIG=" + envConfig);
                initMqFactory();
                return;
            }
        }

        throw new IOException("Could not locate bloomberg.properties on classpath or via overrides.");
    }

    private static void initMqFactory() throws JMSException {
        mqFactory = new MQConnectionFactory();
        mqFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        mqFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, mqProperties.getProperty("mq.host"));
        mqFactory.setIntProperty(WMQConstants.WMQ_PORT, Integer.parseInt(mqProperties.getProperty("mq.port")));
        mqFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, mqProperties.getProperty("mq.channel"));
        mqFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, mqProperties.getProperty("mq.qmgr"));
    }

    private static JMSContext createMqContext() throws JMSException {
        return createMqContext(JMSContext.AUTO_ACKNOWLEDGE);
    }

    private static JMSContext createMqContext(int sessionMode) throws JMSException {
        String user = mqProperties.getProperty("mq.user", "").trim();
        if (user.isEmpty()) {
            return mqFactory.createContext(sessionMode);
        } else {
            return mqFactory.createContext(user, mqProperties.getProperty("mq.password", ""), sessionMode);
        }
    }
}
