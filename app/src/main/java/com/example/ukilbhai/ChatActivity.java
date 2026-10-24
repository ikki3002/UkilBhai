package com.example.ukilbhai;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.chat.AiLegalClient;
import com.example.ukilbhai.chat.ChatAdapter;
import com.example.ukilbhai.chat.ChatMessage;
import com.example.ukilbhai.receiver.AirplaneModeReceiver;
import com.example.ukilbhai.receiver.NetworkConnectivityReceiver;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2001;

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private View btnDrawerSettings;
    private View btnNewChat;

    private RecyclerView rvChatMessages;
    private EditText etUserPrompt;
    private MaterialCardView cardGeminiDock;
    private ImageButton btnAddAttachment;
    private ImageButton btnVoiceInput;
    private View viewMicPulse;
    private MaterialCardView cardActionButton;
    private ImageView ivActionIcon;
    private LinearLayout layoutTypingIndicator;

    private View layoutAttachmentBar;
    private TextView tvAttachmentTitle;
    private ImageButton btnRemoveAttachment;
    private String currentAttachmentContext = null;

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
    private AnimatorSet micPulseAnimatorSet;
    private boolean isShowingSendButton = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
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
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        btnDrawerSettings = findViewById(R.id.btnDrawerSettings);
        btnNewChat = findViewById(R.id.btnNewChat);

        rvChatMessages = findViewById(R.id.rvChatMessages);
        etUserPrompt = findViewById(R.id.etUserPrompt);
        cardGeminiDock = findViewById(R.id.cardGeminiDock);
        btnAddAttachment = findViewById(R.id.btnAddAttachment);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        viewMicPulse = findViewById(R.id.viewMicPulse);
        cardActionButton = findViewById(R.id.cardActionButton);
        ivActionIcon = findViewById(R.id.ivActionIcon);
        layoutTypingIndicator = findViewById(R.id.layoutTypingIndicator);

        layoutAttachmentBar = findViewById(R.id.layoutAttachmentBar);
        tvAttachmentTitle = findViewById(R.id.tvAttachmentTitle);
        btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);

        layoutOfflineBanner = findViewById(R.id.layoutOfflineBanner);
        tvOfflineBannerText = findViewById(R.id.tvOfflineBannerText);
        ivOfflineBannerIcon = findViewById(R.id.ivOfflineBannerIcon);
    }

    private void setupSpeechRecognizer() {
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    stopMicPulse();
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                startNewChat();
            });
        }

        if (btnDrawerSettings != null) {
            btnDrawerSettings.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                showSettingsDialog();
            });
        }

        // Micro-interaction: active:scale-95 with haptic feedback
        applyPressScale(btnAddAttachment);
        applyPressScale(btnVoiceInput);
        applyPressScale(cardActionButton);

        // Auto-expanding input: dynamic corner radius & seamless Send icon transition
        etUserPrompt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !TextUtils.isEmpty(s.toString().trim());
                updateActionButtonState(hasText, true);

                // Dynamically adjust corner radius between rounded-full (28dp) and rounded-2xl (20dp)
                int lineCount = etUserPrompt.getLineCount();
                float targetRadius = dpToPx(lineCount > 1 ? 20 : 28);
                if (cardGeminiDock != null && cardGeminiDock.getRadius() != targetRadius) {
                    cardGeminiDock.setRadius(targetRadius);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Pill Action Button: Sends message if text exists, otherwise triggers Voice-to-Text
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

        // Plus / Add legal quick templates
        btnAddAttachment.setOnClickListener(v -> showQuickLegalTopicsDialog());

        // Remove active attachment
        btnRemoveAttachment.setOnClickListener(v -> removeAttachment());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyPressScale(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).start();
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void updateActionButtonState(boolean hasText, boolean animated) {
        if (isShowingSendButton == hasText) return;
        isShowingSendButton = hasText;

        if (animated && ivActionIcon != null) {
            ivActionIcon.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(90).withEndAction(() -> {
                applyButtonVisuals(hasText);
                ivActionIcon.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(110).start();
            }).start();
        } else {
            applyButtonVisuals(hasText);
        }
    }

    private void applyButtonVisuals(boolean hasText) {
        if (hasText) {
            cardActionButton.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_emerald));
            ivActionIcon.setImageResource(R.drawable.ic_send);
            ivActionIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
            cardActionButton.setContentDescription("পাঠান");
        } else {
            cardActionButton.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gemini_live_pill_bg));
            ivActionIcon.setImageResource(R.drawable.ic_waveform);
            ivActionIcon.setColorFilter(ContextCompat.getColor(this, R.color.gemini_live_bars));
            cardActionButton.setContentDescription("ভয়েস মোড");
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

        startMicPulse();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "আপনার আইনি সমস্যাটি বলুন...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            stopMicPulse();
            Toast.makeText(this, "আপনার ডিভাইসে ভয়েস রিকগনিশন সুবিধা পাওয়া যায়নি।", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMicPulse() {
        if (viewMicPulse == null) return;
        viewMicPulse.setVisibility(View.VISIBLE);
        btnVoiceInput.setColorFilter(ContextCompat.getColor(this, R.color.gemini_mic_pulse));

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(viewMicPulse, "scaleX", 1f, 1.35f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(viewMicPulse, "scaleY", 1f, 1.35f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(viewMicPulse, "alpha", 0.7f, 0.1f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        alpha.setRepeatMode(ValueAnimator.REVERSE);

        micPulseAnimatorSet = new AnimatorSet();
        micPulseAnimatorSet.playTogether(scaleX, scaleY, alpha);
        micPulseAnimatorSet.setDuration(600);
        micPulseAnimatorSet.start();
    }

    private void stopMicPulse() {
        if (micPulseAnimatorSet != null) {
            micPulseAnimatorSet.cancel();
            micPulseAnimatorSet = null;
        }
        if (viewMicPulse != null) {
            viewMicPulse.setVisibility(View.GONE);
            viewMicPulse.setScaleX(1f);
            viewMicPulse.setScaleY(1f);
            viewMicPulse.setAlpha(1f);
        }
        if (btnVoiceInput != null) {
            btnVoiceInput.setColorFilter(ContextCompat.getColor(this, R.color.gemini_icon_tint));
        }
    }

    private void showQuickLegalTopicsDialog() {
        String[] topics = new String[]{
                "জিডি (সাধারণ ডায়েরি) করার নিয়ম ও ড্রাফট ফরম্যাট",
                "দেনমোহর ও তালাক সংক্রান্ত আইনি অধিকার",
                "বাড়িভাড়া আইন ও উচ্ছেদ নোটিশ বিধিমালা",
                "মোটরযান আইন ও ট্রাফিক মামলার জরিমানা তালিকা",
                "চেক ডিজঅনার (এনআই এক্ট ১৩৮ ধারা) মামলা"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_UkilBhai);
        builder.setTitle("দ্রুত আইনি বিষয় ও সংযুক্তি নির্বাচন করুন");
        builder.setItems(topics, (dialog, which) -> {
            currentAttachmentContext = topics[which];
            if (layoutAttachmentBar != null && tvAttachmentTitle != null) {
                tvAttachmentTitle.setText(currentAttachmentContext);
                layoutAttachmentBar.setVisibility(View.VISIBLE);
            }
        });
        builder.setNegativeButton("বাতিল", null);
        builder.show();
    }

    private void removeAttachment() {
        currentAttachmentContext = null;
        if (layoutAttachmentBar != null) {
            layoutAttachmentBar.setVisibility(View.GONE);
        }
    }

    private void startNewChat() {
        messageList.clear();
        addInitialWelcomeMessage();
        chatAdapter.notifyDataSetChanged();
        Toast.makeText(this, "নতুন আইনি জিজ্ঞাসা শুরু হয়েছে", Toast.LENGTH_SHORT).show();
    }

    private void showSettingsDialog() {
        int currentTheme = ThemeHelper.getSavedTheme(this);
        String currentThemeName = ThemeHelper.getThemeDisplayName(this, currentTheme);

        String[] settingsItems = new String[]{"থিম (Theme): " + currentThemeName};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_UkilBhai);
        builder.setTitle("সেটিংস (Settings)");
        builder.setItems(settingsItems, (dialog, which) -> {
            if (which == 0) {
                showThemeSelectionModal();
            }
        });
        builder.setNegativeButton("বন্ধ করুন (Close)", null);
        builder.show();
    }

    private void showThemeSelectionModal() {
        String[] themeOptions = new String[]{"Light (লাইট)", "Dark (ডার্ক)", "System Default (সিস্টেম ডিফল্ট)"};
        int currentTheme = ThemeHelper.getSavedTheme(this);
        int checkedIndex = 1; // Default dark
        if (currentTheme == ThemeHelper.THEME_LIGHT) {
            checkedIndex = 0;
        } else if (currentTheme == ThemeHelper.THEME_DARK) {
            checkedIndex = 1;
        } else if (currentTheme == ThemeHelper.THEME_SYSTEM) {
            checkedIndex = 2;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_UkilBhai);
        builder.setTitle("Choose Theme (থিম নির্বাচন করুন)");
        builder.setSingleChoiceItems(themeOptions, checkedIndex, (dialog, which) -> {
            int newThemeMode = ThemeHelper.THEME_SYSTEM;
            if (which == 0) {
                newThemeMode = ThemeHelper.THEME_LIGHT;
            } else if (which == 1) {
                newThemeMode = ThemeHelper.THEME_DARK;
            } else if (which == 2) {
                newThemeMode = ThemeHelper.THEME_SYSTEM;
            }

            ThemeHelper.saveTheme(ChatActivity.this, newThemeMode);
            dialog.dismiss();
            Toast.makeText(ChatActivity.this, "থিম পরিবর্তিত হয়েছে", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("বাতিল (Cancel)", null);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
        stopMicPulse();
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

        // Attach legal context if selected
        String finalPrompt = prompt;
        if (!TextUtils.isEmpty(currentAttachmentContext)) {
            finalPrompt = "[সংযুক্তি/আইনি বিষয়: " + currentAttachmentContext + "]\n" + prompt;
        }

        // 1. Add user message
        messageList.add(new ChatMessage(finalPrompt, ChatMessage.SENDER_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.smoothScrollToPosition(messageList.size() - 1);

        etUserPrompt.setText("");
        removeAttachment();
        setTyping(true);

        // 2. Call unified AiLegalClient
        aiLegalClient.sendMessage(messageList, finalPrompt, new AiLegalClient.ResponseCallback() {
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

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
