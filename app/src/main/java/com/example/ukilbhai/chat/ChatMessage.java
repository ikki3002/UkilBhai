package com.example.ukilbhai.chat;

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
}
