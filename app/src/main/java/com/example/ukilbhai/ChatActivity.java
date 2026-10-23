package com.example.ukilbhai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.chat.AiLegalClient;
import com.example.ukilbhai.chat.ChatAdapter;
import com.example.ukilbhai.chat.ChatMessage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private EditText etUserPrompt;
    private FloatingActionButton btnSendMessage;
    private LinearLayout layoutTypingIndicator;
    private ImageButton btnBack;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final AiLegalClient aiLegalClient = new AiLegalClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupRecyclerView();
        setupListeners();

        // Welcome greeting message
        addInitialWelcomeMessage();
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etUserPrompt = findViewById(R.id.etUserPrompt);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        layoutTypingIndicator = findViewById(R.id.layoutTypingIndicator);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSendMessage.setOnClickListener(v -> handleSendMessage());
    }

    private void addInitialWelcomeMessage() {
        String welcome = "আসসালামু আলাইকুম! আমি 'উকিল ভাই' এআই। আপনার কি ধরনের আইনি পরামর্শ বা তথ্য প্রয়োজন? দয়া করে আপনার সমস্যাটি লিখুন।";
        messageList.add(new ChatMessage(welcome, ChatMessage.SENDER_AI));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
    }

    private void handleSendMessage() {
        String prompt = etUserPrompt.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            return;
        }

        // 1. Add user message
        messageList.add(new ChatMessage(prompt, ChatMessage.SENDER_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.smoothScrollToPosition(messageList.size() - 1);

        etUserPrompt.setText("");
        setTyping(true);

        // 2. Call unified AiLegalClient (Google Gemini 1.5 Flash -> Fallback to OpenRouter Meta/Qwen/Gemma)
        aiLegalClient.sendMessage(messageList, prompt, new AiLegalClient.ResponseCallback() {
            @Override
            public void onSuccess(String reply, String providerUsed) {
                setTyping(false);
                messageList.add(new ChatMessage(reply, ChatMessage.SENDER_AI));
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onError(String errorMessage) {
                setTyping(false);
                messageList.add(new ChatMessage("ত্রুটি: " + errorMessage, ChatMessage.SENDER_AI, true));
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setTyping(boolean isTyping) {
        layoutTypingIndicator.setVisibility(isTyping ? View.VISIBLE : View.GONE);
        btnSendMessage.setEnabled(!isTyping);
        if (isTyping) {
            rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }
}
