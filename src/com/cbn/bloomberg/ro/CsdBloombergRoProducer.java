package com.cbn.bloomberg.ro;

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
import javax.jms.Session;
import javax.jms.TextMessage;

import com.cbn.bloomberg.hp.CsdBloombergRoProperties;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.common.CommonConstants;

/**
 * Title: CsdBloombergRoProducer.java Author: CSD Development Team Date Created:
 * 2025-11-06
 *
 * Purpose: Publishes RO transaction response messages to outbound channels.
 * Supports both FILE and WMQ (IBM MQ) output modes.
 *
 * Usage: Used by CsdBloombergFtService to publish response payloads after
 * transaction processing.
 */
public class CsdBloombergRoProducer {

    private static final Logger LOGGER = Logger.getLogger(CsdBloombergRoProducer.class.getName());
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Publishes a response message based on the adapter mode.
     *
     * @param jsonResponse  JSON response payload
     * @param adapterMode   Adapter mode ("FILE" or "WMQ")
     * @param transactionId Transaction ID for logging/tracking
     */
    public void publishResponse(String jsonResponse, String adapterMode, String transactionId) {
        try {
            if ("FILE".equalsIgnoreCase(adapterMode)) {
                publishToFile(jsonResponse, transactionId);
            } else if ("WMQ".equalsIgnoreCase(adapterMode)) {
                publishToMq(jsonResponse, transactionId);
            } else {
                LOGGER.log(Level.WARNING, "[CsdBloombergRoProducer] Unknown adapter mode: {0}", adapterMode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    String.format("[CsdBloombergRoProducer] Error publishing response for txn=%s", transactionId), e);
        }
    }

    /**
     * Publishes response to FILE system (outbound directory).
     */
    private void publishToFile(String jsonResponse, String transactionId) {
        try {
            CsdBloombergRoProperties props = CsdBloombergRoProperties.getInstance();
            String outboundDir = props.getProperty("ro.outbound.dir", "D:/Temenos/R24/bnk/UD/BLOOMBERG/OUT/RO");

            Path outDir = Paths.get(outboundDir);
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }

            String timestamp = LocalDateTime.now().format(TS_FMT);
            String fileName = String.format("FT_RESPONSE_%s_%s.json", extractPrefix(transactionId), timestamp);
            Path outFile = outDir.resolve(fileName);

            Files.write(outFile, jsonResponse.getBytes(StandardCharsets.UTF_8));
            LOGGER.log(Level.INFO, "[CsdBloombergRoProducer] FILE: Published response to {0}", outFile);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoProducer] FILE: Error writing response", e);
        }
    }

    /**
     * Publishes response to IBM MQ (outbound queue).
     */
    private void publishToMq(String jsonResponse, String transactionId) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            CsdBloombergRoProperties props = CsdBloombergRoProperties.getInstance();
            MQConnectionFactory factory = createMqFactory(props);

            String user = props.getProperty("mq.user", "").trim();
            connection = user.isEmpty() ? factory.createConnection()
                    : factory.createConnection(user, props.getProperty("mq.password", ""));

            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            String outboundQueue = props.getProperty("ro.mq.outbound.queue", "RO.RESPONSE.QUEUE");
            javax.jms.Queue queue = session.createQueue("queue:///" + outboundQueue);
            producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage(jsonResponse);
            producer.send(message);

            LOGGER.log(Level.INFO, "[CsdBloombergRoProducer] WMQ: Published response to queue {0}", outboundQueue);

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[CsdBloombergRoProducer] WMQ: Error publishing response", e);
        } finally {
            closeQuietly(producer);
            closeQuietly(session);
            closeQuietly(connection);
        }
    }

    /**
     * Creates MQ connection factory from properties.
     */
    private MQConnectionFactory createMqFactory(CsdBloombergRoProperties props) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(props.getProperty("mq.host", "172.105.249.157"));
        factory.setPort(Integer.parseInt(props.getProperty("mq.port", "1414")));
        factory.setChannel(props.getProperty("mq.channel", "DEV.APP.SVRCONN"));
        factory.setQueueManager(props.getProperty("mq.qmgr", "QM_BLOOMBERG"));
        factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
        return factory;
    }

    /**
     * Extracts prefix from transaction ID for file naming.
     */
    private String extractPrefix(String transactionId) {
        try {
            String[] segments = transactionId.split("\\|");
            if (segments.length < 2) {
                return "UNKNOWN";
            }
            String part = segments[1];
            int lastSlash = Math.max(part.lastIndexOf('/'), part.lastIndexOf('\\'));
            String fileName = lastSlash >= 0 ? part.substring(lastSlash + 1) : part;
            int dashIndex = fileName.indexOf('-');
            return dashIndex > 0 ? fileName.substring(0, dashIndex) : fileName;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private void closeQuietly(MessageProducer p) {
        if (p != null)
            try {
                p.close();
            } catch (Exception e) {
                /* ignore */ }
    }

    private void closeQuietly(Session s) {
        if (s != null)
            try {
                s.close();
            } catch (Exception e) {
                /* ignore */ }
    }

    private void closeQuietly(Connection c) {
        if (c != null)
            try {
                c.close();
            } catch (Exception e) {
                /* ignore */ }
    }
}