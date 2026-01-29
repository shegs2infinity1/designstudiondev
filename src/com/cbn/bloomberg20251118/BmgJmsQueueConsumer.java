package com.cbn.bloomberg;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JMS consumer wrapper. - Public API does not throw JMSException (safe to call
 * from Service without checked JMS types). - Internally uses JMS; on errors it
 * logs and returns empty lists.
 */
public class BmgJmsQueueConsumer {

    private static final Logger LOGGER = Logger.getLogger(BmgJmsQueueConsumer.class.getName());

    // Defaults (can be overridden via constructor)
    private static final String DEFAULT_HOST = "172.105.249.157";
    private static final int DEFAULT_PORT = 1414;
    private static final String DEFAULT_QM = "QM_BLOOMBERG";
    private static final String DEFAULT_QUEUE = "TEST.QUEUE";
    private static final String DEFAULT_CHANNEL = "DEV.APP.SVRCONN";
    private static final String DEFAULT_USER = ""; // MCAUSER mapping
    private static final String DEFAULT_PASSWORD = "";

    private String host;
    private int port;
    private String qmName;
    private String queueName;
    private String channel;
    private String user;
    private String password;

    private JMSContext context;
    private JMSConsumer consumer;
    private boolean initialized = false;
    private boolean externalContext = false;

    public BmgJmsQueueConsumer() {
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.qmName = DEFAULT_QM;
        this.queueName = DEFAULT_QUEUE;
        this.channel = DEFAULT_CHANNEL;
        this.user = DEFAULT_USER;
        this.password = DEFAULT_PASSWORD;
    }

    public BmgJmsQueueConsumer(String hostPort, String qmName, String queueName, String channel, String user,
            String password) {
        String[] parts = hostPort.split(":");
        this.host = parts[0];
        this.port = Integer.parseInt(parts[1]);
        this.qmName = qmName;
        this.queueName = queueName;
        this.channel = channel;
        this.user = user;
        this.password = password;
    }

    public BmgJmsQueueConsumer(JMSContext externalContext, String queueName) {
        if (externalContext == null) {
            throw new IllegalArgumentException("externalContext cannot be null");
        }
        this.context = externalContext;
        this.queueName = queueName == null ? DEFAULT_QUEUE : queueName;
        this.externalContext = true;
    }

    /**
     * Initialize the consumer. Throws RuntimeException on unrecoverable init
     * errors. (kept internal; public APIs will catch and convert errors to empty
     * results)
     */
    private void init() {
        if (initialized)
            return;

        try {
            LOGGER.info("Initializing WMQ consumer: host=" + host + ", port=" + port + ", queueManager=" + qmName
                    + ", queue=" + queueName + ", channel=" + channel + ", externalContext=" + externalContext);

            if (!externalContext) {
                MQConnectionFactory factory = new MQConnectionFactory();
                factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
                factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
                factory.setIntProperty(WMQConstants.WMQ_PORT, port);
                factory.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
                factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, qmName);

                if (user == null || user.trim().isEmpty()) {
                    context = factory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
                    LOGGER.info("Created JMSContext in no-auth mode (MCAUSER)");
                } else {
                    context = factory.createContext(user, password, JMSContext.AUTO_ACKNOWLEDGE);
                    LOGGER.info("Created JMSContext with user: " + user);
                }
            }

            Queue queue = context.createQueue("queue:///" + queueName);
            consumer = context.createConsumer(queue);

            initialized = true;
            LOGGER.info("MQ consumer initialized successfully");
        } catch (Exception e) {
            // Initialization failed - log and mark not-initialized
            initialized = false;
            LOGGER.log(Level.SEVERE, "Failed to initialize MQ consumer", e);
            throw new RuntimeException("Failed to initialize MQ consumer", e);
        }
    }

    /**
     * Read payloads only. Returns empty list on error.
     */
    public List<String> readMessages() {
        try {
            List<MessageDetail> details = readMessagesDetailed();
            if (details == null || details.isEmpty())
                return Collections.emptyList();
            List<String> payloads = new ArrayList<>(details.size());
            for (MessageDetail d : details)
                payloads.add(d.payload);
            return payloads;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading messages (payloads)", e);
            return Collections.emptyList();
        }
    }

    /**
     * Read detailed messages. Returns empty list on error.
     */
    public List<MessageDetail> readMessagesDetailed() {
        try {
            init();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot init consumer; returning empty message list", e);
            return Collections.emptyList();
        }

        List<MessageDetail> results = new ArrayList<>();
        LOGGER.info("Reading messages (detailed) from queue: " + queueName);

        try {
            while (true) {
                Message msg = consumer.receiveNoWait();
                if (msg == null) {
                    LOGGER.info("No more messages available in queue");
                    break;
                }
                String payload = null;
                try {
                    payload = msg.getBody(String.class);
                } catch (JMSException je) {
                    LOGGER.log(Level.WARNING, "Failed to extract message body as String", je);
                }
                long ts = 0L;
                String corr = null;
                String mid = null;
                try {
                    ts = msg.getJMSTimestamp();
                    corr = msg.getJMSCorrelationID();
                    mid = msg.getJMSMessageID();
                } catch (JMSException ignore) {
                }

                MessageDetail detail = new MessageDetail(payload, ts, corr, mid);
                LOGGER.fine("Read " + detail);
                results.add(detail);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error while reading messages", e);
            return Collections.emptyList();
        }

        LOGGER.info("Total messages read (detailed): " + results.size());
        return results;
    }

    public void close() {
        try {
            if (consumer != null) {
                consumer.close();
                LOGGER.info("JMSConsumer closed");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing JMSConsumer", e);
        }
        try {
            if (!externalContext && context != null) {
                context.close();
                LOGGER.info("JMSContext closed");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing JMSContext", e);
        }
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // Observability DTO
    public static class MessageDetail {
        public final String payload;
        public final long jmsTimestamp;
        public final String correlationId;
        public final String messageId;

        public MessageDetail(String payload, long jmsTimestamp, String correlationId, String messageId) {
            this.payload = payload;
            this.jmsTimestamp = jmsTimestamp;
            this.correlationId = correlationId;
            this.messageId = messageId;
        }

        @Override
        public String toString() {
            return "MessageDetail{messageId=" + messageId + ", correlationId=" + correlationId + ", jmsTimestamp="
                    + jmsTimestamp + ", payloadLen=" + (payload == null ? 0 : payload.length()) + "}";
        }
    }

    // getters for observability
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getChannel() {
        return channel;
    }

    public String getQueueManager() {
        return qmName;
    }
}