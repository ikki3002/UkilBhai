package com.example.ukilbhai.chat;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {
    public static final int SENDER_USER = 1;
    public static final int SENDER_AI = 2;

    private String text;
    private int sender;
    private long timestamp;
    private boolean isError;

    public ChatMessage(String text, int sender) {
        this.text = text;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
        this.isError = false;
    }

    public ChatMessage(String text, int sender, boolean isError) {
        this.text = text;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
        this.isError = isError;
    }

    public ChatMessage(String text, int sender, long timestamp, boolean isError) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
        this.isError = isError;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("text", text);
            json.put("sender", sender);
            json.put("timestamp", timestamp);
            json.put("isError", isError);
        } catch (JSONException ignored) {}
        return json;
    }

    public static ChatMessage fromJson(JSONObject json) {
        if (json == null) return null;
        String text = json.optString("text", "");
        int sender = json.optInt("sender", SENDER_USER);
        long timestamp = json.optLong("timestamp", System.currentTimeMillis());
        boolean isError = json.optBoolean("isError", false);
        return new ChatMessage(text, sender, timestamp, isError);
    }
}
