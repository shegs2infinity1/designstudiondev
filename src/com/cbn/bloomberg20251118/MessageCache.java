package com.cbn.bloomberg;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MessageCache {
    private static final Logger logger = Logger.getLogger(MessageCache.class.getName());
    private static MessageCache instance;
    private final Map<String, String> messageStore = new ConcurrentHashMap<>();

    private MessageCache() {}

    public static synchronized MessageCache getInstance() {
        if (instance == null) {
            instance = new MessageCache();
        }
        return instance;
    }

    public void storeMessage(String messageId, String message) {
        messageStore.put(messageId, message);
        logger.fine("Stored message: " + messageId);
    }

    public String getMessage(String messageId) {
        return messageStore.get(messageId);
    }

    public void removeMessage(String messageId) {
        messageStore.remove(messageId);
        logger.fine("Removed message: " + messageId);
    }

    public int size() {
        return messageStore.size();
    }
}