package com.example.ukilbhai.rag;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages bookmarked statutory sections locally (0ms offline access)
 * and syncs with Cloud Firestore (/users/{uid}/bookmarked_laws).
 */
public class BookmarkManager {

    private static final String PREF_NAME = "ukilbhai_bookmarked_laws";
    private static final String KEY_BOOKMARKS = "bookmarks_json";

    public static String getSectionUniqueKey(LegalSection section) {
        if (section == null) return "";
        String raw = section.getCleanActTitle() + "|" + section.getDisplayTitle();
        return String.valueOf(raw.hashCode());
    }

    public static List<LegalSection> getBookmarkedSections(Context context) {
        List<LegalSection> list = new ArrayList<>();
        if (context == null) return list;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_BOOKMARKS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new LegalSection(
                        obj.optString("actTitle", ""),
                        obj.optString("actYear", ""),
                        obj.optString("sectionTitle", ""),
                        obj.optString("sectionContent", "")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static boolean isBookmarked(Context context, LegalSection target) {
        if (context == null || target == null) return false;
        String targetKey = getSectionUniqueKey(target);
        for (LegalSection item : getBookmarkedSections(context)) {
            if (targetKey.equals(getSectionUniqueKey(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles bookmark state. Returns true if added, false if removed.
     */
    public static boolean toggleBookmark(Context context, LegalSection target) {
        if (context == null || target == null) return false;
        List<LegalSection> current = getBookmarkedSections(context);
        String targetKey = getSectionUniqueKey(target);
        boolean removed = false;

        for (int i = 0; i < current.size(); i++) {
            if (targetKey.equals(getSectionUniqueKey(current.get(i)))) {
                current.remove(i);
                removed = true;
                break;
            }
        }

        if (!removed) {
            current.add(0, target);
        }

        saveBookmarksList(context, current);
        syncWithFirestore(target, !removed);
        return !removed;
    }

    private static void saveBookmarksList(Context context, List<LegalSection> list) {
        try {
            JSONArray arr = new JSONArray();
            for (LegalSection s : list) {
                JSONObject obj = new JSONObject();
                obj.put("actTitle", s.getActTitle());
                obj.put("actYear", s.getActYear());
                obj.put("sectionTitle", s.getSectionTitle());
                obj.put("sectionContent", s.getSectionContent());
                arr.put(obj);
            }
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_BOOKMARKS, arr.toString())
                    .apply();
        } catch (Exception ignored) {}
    }

    private static void syncWithFirestore(LegalSection section, boolean added) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || TextUtils.isEmpty(user.getUid())) return;
            String docId = "sec_" + Math.abs(getSectionUniqueKey(section).hashCode());
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (added) {
                Map<String, Object> data = new HashMap<>();
                data.put("actTitle", section.getCleanActTitle());
                data.put("actYear", section.getActYear());
                data.put("sectionTitle", section.getDisplayTitle());
                data.put("sectionContent", section.getSectionContent());
                data.put("savedAt", System.currentTimeMillis());
                db.collection("users")
                        .document(user.getUid())
                        .collection("bookmarked_laws")
                        .document(docId)
                        .set(data);
            } else {
                db.collection("users")
                        .document(user.getUid())
                        .collection("bookmarked_laws")
                        .document(docId)
                        .delete();
            }
        } catch (Exception ignored) {}
    }
}
