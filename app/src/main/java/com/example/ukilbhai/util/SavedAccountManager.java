package com.example.ukilbhai.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Facebook-style intelligent account suggestion & quick login manager.
 * Remembers logged-in identities on the device for 1-tap quick authentication.
 */
public class SavedAccountManager {

    private static final String PREF_NAME = "ukilbhai_saved_accounts_store";
    private static final String KEY_ACCOUNTS_JSON = "saved_accounts_list";

    public static class SavedAccount implements Serializable {
        public String email;
        public String name;
        public String role; // "citizen" or "lawyer"
        public String photoUrl;
        public String savedPassword;
        public long lastLoginTime;

        public SavedAccount(String email, String name, String role, String photoUrl, String savedPassword, long lastLoginTime) {
            this.email = email;
            this.name = name;
            this.role = role;
            this.photoUrl = photoUrl;
            this.savedPassword = savedPassword;
            this.lastLoginTime = lastLoginTime;
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void saveAccount(Context context, String email, String name, String role, String photoUrl, String password) {
        if (TextUtils.isEmpty(email)) return;

        List<SavedAccount> list = getAllSavedAccounts(context);
        List<SavedAccount> updated = new ArrayList<>();

        SavedAccount newAcc = new SavedAccount(
                email.trim(),
                !TextUtils.isEmpty(name) ? name : extractNameFromEmail(email),
                !TextUtils.isEmpty(role) ? role : "citizen",
                photoUrl != null ? photoUrl : "",
                password != null ? password : "",
                System.currentTimeMillis()
        );
        updated.add(newAcc);

        for (SavedAccount acc : list) {
            if (!acc.email.equalsIgnoreCase(email.trim())) {
                updated.add(acc);
            }
        }

        saveAll(context, updated);
    }

    public static synchronized SavedAccount getRecentAccount(Context context) {
        List<SavedAccount> list = getAllSavedAccounts(context);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public static synchronized List<SavedAccount> getAllSavedAccounts(Context context) {
        List<SavedAccount> result = new ArrayList<>();
        String json = getPrefs(context).getString(KEY_ACCOUNTS_JSON, null);
        if (!TextUtils.isEmpty(json)) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    result.add(new SavedAccount(
                            obj.optString("email", ""),
                            obj.optString("name", ""),
                            obj.optString("role", "citizen"),
                            obj.optString("photoUrl", ""),
                            obj.optString("savedPassword", ""),
                            obj.optLong("lastLoginTime", 0L)
                    ));
                }
            } catch (Exception e) {
                Log.e("SavedAccountManager", "Error parsing accounts", e);
            }
        }
        return result;
    }

    public static synchronized List<String> getAllSavedEmails(Context context) {
        List<String> emails = new ArrayList<>();
        for (SavedAccount acc : getAllSavedAccounts(context)) {
            if (!TextUtils.isEmpty(acc.email) && !emails.contains(acc.email)) {
                emails.add(acc.email);
            }
        }
        return emails;
    }

    public static synchronized void removeAccount(Context context, String email) {
        if (TextUtils.isEmpty(email)) return;
        List<SavedAccount> list = getAllSavedAccounts(context);
        List<SavedAccount> updated = new ArrayList<>();
        for (SavedAccount acc : list) {
            if (!acc.email.equalsIgnoreCase(email.trim())) {
                updated.add(acc);
            }
        }
        saveAll(context, updated);
    }

    private static void saveAll(Context context, List<SavedAccount> accounts) {
        try {
            JSONArray arr = new JSONArray();
            for (SavedAccount acc : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("email", acc.email);
                obj.put("name", acc.name);
                obj.put("role", acc.role);
                obj.put("photoUrl", acc.photoUrl);
                obj.put("savedPassword", acc.savedPassword);
                obj.put("lastLoginTime", acc.lastLoginTime);
                arr.put(obj);
            }
            getPrefs(context).edit().putString(KEY_ACCOUNTS_JSON, arr.toString()).apply();
        } catch (Exception e) {
            Log.e("SavedAccountManager", "Error saving accounts", e);
        }
    }

    private static String extractNameFromEmail(String email) {
        if (email.contains("@")) {
            String prefix = email.split("@")[0];
            return prefix.substring(0, 1).toUpperCase() + prefix.substring(1);
        }
        return "ইউজার";
    }
}
