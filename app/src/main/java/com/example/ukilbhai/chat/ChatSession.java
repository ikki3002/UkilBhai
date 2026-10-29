package com.example.ukilbhai.chat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatSession {
    private String id;
    private String title;
    private long createdAt;
    private long updatedAt;
    private boolean isPinned;
    private final List<ChatMessage> messages = new ArrayList<>();

    public ChatSession() {
        this.id = UUID.randomUUID().toString();
        this.title = "নতুন আইনি পরামর্শ";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPinned = false;
    }

    public ChatSession(String id, String title, long createdAt, long updatedAt, boolean isPinned) {
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPinned = isPinned;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            messages.add(message);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    /**
     * Intelligently derives a proper topic title from the user prompt.
     */
    public static String deriveTitle(String rawPrompt, boolean isBengali) {
        if (rawPrompt == null || rawPrompt.trim().isEmpty()) {
            return isBengali ? "নতুন আইনি পরামর্শ" : "New Consultation";
        }

        String cleaned = rawPrompt.trim();

        // Check if an attachment header was prepended (e.g., [সংযুক্তি/আইনি বিষয়: ...])
        Pattern pattern = Pattern.compile("^\\[(?:সংযুক্তি/আইনি বিষয়|Attachment/Legal Topic):\\s*([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cleaned);
        if (matcher.find()) {
            String topic = matcher.group(1);
            if (topic != null && !topic.trim().isEmpty()) {
                return topic.trim();
            }
            cleaned = pattern.matcher(cleaned).replaceFirst("").trim();
        }

        // Clean newlines and extra spaces
        cleaned = cleaned.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();

        if (cleaned.length() <= 32) {
            return cleaned;
        }

        // Cut at word boundary near 32 chars
        int cutIndex = 32;
        int spaceIndex = cleaned.lastIndexOf(' ', cutIndex);
        if (spaceIndex > 15) {
            cutIndex = spaceIndex;
        }
        return cleaned.substring(0, cutIndex) + "...";
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("isPinned", isPinned);

            JSONArray msgArray = new JSONArray();
            for (ChatMessage msg : messages) {
                msgArray.put(msg.toJson());
            }
            json.put("messages", msgArray);
        } catch (JSONException ignored) {}
        return json;
    }

    public static ChatSession fromJson(JSONObject json) {
        if (json == null) return null;
        String id = json.optString("id", UUID.randomUUID().toString());
        String title = json.optString("title", "আইনি পরামর্শ");
        long createdAt = json.optLong("createdAt", System.currentTimeMillis());
        long updatedAt = json.optLong("updatedAt", System.currentTimeMillis());
        boolean isPinned = json.optBoolean("isPinned", false);

        ChatSession session = new ChatSession(id, title, createdAt, updatedAt, isPinned);
        JSONArray msgArray = json.optJSONArray("messages");
        if (msgArray != null) {
            for (int i = 0; i < msgArray.length(); i++) {
                JSONObject msgObj = msgArray.optJSONObject(i);
                if (msgObj != null) {
                    ChatMessage msg = ChatMessage.fromJson(msgObj);
                    if (msg != null) {
                        session.messages.add(msg);
                    }
                }
            }
        }
        return session;
    }
}
