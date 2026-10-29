package com.example.ukilbhai.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for client-to-lawyer consultation application.
 * Stored in Cloud Firestore collection: "consultation_requests/{requestId}".
 */
public class ConsultationRequest implements Serializable {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String requestId;
    private String citizenId;
    private String citizenName;
    private String citizenPhone;
    private String citizenDistrict;
    private String citizenPhoto;

    private String lawyerId;
    private String lawyerName;
    private String lawyerPhoto;

    private String caseCategory;
    private String caseSummary;

    private String status = STATUS_PENDING;
    private String chatRoomId;

    private long requestedAt;
    private long respondedAt;

    public ConsultationRequest() {
        // Required for Firestore
    }

    public ConsultationRequest(String requestId, String citizenId, String citizenName,
                               String citizenPhone, String citizenDistrict, String citizenPhoto,
                               String lawyerId, String lawyerName, String lawyerPhoto,
                               String caseCategory, String caseSummary, long requestedAt) {
        this.requestId = requestId;
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.citizenPhone = citizenPhone;
        this.citizenDistrict = citizenDistrict;
        this.citizenPhoto = citizenPhoto;
        this.lawyerId = lawyerId;
        this.lawyerName = lawyerName;
        this.lawyerPhoto = lawyerPhoto;
        this.caseCategory = caseCategory;
        this.caseSummary = caseSummary;
        this.requestedAt = requestedAt;
        this.status = STATUS_PENDING;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public String getCitizenPhone() {
        return citizenPhone;
    }

    public void setCitizenPhone(String citizenPhone) {
        this.citizenPhone = citizenPhone;
    }

    public String getCitizenDistrict() {
        return citizenDistrict;
    }

    public void setCitizenDistrict(String citizenDistrict) {
        this.citizenDistrict = citizenDistrict;
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

    public String getCaseSummary() {
        return caseSummary;
    }

    public void setCaseSummary(String caseSummary) {
        this.caseSummary = caseSummary;
    }

    public String getStatus() {
        return status != null ? status : STATUS_PENDING;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public long getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId != null ? requestId : "");
        map.put("citizenId", citizenId != null ? citizenId : "");
        map.put("citizenName", citizenName != null ? citizenName : "");
        map.put("citizenPhone", citizenPhone != null ? citizenPhone : "");
        map.put("citizenDistrict", citizenDistrict != null ? citizenDistrict : "");
        map.put("citizenPhoto", citizenPhoto != null ? citizenPhoto : "");
        map.put("lawyerId", lawyerId != null ? lawyerId : "");
        map.put("lawyerName", lawyerName != null ? lawyerName : "");
        map.put("lawyerPhoto", lawyerPhoto != null ? lawyerPhoto : "");
        map.put("caseCategory", caseCategory != null ? caseCategory : "");
        map.put("caseSummary", caseSummary != null ? caseSummary : "");
        map.put("status", getStatus());
        map.put("chatRoomId", chatRoomId != null ? chatRoomId : "");
        map.put("requestedAt", requestedAt);
        map.put("respondedAt", respondedAt);
        return map;
    }
}
