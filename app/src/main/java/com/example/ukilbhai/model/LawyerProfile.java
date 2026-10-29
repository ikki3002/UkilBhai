package com.example.ukilbhai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data model for accredited Advocate / Lawyer profile in Ukil Bhai.
 * Stored in Cloud Firestore collection: "lawyers/{uid}".
 */
public class LawyerProfile implements Serializable {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String photoUrl;
    private String district;

    // Professional Advocate Credentials
    private String barCouncilNo;      // e.g. "BC-1948/2018"
    private String barAssociation;    // e.g. "ঢাকা আইনজীবী সমিতি"
    private String courtAffiliation;   // e.g. "জেলা ও দায়রা জজ আদালত, ঢাকা"
    private String designation;       // e.g. "অ্যাডভোকেট" or "হাইকোর্ট বিভাগের আইনজীবী"

    // Specialties / Practice Area Tags
    private List<String> practiceAreas = new ArrayList<>();

    // Professional History & Bio
    private String bio;
    private int experienceYears = 0;
    private double consultationFee = 0.0; // in BDT

    // Real-time Availability & Presence
    private boolean isOnline = false;
    private boolean isAvailable = true;
    private long lastSeen = 0L;

    // Ratings & Performance
    private double rating = 5.0;
    private int reviewCount = 0;
    private int completedConsultations = 0;

    private long createdAt;

    public LawyerProfile() {
        // Required for Firestore deserialization
    }

    public LawyerProfile(String uid, String name, String email, String phone, String photoUrl,
                         String district, String barCouncilNo, String barAssociation,
                         String courtAffiliation, String designation, List<String> practiceAreas,
                         String bio, int experienceYears, double consultationFee,
                         boolean isOnline, boolean isAvailable, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.district = district;
        this.barCouncilNo = barCouncilNo;
        this.barAssociation = barAssociation;
        this.courtAffiliation = courtAffiliation;
        this.designation = designation;
        if (practiceAreas != null) {
            this.practiceAreas = new ArrayList<>(practiceAreas);
        }
        this.bio = bio;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
        this.isOnline = isOnline;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getBarCouncilNo() {
        return barCouncilNo;
    }

    public void setBarCouncilNo(String barCouncilNo) {
        this.barCouncilNo = barCouncilNo;
    }

    public String getBarAssociation() {
        return barAssociation;
    }

    public void setBarAssociation(String barAssociation) {
        this.barAssociation = barAssociation;
    }

    public String getCourtAffiliation() {
        return courtAffiliation;
    }

    public void setCourtAffiliation(String courtAffiliation) {
        this.courtAffiliation = courtAffiliation;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public List<String> getPracticeAreas() {
        return practiceAreas != null ? practiceAreas : new ArrayList<>();
    }

    public void setPracticeAreas(List<String> practiceAreas) {
        this.practiceAreas = practiceAreas != null ? practiceAreas : new ArrayList<>();
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getCompletedConsultations() {
        return completedConsultations;
    }

    public void setCompletedConsultations(int completedConsultations) {
        this.completedConsultations = completedConsultations;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid != null ? uid : "");
        map.put("name", name != null ? name : "");
        map.put("email", email != null ? email : "");
        map.put("phone", phone != null ? phone : "");
        map.put("photoUrl", photoUrl != null ? photoUrl : "");
        map.put("district", district != null ? district : "");
        map.put("barCouncilNo", barCouncilNo != null ? barCouncilNo : "");
        map.put("barAssociation", barAssociation != null ? barAssociation : "");
        map.put("courtAffiliation", courtAffiliation != null ? courtAffiliation : "");
        map.put("designation", designation != null ? designation : "");
        map.put("practiceAreas", practiceAreas != null ? practiceAreas : new ArrayList<>());
        map.put("bio", bio != null ? bio : "");
        map.put("experienceYears", experienceYears);
        map.put("consultationFee", consultationFee);
        map.put("isOnline", isOnline);
        map.put("isAvailable", isAvailable);
        map.put("lastSeen", lastSeen);
        map.put("rating", rating);
        map.put("reviewCount", reviewCount);
        map.put("completedConsultations", completedConsultations);
        map.put("createdAt", createdAt);
        return map;
    }
}
