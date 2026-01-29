package com.cbn.bloomberg;

import javax.jms.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * Interactive Producer to push Bloomberg sample JSON messages into IBM MQ.
 * No-auth mode: uses MCAUSER channel mapping, no user/password required.
 */
public class BmgJmsQueueProducer {

    private static String HOST;
    private static int PORT;
    private static String CHANNEL;
    private static String QMGR;
    private static String QUEUE_NAME;

    public static void main(String[] args) {

        loadConfig(); // Load application.properties and print settings

        BmgJmsQueueProducer producer = new BmgJmsQueueProducer();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Bloomberg MQ Test Producer ===");
        System.out.println("Connected to queue: " + QUEUE_NAME);

        while (true) {
            producer.showMenu();
            System.out.print("Select option: ");
            String choice = scanner.nextLine().trim().toUpperCase();

            switch (choice) {
            case "1":
                producer.pushSecurityMasterSample();
                break;
            case "2":
                producer.pushPlacementSample();
                break;
            case "3":
                producer.pushRepoSample();
                break;
            case "4":
                producer.pushSecurityTradeSample();
                break;
            case "5":
                producer.pushForexSample();
                break;
            case "6":
                producer.pushFundMovementSample();
                break;
            case "7":
                for (int i = 0; i < 5; i++) {
                    producer.pushSecurityMasterSample();
                    producer.pushPlacementSample();
                    producer.pushRepoSample();
                    producer.pushSecurityTradeSample();
                    producer.pushForexSample();
                    producer.pushFundMovementSample();
                }
                System.out.println("✓ Sent bulk (30 messages).");
                producer.printQueueDepth();
                break;
            case "Q":
            case "QUIT":
            case "EXIT":
                System.out.println("Exiting producer.");
                scanner.close();
                return;
            default:
                System.out.println("Invalid choice, try again.");
            }
        }
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            props.load(fis);
            HOST = props.getProperty("mq.host", "172.105.249.157");
            PORT = Integer.parseInt(props.getProperty("mq.port", "1414"));
            CHANNEL = props.getProperty("mq.channel", "DEV.APP.SVRCONN");
            QMGR = props.getProperty("mq.qmgr", "QM_BLOOMBERG");
            QUEUE_NAME = props.getProperty("mq.queue", "TEST.QUEUE");

            // ✅ Log config values
            System.out.println("=== Loaded application.properties ===");
            System.out.println(" Host:    " + HOST);
            System.out.println(" Port:    " + PORT);
            System.out.println(" Channel: " + CHANNEL);
            System.out.println(" QMgr:    " + QMGR);
            System.out.println(" Queue:   " + QUEUE_NAME);
            System.out.println(" Auth mode: No-Auth (MCAUSER mapping)");
            System.out.println("====\n");

        } catch (IOException e) {
            System.err.println("✗ Failed to load application.properties: " + e.getMessage());
            System.exit(1);
        }
    }

    private void showMenu() {
        System.out.println("\n--- Message Types ---");
        System.out.println("1. Security Master");
        System.out.println("2. Placement");
        System.out.println("3. Repo");
        System.out.println("4. Security Trade");
        System.out.println("5. Forex Transaction");
        System.out.println("6. Fund Movement");
        System.out.println("7. Bulk Send (5 of each)");
        System.out.println("Q. Quit");
    }

    private MQConnectionFactory createFactory() throws JMSException {
        MQConnectionFactory cf = new MQConnectionFactory();
        cf.setHostName(HOST);
        cf.setPort(PORT);
        cf.setQueueManager(QMGR);
        cf.setChannel(CHANNEL);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);

        // ✅ Force no-auth mode (uses MCAUSER channel mapping)
        System.out.println("Connecting without credentials (MCAUSER mapping)...");
        cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);

        return cf;
    }

    private void sendJson(String payload, String typeLabel) {
        try (Connection conn = createFactory().createConnection("", ""); // ✅ BLANK CREDS
                Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            conn.start();

            javax.jms.Queue queue = session.createQueue("queue:///" + QUEUE_NAME);
            try (MessageProducer producer = session.createProducer(queue)) {
                TextMessage msg = session.createTextMessage(payload);
                msg.setStringProperty("MessageType", typeLabel);
                msg.setStringProperty("Source", "BloombergTestCLI");
                producer.send(msg);
                System.out.println("✓ Sent " + typeLabel + " message");

                printQueueDepth();
            }
        } catch (JMSException e) {
            System.err.println("✗ Send failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printQueueDepth() {
        try (Connection conn = createFactory().createConnection("", ""); // ✅ BLANK CREDS
                Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            conn.start();
            javax.jms.Queue queue = session.createQueue("queue:///" + QUEUE_NAME);
            QueueBrowser browser = session.createBrowser(queue);
            int depth = 0;
            java.util.Enumeration<?> msgs = browser.getEnumeration();
            while (msgs.hasMoreElements()) {
                msgs.nextElement();
                depth++;
            }
            browser.close();
            System.out.println("➡ Queue " + QUEUE_NAME + " depth is now: " + depth);

        } catch (JMSException e) {
            System.err.println("✗ Depth check failed: " + e.getMessage());
        }
    }

    // --- Message Types ---
    private void pushSecurityMasterSample() {
        String json = "{ \"SECURITY_MASTER\": { \"DESCRIPTION\": \"Gov Bond 2028\","
                + "\"SHORT_NAME\": \"GovBond28\", \"MNEMONIC\": \"GB28\","
                + "\"SECURITY_DOMICILE\": \"NG\", \"SECURITY_CURRENCY\": \"NGN\" } }";
        sendJson(json, "SECURITY_MASTER");
    }

    private void pushPlacementSample() {
        String json = "{ \"PLACEMENTS\": { \"CUSTOMER_NO\": \"CUST100234\","
                + "\"CURRENCY\": \"USD\", \"PRINCIPAL\": 50000.00 } }";
        sendJson(json, "PLACEMENTS");
    }

    private void pushRepoSample() {
        String json = "{ \"REPO\": { \"COUNTERPARTY\": \"HSBC London\","
                + "\"CURRENCY\": \"USD\", \"TRADE_DATE\": \"20250429\" } }";
        sendJson(json, "REPO");
    }

    private void pushSecurityTradeSample() {
        String json = "{ \"SEC_TRADE\": { \"TRANS_TYPE\": \"BUY\","
                + "\"SECURITY_NO\": \"SEC-00012345\", \"TRADE_DATE\": \"20250428\" } }";
        sendJson(json, "SEC_TRADE");
    }

    private void pushForexSample() {
        String json = "{ \"FOREX_TRANSACTION\": { \"COUNTERPARTY\": \"100001981\","
                + "\"CURRENCY_BOUGHT\": \"USD\", \"BUY_AMOUNT\": 10000.00 } }";
        sendJson(json, "FOREX_TRANSACTION");
    }

    private void pushFundMovementSample() {
        String json = "{ \"FUNDS_MOVEMENT\": { \"DEBIT_ACCT_NO\": \"1234567890\","
                + "\"DEBIT_CURRENCY\": \"USD\", \"DEBIT_AMOUNT\": 50000 } }";
        sendJson(json, "FUND_MOVEMENT");
    }
}