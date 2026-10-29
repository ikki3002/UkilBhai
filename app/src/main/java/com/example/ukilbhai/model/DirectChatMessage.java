package com.example.ukilbhai.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for a single message in a 1-on-1 direct consultation chat room.
 * Stored in Firestore subcollection: "chat_rooms/{roomId}/messages/{messageId}".
 */
public class DirectChatMessage implements Serializable {

    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;
    private boolean isRead;
    private String attachmentUrl;
    private String attachmentType; // "none", "image", "doc"

    public DirectChatMessage() {
        // Required for Firestore
    }

    public DirectChatMessage(String messageId, String senderId, String senderName, String text,
                             long timestamp, boolean isRead, String attachmentUrl, String attachmentType) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.attachmentUrl = attachmentUrl;
        this.attachmentType = (attachmentType != null) ? attachmentType : "none";
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentType() {
        return attachmentType != null ? attachmentType : "none";
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId != null ? messageId : "");
        map.put("senderId", senderId != null ? senderId : "");
        map.put("senderName", senderName != null ? senderName : "");
        map.put("text", text != null ? text : "");
        map.put("timestamp", timestamp);
        map.put("isRead", isRead);
        map.put("attachmentUrl", attachmentUrl != null ? attachmentUrl : "");
        map.put("attachmentType", getAttachmentType());
        return map;
    }
}
