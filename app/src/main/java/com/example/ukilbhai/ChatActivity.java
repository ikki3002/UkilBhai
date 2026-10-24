package com.example.ukilbhai;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.chat.AiLegalClient;
import com.example.ukilbhai.chat.ChatAdapter;
import com.example.ukilbhai.chat.ChatMessage;
import com.example.ukilbhai.receiver.AirplaneModeReceiver;
import com.example.ukilbhai.receiver.NetworkConnectivityReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private EditText etUserPrompt;
    private FloatingActionButton btnSendMessage;
    private LinearLayout layoutTypingIndicator;
    private ImageButton btnBack;

    private TextView tvConnectionStatus;
    private View layoutOfflineBanner;
    private TextView tvOfflineBannerText;
    private ImageView ivOfflineBannerIcon;

    private NetworkConnectivityReceiver networkReceiver;
    private AirplaneModeReceiver airplaneModeReceiver;
    private boolean isNetworkConnected = true;
    private boolean isAirplaneModeOn = false;

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
        setupReceivers();

        // Welcome greeting message
        addInitialWelcomeMessage();
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etUserPrompt = findViewById(R.id.etUserPrompt);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        layoutTypingIndicator = findViewById(R.id.layoutTypingIndicator);
        btnBack = findViewById(R.id.btnBack);

        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        layoutOfflineBanner = findViewById(R.id.layoutOfflineBanner);
        tvOfflineBannerText = findViewById(R.id.tvOfflineBannerText);
        ivOfflineBannerIcon = findViewById(R.id.ivOfflineBannerIcon);
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

    private void setupReceivers() {
        networkReceiver = new NetworkConnectivityReceiver(isConnected -> {
            isNetworkConnected = isConnected;
            updateConnectivityUI();
        });

        airplaneModeReceiver = new AirplaneModeReceiver(isAirplaneMode -> {
            isAirplaneModeOn = isAirplaneMode;
            updateConnectivityUI();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check initial connectivity and airplane mode status
        isNetworkConnected = NetworkConnectivityReceiver.isNetworkAvailable(this);
        isAirplaneModeOn = AirplaneModeReceiver.isAirplaneModeOn(this);
        updateConnectivityUI();

        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(networkReceiver);
        } catch (IllegalArgumentException ignored) {}
        try {
            unregisterReceiver(airplaneModeReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void updateConnectivityUI() {
        runOnUiThread(() -> {
            if (isAirplaneModeOn) {
                if (tvConnectionStatus != null) {
                    tvConnectionStatus.setText("বিমান মোড চালু • অফলাইন");
                    tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
                }
                if (layoutOfflineBanner != null) {
                    layoutOfflineBanner.setVisibility(View.VISIBLE);
                    layoutOfflineBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.warning_amber_bg));
                }
                if (tvOfflineBannerText != null) {
                    tvOfflineBannerText.setText("বিমান মোড সক্রিয় রয়েছে। ইন্টারনেট সংযোগ বন্ধ।");
                    tvOfflineBannerText.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
                }
                if (ivOfflineBannerIcon != null) {
                    ivOfflineBannerIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning_amber));
                }
            } else if (!isNetworkConnected) {
                if (tvConnectionStatus != null) {
                    tvConnectionStatus.setText("অফলাইন • ইন্টারনেট সংযোগ বিচ্ছিন্ন");
                    tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.emergency_red_text));
                }
                if (layoutOfflineBanner != null) {
                    layoutOfflineBanner.setVisibility(View.VISIBLE);
                    layoutOfflineBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.emergency_red_bg));
                }
                if (tvOfflineBannerText != null) {
                    tvOfflineBannerText.setText("ইন্টারনেট সংযোগ পাওয়া যাচ্ছে না। অনুগ্রহ করে নেটওয়ার্ক চেক করুন।");
                    tvOfflineBannerText.setTextColor(ContextCompat.getColor(this, R.color.emergency_red_text));
                }
                if (ivOfflineBannerIcon != null) {
                    ivOfflineBannerIcon.setColorFilter(ContextCompat.getColor(this, R.color.emergency_red_text));
                }
            } else {
                if (tvConnectionStatus != null) {
                    tvConnectionStatus.setText("অনলাইন • তাৎক্ষণিক পরামর্শ");
                    tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.mint_accent));
                }
                if (layoutOfflineBanner != null) {
                    layoutOfflineBanner.setVisibility(View.GONE);
                }
            }
        });
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

        if (isAirplaneModeOn) {
            Toast.makeText(this, "বিমান মোড চালু থাকায় বার্তা পাঠানো সম্ভব নয়।", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkConnected) {
            Toast.makeText(this, "ইন্টারনেট সংযোগ নেই। অনুগ্রহ করে নেটওয়ার্ক চালু করুন।", Toast.LENGTH_SHORT).show();
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
