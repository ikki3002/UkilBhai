package com.example.ukilbhai.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for a 1-on-1 direct consultation chat room between a Citizen and an Advocate.
 * Stored in Cloud Firestore collection: "chat_rooms/{roomId}".
 */
public class DirectChatRoom implements Serializable {

    private String roomId;
    private String citizenId;
    private String citizenName;
    private String citizenPhoto;

    private String lawyerId;
    private String lawyerName;
    private String lawyerPhoto;

    private String caseCategory;
    private String lastMessage;
    private long lastMessageTime;
    private String lastSenderId;

    private int unreadCitizenCount = 0;
    private int unreadLawyerCount = 0;
    private boolean isActive = true;

    public DirectChatRoom() {
        // Required for Firestore
    }

    public DirectChatRoom(String roomId, String citizenId, String citizenName, String citizenPhoto,
                          String lawyerId, String lawyerName, String lawyerPhoto, String caseCategory) {
        this.roomId = roomId;
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.citizenPhoto = citizenPhoto;
        this.lawyerId = lawyerId;
        this.lawyerName = lawyerName;
        this.lawyerPhoto = lawyerPhoto;
        this.caseCategory = caseCategory;
        this.lastMessage = "পরামর্শ চ্যাট শুরু হয়েছে";
        this.lastMessageTime = System.currentTimeMillis();
        this.isActive = true;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getCitizenPhoto() {
        return citizenPhoto;
    }

    public void setCitizenPhoto(String citizenPhoto) {
        this.citizenPhoto = citizenPhoto;
    }

    public String getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(String lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public String getLawyerPhoto() {
        return lawyerPhoto;
    }

    public void setLawyerPhoto(String lawyerPhoto) {
        this.lawyerPhoto = lawyerPhoto;
    }

    public String getCaseCategory() {
        return caseCategory;
    }

    public void setCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public int getUnreadCitizenCount() {
        return unreadCitizenCount;
    }

    public void setUnreadCitizenCount(int unreadCitizenCount) {
        this.unreadCitizenCount = unreadCitizenCount;
    }

    public int getUnreadLawyerCount() {
        return unreadLawyerCount;
    }

    public void setUnreadLawyerCount(int unreadLawyerCount) {
        this.unreadLawyerCount = unreadLawyerCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", roomId != null ? roomId : "");
        map.put("citizenId", citizenId != null ? citizenId : "");
        map.put("citizenName", citizenName != null ? citizenName : "");
        map.put("citizenPhoto", citizenPhoto != null ? citizenPhoto : "");
        map.put("lawyerId", lawyerId != null ? lawyerId : "");
        map.put("lawyerName", lawyerName != null ? lawyerName : "");
        map.put("lawyerPhoto", lawyerPhoto != null ? lawyerPhoto : "");
        map.put("caseCategory", caseCategory != null ? caseCategory : "");
        map.put("lastMessage", lastMessage != null ? lastMessage : "");
        map.put("lastMessageTime", lastMessageTime);
        map.put("lastSenderId", lastSenderId != null ? lastSenderId : "");
        map.put("unreadCitizenCount", unreadCitizenCount);
        map.put("unreadLawyerCount", unreadLawyerCount);
        map.put("isActive", isActive);
        return map;
    }
}
