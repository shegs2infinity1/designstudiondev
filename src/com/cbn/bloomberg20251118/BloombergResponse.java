package com.cbn.bloomberg;

public class BloombergResponse {
    private String messageId;
    private String status;
    private String mnemonic;
    private String message;
    private String errorCode;
    private long timestamp;

    public BloombergResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMnemonic() { return mnemonic; }
    public void setMnemonic(String mnemonic) { this.mnemonic = mnemonic; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"messageId\":\"").append(messageId).append("\",");
        json.append("\"status\":\"").append(status).append("\",");
        json.append("\"timestamp\":").append(timestamp);
        if (mnemonic != null) {
            json.append(",\"mnemonic\":\"").append(mnemonic).append("\"");
        }
        if (message != null) {
            json.append(",\"message\":\"").append(message).append("\"");
        }
        if (errorCode != null) {
            json.append(",\"errorCode\":\"").append(errorCode).append("\"");
        }
        json.append("}");
        return json.toString();
    }
}