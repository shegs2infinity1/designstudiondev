package com.cbn.bloomberg.ft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.cbn.bloomberg.hp.CsdBloombergProperties;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.common.CommonConstants;

/**
 * Title: CsdBloombergFtProducer.java Author: CSD Development Team Date Created:
 * 2025-11-05
 * 
 * Purpose: MQ Producer for Bloomberg FT (Funds Transfer) integration. Publishes
 * JSON responses to MQ queue or writes to file system as fallback.
 * 
 * Usage: CsdBloombergFtProducer producer = new CsdBloombergFtProducer();
 * producer.publishResponse(jsonResponse, "WMQ", "txn123");
 * 
 * Modification Details: ---- 05/11/25 - Initial version Extracted MQ producer
 * logic from CsdBloombergFtService Added file-based fallback mechanism
 * Compliant with CSD Java Programming Standards r2022
 */
public class CsdBloombergFtProducer {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergFtProducer.class.getName());
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CsdBloombergProperties properties;
    private final Path responseDir;

    /**
     * Default constructor - loads properties and sets default response directory.
     */
    public CsdBloombergFtProducer() {
        this.properties = CsdBloombergProperties.getInstance();
        this.responseDir = Paths.get("/t24app/app/bnk/UD/BLOOMBERG/OUT/FT");
    }

    /**
     * Constructor with custom response directory.
     * 
     * @param p_responseDir Custom response directory path
     */
    public CsdBloombergFtProducer(Path p_responseDir) {
        this.properties = CsdBloombergProperties.getInstance();
        this.responseDir = p_responseDir != null ? p_responseDir
                : Paths.get("/t24app/app/bnk/UD/BLOOMBERG/OUT/FT");
    }

    /**
     * Publishes response based on adapter mode.
     * 
     * @param p_jsonResponse JSON response string
     * @param p_adapterMode  Adapter mode (FILE or WMQ)
     * @param p_id           Transaction ID for file naming
     */
    public void publishResponse(String p_jsonResponse, String p_adapterMode, String p_id) {
        if (p_jsonResponse == null || p_jsonResponse.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtProducer] Empty response, skipping publish");
            return;
        }

        if ("WMQ".equalsIgnoreCase(p_adapterMode)) {
            publishToMq(p_jsonResponse);
        } else if ("FILE".equalsIgnoreCase(p_adapterMode)) {
            writeResponseToFile(p_id, p_jsonResponse);
        } else {
            LOGGER.log(Level.WARNING, "[CsdBloombergFtProducer] Unknown adapter mode: {0}, defaulting to FILE",
                    p_adapterMode);
            writeResponseToFile(p_id, p_jsonResponse);
        }
    }

    /**
     * Publishes JSON response to MQ queue. Falls back to file-based response if MQ
     * publish fails.
     * 
     * @param p_jsonResponse JSON response string
     */
    public void publishToMq(String p_jsonResponse) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            // Create MQ connection factory
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(CommonConstants.WMQ_HOST_NAME, properties.getMqHost());
            factory.setIntProperty(CommonConstants.WMQ_PORT, properties.getMqPort());
            factory.setStringProperty(CommonConstants.WMQ_CHANNEL, properties.getMqChannel());
            factory.setStringProperty(CommonConstants.WMQ_QUEUE_MANAGER, properties.getMqQueueManager());

            // Optional authentication
            String mqUser = properties.getMqUser();
            if (mqUser != null && !mqUser.isEmpty()) {
                factory.setStringProperty(JmsConstants.USERID, mqUser);
                factory.setStringProperty(JmsConstants.PASSWORD, properties.getMqPassword());
                factory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true);
            }

            // Connect to MQ
            if (mqUser == null || mqUser.isEmpty()) {
                connection = factory.createConnection();
            } else {
                connection = factory.createConnection(mqUser, properties.getMqPassword());
            }
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //Queue queue = session.createQueue("queue:///" + properties.getMqResponseQueue());
            Queue queue = session.createQueue("queue:///" + properties.getMqPassword());
            producer = session.createProducer(queue);

            // Create and send text message
            TextMessage message = session.createTextMessage(p_jsonResponse);

            // Set IBM MQ specific properties for UTF-8 encoding
            try {
                message.setIntProperty(CommonConstants.JMS_IBM_CHARACTER_SET, 1208); // UTF-8
                message.setIntProperty(CommonConstants.JMS_IBM_ENCODING, CommonConstants.WMQ_ENCODING_NATIVE);
            } catch (JMSException e) {
                LOGGER.log(Level.WARNING, "[CsdBloombergFtProducer] Failed to set encoding properties", e);
            }

            producer.send(message);

            LOGGER.log(Level.INFO, "[CsdBloombergFtProducer] Published response to MQ queue: {0} (msgId={1})",
                    //new Object[] { properties.getMqResponseQueue(), message.getJMSMessageID() });
                    new Object[] { properties.getMqPassword(), message.getJMSMessageID() });

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtProducer] Error publishing to MQ: " + e.getMessage(), e);
            // Fallback: write to file if MQ fails
            LOGGER.log(Level.INFO, "[CsdBloombergFtProducer] Falling back to file-based response");
            writeResponseToFile("MQ_FALLBACK_" + System.currentTimeMillis(), p_jsonResponse);
        } finally {
            // Cleanup
            closeQuietly(producer);
            closeQuietly(session);
            closeQuietly(connection);
        }
    }

    /**
     * Writes JSON response to file in RESPONSE directory.
     * 
     * @param p_id           Transaction ID for file naming
     * @param p_jsonResponse JSON response string
     */
    public void writeResponseToFile(String p_id, String p_jsonResponse) {
        try {
            if (!Files.exists(responseDir)) {
                Files.createDirectories(responseDir);
                LOGGER.log(Level.INFO, "[CsdBloombergFtProducer] Created response directory: {0}", responseDir);
            }

            String timestamp = LocalDateTime.now().format(TS_FMT);
            String sanitizedId = p_id != null ? p_id.replaceAll("[^A-Za-z0-9_\\-]", "_") : "UNKNOWN";
            Path responseFile = responseDir.resolve("RESPONSE_" + sanitizedId + "_" + timestamp + ".json");

            Files.write(responseFile, p_jsonResponse.getBytes(StandardCharsets.UTF_8));

            LOGGER.log(Level.INFO, "[CsdBloombergFtProducer] Wrote response to file: {0}", responseFile);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergFtProducer] Error writing response to file", e);
        }
    }

    /**
     * Closes a MessageProducer quietly (suppressing exceptions).
     */
    private void closeQuietly(MessageProducer p_producer) {
        if (p_producer != null) {
            try {
                p_producer.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtProducer] Error closing producer (ignored)", e);
            }
        }
    }

    /**
     * Closes a Session quietly (suppressing exceptions).
     */
    private void closeQuietly(Session p_session) {
        if (p_session != null) {
            try {
                p_session.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtProducer] Error closing session (ignored)", e);
            }
        }
    }

    /**
     * Closes a Connection quietly (suppressing exceptions).
     */
    private void closeQuietly(Connection p_connection) {
        if (p_connection != null) {
            try {
                p_connection.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "[CsdBloombergFtProducer] Error closing connection (ignored)", e);
            }
        }
    }
}