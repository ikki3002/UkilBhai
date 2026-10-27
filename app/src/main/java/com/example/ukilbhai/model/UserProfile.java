package com.example.ukilbhai.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for citizen/user legal profile stored in Firestore.
 */
public class UserProfile implements Serializable {

    private String uid;
    private String name;
    private String email;
    private String nid;
    private String location;
    private String address;
    private long createdAt;
    private boolean isGuest;

    public UserProfile() {
        // Required for Firestore deserialization
    }

    public UserProfile(String uid, String name, String email, String nid, String location, String address, long createdAt, boolean isGuest) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.nid = nid;
        this.location = location;
        this.address = address;
        this.createdAt = createdAt;
        this.isGuest = isGuest;
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

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isGuest() {
        return isGuest;
    }

    public void setGuest(boolean guest) {
        isGuest = guest;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid != null ? uid : "");
        map.put("name", name != null ? name : "");
        map.put("email", email != null ? email : "");
        map.put("nid", nid != null ? nid : "");
        map.put("location", location != null ? location : "");
        map.put("address", address != null ? address : "");
        map.put("createdAt", createdAt);
        map.put("isGuest", isGuest);
        return map;
    }
}
