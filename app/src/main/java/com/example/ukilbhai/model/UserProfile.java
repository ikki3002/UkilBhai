package com.example.ukilbhai.model;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data model for citizen/user legal profile stored in Cloud Firestore.
 */
public class UserProfile implements Serializable {

    private String uid;
    private String name;
    private String email;
    private String nid;
    private String photoUrl;

    // Structured Bangladesh Legal Address
    private String district;      // জেলা (e.g. ঢাকা)
    private String thana;         // থানা / উপজেলা (e.g. ধানমন্ডি)
    private String unionWard;      // ইউনিয়ন / ওয়ার্ড (e.g. ওয়ার্ড নং ১৫)
    private String postOffice;     // ডাকঘর ও কোড (e.g. মোহাম্মদপুর - ১২০৭)
    private String villageRoad;    // গ্রাম / সড়ক / বাসা নং (e.g. বাড়ি নং ১২, রোড নং ৪)

    // Legacy fields for backward compatibility
    private String location;
    private String address;

    private long createdAt;
    private boolean isGuest;

    public UserProfile() {
        // Required for Firestore deserialization
    }

    public UserProfile(String uid, String name, String email, String nid, String photoUrl,
                       String district, String thana, String unionWard, String postOffice, String villageRoad,
                       long createdAt, boolean isGuest) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.nid = nid;
        this.photoUrl = photoUrl;
        this.district = district;
        this.thana = thana;
        this.unionWard = unionWard;
        this.postOffice = postOffice;
        this.villageRoad = villageRoad;
        this.createdAt = createdAt;
        this.isGuest = isGuest;

        // Auto-populate legacy fields
        this.location = (!TextUtils.isEmpty(district) ? district : "") + (!TextUtils.isEmpty(thana) ? ", " + thana : "");
        this.address = getFormattedAddress();
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

    public String getThana() {
        return thana;
    }

    public void setThana(String thana) {
        this.thana = thana;
    }

    public String getUnionWard() {
        return unionWard;
    }

    public void setUnionWard(String unionWard) {
        this.unionWard = unionWard;
    }

    public String getPostOffice() {
        return postOffice;
    }

    public void setPostOffice(String postOffice) {
        this.postOffice = postOffice;
    }

    public String getVillageRoad() {
        return villageRoad;
    }

    public void setVillageRoad(String villageRoad) {
        this.villageRoad = villageRoad;
    }

    public String getLocation() {
        if (TextUtils.isEmpty(location)) {
            String loc = "";
            if (!TextUtils.isEmpty(district)) loc += district;
            if (!TextUtils.isEmpty(thana)) loc += (!TextUtils.isEmpty(loc) ? ", " : "") + thana;
            return loc;
        }
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        if (TextUtils.isEmpty(address)) {
            return getFormattedAddress();
        }
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

    public String getFormattedAddress() {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(villageRoad)) parts.add(villageRoad);
        if (!TextUtils.isEmpty(unionWard)) parts.add(unionWard);
        if (!TextUtils.isEmpty(postOffice)) parts.add(postOffice);
        if (!TextUtils.isEmpty(thana)) parts.add(thana);
        if (!TextUtils.isEmpty(district)) parts.add(district);
        return TextUtils.join(", ", parts);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid != null ? uid : "");
        map.put("name", name != null ? name : "");
        map.put("email", email != null ? email : "");
        map.put("nid", nid != null ? nid : "");
        map.put("photoUrl", photoUrl != null ? photoUrl : "");
        map.put("district", district != null ? district : "");
        map.put("thana", thana != null ? thana : "");
        map.put("unionWard", unionWard != null ? unionWard : "");
        map.put("postOffice", postOffice != null ? postOffice : "");
        map.put("villageRoad", villageRoad != null ? villageRoad : "");
        map.put("location", getLocation());
        map.put("address", getAddress());
        map.put("createdAt", createdAt);
        map.put("isGuest", isGuest);
        return map;
    }
}
