package com.cbn.bloomberg;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ImprovedJMSQueueSender {
    private static final Logger logger = Logger.getLogger(ImprovedJMSQueueSender.class.getName());

    // Reuse same config as reader for now
    private final String HOST = "196.216.200.214";
    private final int PORT = 1415;
    private final String CHANNEL = "DEV.APP.SVRCONN";
    private final String QMGR = "QM_BLOOMBERG";
    private final String QUSER = "CBN";
    private final String QPASS = "CBNPASS";

    private MQConnectionFactory factory;

    public ImprovedJMSQueueSender() {
        try {
            factory = new MQConnectionFactory();
            factory.setHostName(HOST);
            factory.setPort(PORT);
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setQueueManager(QMGR);
            factory.setChannel(CHANNEL);
            factory.setIntProperty(WMQConstants.JMS_IBM_ENCODING, 273);
            factory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208);
            factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        } catch (JMSException e) {
            throw new RuntimeException("Failed to initialize MQConnectionFactory", e);
        }
    }

    public boolean sendMessage(String queueName, String messageText) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            connection = factory.createConnection(QUSER, QPASS);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);
            producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage(messageText);
            connection.start();

            producer.send(message);
            logger.info("Sent message to " + queueName);
            return true;

        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Error sending message to queue: " + queueName, e);
            return false;
        } finally {
            try { if (producer != null) producer.close(); } catch (Exception ignored) {}
            try { if (session != null) session.close(); } catch (Exception ignored) {}
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }
    }
}