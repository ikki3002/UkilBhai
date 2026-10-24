package com.example.ukilbhai;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.chat.AiLegalClient;
import com.example.ukilbhai.chat.ChatAdapter;
import com.example.ukilbhai.chat.ChatMessage;
import com.example.ukilbhai.receiver.AirplaneModeReceiver;
import com.example.ukilbhai.receiver.NetworkConnectivityReceiver;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2001;

    private RecyclerView rvChatMessages;
    private EditText etUserPrompt;
    private ImageButton btnAddAttachment;
    private ImageButton btnVoiceInput;
    private MaterialCardView cardActionButton;
    private ImageView ivActionIcon;
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

    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupSpeechRecognizer();
        setupRecyclerView();
        setupListeners();
        setupReceivers();

        // Welcome greeting message
        addInitialWelcomeMessage();
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etUserPrompt = findViewById(R.id.etUserPrompt);
        btnAddAttachment = findViewById(R.id.btnAddAttachment);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        cardActionButton = findViewById(R.id.cardActionButton);
        ivActionIcon = findViewById(R.id.ivActionIcon);
        layoutTypingIndicator = findViewById(R.id.layoutTypingIndicator);
        btnBack = findViewById(R.id.btnBack);

        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        layoutOfflineBanner = findViewById(R.id.layoutOfflineBanner);
        tvOfflineBannerText = findViewById(R.id.tvOfflineBannerText);
        ivOfflineBannerIcon = findViewById(R.id.ivOfflineBannerIcon);
    }

    private void setupSpeechRecognizer() {
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            String existing = etUserPrompt.getText().toString().trim();
                            if (!TextUtils.isEmpty(existing)) {
                                etUserPrompt.setText(existing + " " + spokenText);
                            } else {
                                etUserPrompt.setText(spokenText);
                            }
                            etUserPrompt.setSelection(etUserPrompt.getText().length());
                        }
                    }
                }
        );
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

        // Dynamic transition between Gemini Live Waveform and Send action
        etUserPrompt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !TextUtils.isEmpty(s.toString().trim());
                updateActionButtonState(hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Pill Action Button: Sends message if text exists, otherwise starts Voice Input
        cardActionButton.setOnClickListener(v -> {
            String prompt = etUserPrompt.getText().toString().trim();
            if (!TextUtils.isEmpty(prompt)) {
                handleSendMessage();
            } else {
                startVoiceInput();
            }
        });

        // Dedicated Voice Input (Microphone button)
        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        // Quick Legal Templates (+)
        btnAddAttachment.setOnClickListener(v -> showQuickLegalTopicsDialog());
    }

    private void updateActionButtonState(boolean hasText) {
        if (hasText) {
            cardActionButton.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_emerald));
            ivActionIcon.setImageResource(R.drawable.ic_send);
            ivActionIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
            cardActionButton.setContentDescription("পাঠান");
        } else {
            cardActionButton.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gemini_live_pill_bg));
            ivActionIcon.setImageResource(R.drawable.ic_waveform);
            ivActionIcon.setColorFilter(ContextCompat.getColor(this, R.color.gemini_live_bars));
            cardActionButton.setContentDescription("ভয়েস ইনপুট");
        }
    }

    private void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "আপনার আইনি সমস্যাটি বলুন...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "আপনার ডিভাইসে ভয়েস রিকগনিশন সুবিধা পাওয়া যায়নি।", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQuickLegalTopicsDialog() {
        String[] topics = new String[]{
                "জিডি (সাধারণ ডায়েরি) করার নিয়ম ও নমুনা ড্রাফট",
                "দেনমোহর ও তালাক সংক্রান্ত আইনি অধিকার",
                "বাড়িভাড়া আইন ও উচ্ছেদ নোটিশ বিধিমালা",
                "মোটরযান আইন ও ট্রাফিক মামলার জরিমানা তালিকা",
                "চেক ডিজঅনার (এনআই এক্ট ১৩৮ ধারা) সংক্রান্ত মামলা"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_UkilBhai);
        builder.setTitle("দ্রুত আইনি বিষয় নির্বাচন করুন");
        builder.setItems(topics, (dialog, which) -> {
            etUserPrompt.setText(topics[which]);
            etUserPrompt.setSelection(etUserPrompt.getText().length());
        });
        builder.setNegativeButton("বাতিল", null);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, "ভয়েস ইনপুটের জন্য অডিও রেকর্ডিং পারমিশন প্রয়োজন।", Toast.LENGTH_SHORT).show();
            }
        }
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
        cardActionButton.setEnabled(!isTyping);
        btnVoiceInput.setEnabled(!isTyping);
        btnAddAttachment.setEnabled(!isTyping);
        if (isTyping) {
            rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }
}
