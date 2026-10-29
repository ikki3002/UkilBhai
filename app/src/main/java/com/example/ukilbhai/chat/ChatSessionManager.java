package com.example.ukilbhai.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatSessionManager {
    private static final String PREF_NAME = "ukilbhai_chat_sessions";
    private static final String KEY_SESSION_IDS = "session_ids";
    private static final String KEY_ACTIVE_SESSION_ID = "active_session_id";
    private static final String PREFIX_SESSION = "session_";

    private final SharedPreferences prefs;

    public ChatSessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<ChatSession> getAllSessions() {
        List<String> ids = getSessionIds();
        List<ChatSession> sessions = new ArrayList<>();

        for (String id : ids) {
            ChatSession session = getSession(id);
            if (session != null) {
                sessions.add(session);
            }
        }

        // Sort: Pinned first, then by updatedAt descending
        Collections.sort(sessions, (s1, s2) -> {
            if (s1.isPinned() != s2.isPinned()) {
                return s1.isPinned() ? -1 : 1;
            }
            return Long.compare(s2.getUpdatedAt(), s1.getUpdatedAt());
        });

        return sessions;
    }

    public synchronized ChatSession getSession(String id) {
        if (TextUtils.isEmpty(id)) return null;
        String jsonStr = prefs.getString(PREFIX_SESSION + id, null);
        if (jsonStr == null) return null;
        try {
            return ChatSession.fromJson(new JSONObject(jsonStr));
        } catch (JSONException e) {
            return null;
        }
    }

    public synchronized void saveSession(ChatSession session) {
        if (session == null || TextUtils.isEmpty(session.getId())) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFIX_SESSION + session.getId(), session.toJson().toString());

        List<String> ids = getSessionIds();
        if (!ids.contains(session.getId())) {
            ids.add(0, session.getId());
            saveSessionIds(ids, editor);
        }
        editor.apply();
    }

    public synchronized void deleteSession(String id) {
        if (TextUtils.isEmpty(id)) return;
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREFIX_SESSION + id);

        List<String> ids = getSessionIds();
        if (ids.remove(id)) {
            saveSessionIds(ids, editor);
        }

        // If the active session is deleted, clear active
        if (id.equals(getActiveSessionId())) {
            editor.remove(KEY_ACTIVE_SESSION_ID);
        }
        editor.apply();
    }

    public synchronized boolean togglePin(String id) {
        ChatSession session = getSession(id);
        if (session != null) {
            session.setPinned(!session.isPinned());
            saveSession(session);
            return session.isPinned();
        }
        return false;
    }

    public synchronized List<ChatSession> searchSessions(String query) {
        if (TextUtils.isEmpty(query)) {
            return getAllSessions();
        }
        String lowerQuery = query.toLowerCase().trim();
        List<ChatSession> all = getAllSessions();
        List<ChatSession> filtered = new ArrayList<>();

        for (ChatSession s : all) {
            if (s.getTitle() != null && s.getTitle().toLowerCase().contains(lowerQuery)) {
                filtered.add(s);
                continue;
            }
            // Check messages
            for (ChatMessage msg : s.getMessages()) {
                if (msg.getText() != null && msg.getText().toLowerCase().contains(lowerQuery)) {
                    filtered.add(s);
                    break;
                }
            }
        }
        return filtered;
    }

    public synchronized String getActiveSessionId() {
        return prefs.getString(KEY_ACTIVE_SESSION_ID, null);
    }

    public synchronized void setActiveSessionId(String id) {
        prefs.edit().putString(KEY_ACTIVE_SESSION_ID, id).apply();
    }

    private List<String> getSessionIds() {
        String raw = prefs.getString(KEY_SESSION_IDS, "[]");
        List<String> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    private void saveSessionIds(List<String> ids, SharedPreferences.Editor editor) {
        JSONArray array = new JSONArray();
        for (String id : ids) {
            array.put(id);
        }
        editor.putString(KEY_SESSION_IDS, array.toString());
    }
}
