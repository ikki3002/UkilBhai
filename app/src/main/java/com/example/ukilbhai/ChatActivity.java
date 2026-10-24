package com.example.ukilbhai;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.SettingsDialogHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private ImageButton btnChatMore;
    private View btnDrawerSettings;
    private View btnNewChat;
    private TextView tvChatTitle;
    private TextView tvDrawerTitle;
    private TextView tvDrawerSubtitle;
    private TextView tvBtnNewChat;
    private TextView tvRecentLabel;
    private TextView tvDrawerSettings;

    private RecyclerView rvChatMessages;
    private EditText etUserPrompt;
    private MaterialCardView cardGeminiDock;
    private ImageButton btnAddAttachment;
    private ImageButton btnVoiceInput;
    private View viewMicPulse;
    private MaterialCardView cardActionButton;
    private ImageView ivActionIcon;
    private View layoutTypingIndicator;

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

    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        initTextToSpeech();
        setupSpeechRecognizer();
        setupRecyclerView();
        setupListeners();
        setupReceivers();
        updateLanguageUI();

        // Welcome greeting message
        addInitialWelcomeMessage();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        btnChatMore = findViewById(R.id.btnChatMore);
        btnDrawerSettings = findViewById(R.id.btnDrawerSettings);
        btnNewChat = findViewById(R.id.btnNewChat);

        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvDrawerTitle = findViewById(R.id.tvDrawerTitle);
        tvDrawerSubtitle = findViewById(R.id.tvDrawerSubtitle);
        tvBtnNewChat = findViewById(R.id.tvBtnNewChat);
        tvRecentLabel = findViewById(R.id.tvRecentLabel);
        tvDrawerSettings = findViewById(R.id.tvDrawerSettings);

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

    private void initTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true;
                applyTtsLanguage();
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            if (chatAdapter != null) {
                                chatAdapter.setCurrentlySpeakingPosition(-1);
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            if (chatAdapter != null) {
                                chatAdapter.setCurrentlySpeakingPosition(-1);
                            }
                        });
                    }
                });
            }
        });
    }

    private void applyTtsLanguage() {
        if (tts == null || !isTtsInitialized) return;
        boolean isBn = LanguageHelper.isBengali(this);
        Locale targetLocale = isBn ? new Locale("bn", "BD") : Locale.US;
        int result = tts.setLanguage(targetLocale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (isBn) {
                tts.setLanguage(new Locale("bn"));
            }
        }
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
        chatAdapter.setOnChatActionListener(new ChatAdapter.OnChatActionListener() {
            @Override
            public void onReadAloud(int position, String text) {
                handleReadAloud(position, text);
            }

            @Override
            public void onCopy(int position, String text) {
                handleCopyAiText(text);
            }
        });
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

        if (btnChatMore != null) {
            btnChatMore.setOnClickListener(v -> {
                SettingsDialogHelper.showSettingsDialog(this, this::recreate);
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
                SettingsDialogHelper.showSettingsDialog(this, this::recreate);
            });
        }

        // Micro-interaction: active:scale-95 with haptic feedback
        applyPressScale(btnAddAttachment);
        applyPressScale(btnVoiceInput);
        applyPressScale(cardActionButton);

        // Auto-expanding input: dynamic corner radius & toggle Send button
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

        // Pill Action Button: Sends message (visible only when typing)
        cardActionButton.setOnClickListener(v -> {
            String prompt = etUserPrompt.getText().toString().trim();
            if (!TextUtils.isEmpty(prompt)) {
                handleSendMessage();
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

        boolean isBn = LanguageHelper.isBengali(this);
        if (hasText) {
            cardActionButton.setVisibility(View.VISIBLE);
            cardActionButton.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_emerald));
            ivActionIcon.setImageResource(R.drawable.ic_send);
            ivActionIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
            cardActionButton.setContentDescription(isBn ? "পাঠান" : "Send");
            if (animated) {
                cardActionButton.setAlpha(0f);
                cardActionButton.setScaleX(0.7f);
                cardActionButton.setScaleY(0.7f);
                cardActionButton.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start();
            } else {
                cardActionButton.setAlpha(1f);
                cardActionButton.setScaleX(1f);
                cardActionButton.setScaleY(1f);
            }
        } else {
            if (animated) {
                cardActionButton.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(100).withEndAction(() -> {
                    cardActionButton.setVisibility(View.GONE);
                }).start();
            } else {
                cardActionButton.setVisibility(View.GONE);
            }
        }
    }

    private void updateLanguageUI() {
        boolean isBn = LanguageHelper.isBengali(this);

        if (tvChatTitle != null) {
            tvChatTitle.setText(isBn ? "উকিল ভাই" : "Ukil Bhai");
        }
        if (etUserPrompt != null) {
            etUserPrompt.setHint(isBn ? "আইনি সমস্যা বা প্রশ্ন লিখুন..." : "Ask legal questions or advice...");
        }
        if (tvDrawerTitle != null) {
            tvDrawerTitle.setText(isBn ? "উকিল ভাই" : "Ukil Bhai");
        }
        if (tvDrawerSubtitle != null) {
            tvDrawerSubtitle.setText(isBn ? "আপনার ডিজিটাল আইনি সহায়ক" : "Your Digital Legal Assistant");
        }
        if (tvBtnNewChat != null) {
            tvBtnNewChat.setText(isBn ? "নতুন আইনি জিজ্ঞাসা" : "New Legal Inquiry");
        }
        if (tvRecentLabel != null) {
            tvRecentLabel.setText(isBn ? "সাম্প্রতিক পরামর্শ" : "Recent Consultations");
        }
        if (tvDrawerSettings != null) {
            tvDrawerSettings.setText(isBn ? "সেটিংস" : "Settings");
        }
        if (btnVoiceInput != null) {
            btnVoiceInput.setContentDescription(isBn ? "ভয়েস ইনপুট" : "Voice Input");
        }
        if (cardActionButton != null) {
            cardActionButton.setContentDescription(isBn ? "পাঠান" : "Send");
        }

        applyTtsLanguage();
        updateConnectivityUI();
    }

    private void handleReadAloud(int position, String rawText) {
        if (!isTtsInitialized || tts == null) {
            boolean isBn = LanguageHelper.isBengali(this);
            Toast.makeText(this, isBn
                    ? "ভয়েস রিডিং ইঞ্জিন প্রস্তুত হচ্ছে..."
                    : "Text-to-speech engine initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Toggle: if currently reading this position, stop it
        if (chatAdapter != null && chatAdapter.getCurrentlySpeakingPosition() == position) {
            tts.stop();
            chatAdapter.setCurrentlySpeakingPosition(-1);
            return;
        }

        tts.stop();
        applyTtsLanguage();

        String speechText = stripMarkdownForSpeech(rawText);
        if (TextUtils.isEmpty(speechText)) return;

        if (chatAdapter != null) {
            chatAdapter.setCurrentlySpeakingPosition(position);
        }

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ai_reply_" + position);
        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "ai_reply_" + position);
    }

    private void handleCopyAiText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("UkilBhai AI Response", text);
            clipboard.setPrimaryClip(clip);
            boolean isBn = LanguageHelper.isBengali(this);
            Toast.makeText(this, isBn ? "অনুলিপি করা হয়েছে" : "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private String stripMarkdownForSpeech(String markdown) {
        if (markdown == null || markdown.isEmpty()) return "";
        // Remove markdown tables
        String clean = markdown.replaceAll("(?m)^\\|.*\\|$", " ");
        clean = clean.replaceAll("\\|", " ");
        // Remove code blocks
        clean = clean.replaceAll("```[\\s\\S]*?```", " ");
        clean = clean.replaceAll("`([^`]+)`", "$1");
        // Remove markdown links [text](url) -> text
        clean = clean.replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1");
        // Remove images ![alt](url)
        clean = clean.replaceAll("!\\[[^\\]]*\\]\\([^\\)]+\\)", " ");
        // Remove headers #, ##, etc.
        clean = clean.replaceAll("(?m)^#{1,6}\\s+", "");
        // Remove bold/italics
        clean = clean.replaceAll("[*_~]{1,3}(.*?)[*_~]{1,3}", "$1");
        // Remove bullet markers
        clean = clean.replaceAll("(?m)^[\\*\\-\\+]\\s+", "");
        // Remove numbered list prefixes
        clean = clean.replaceAll("(?m)^\\d+\\.\\s+", "");
        // Collapse multiple spaces/newlines
        clean = clean.replaceAll("\\s+", " ").trim();
        return clean;
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

        boolean isBn = LanguageHelper.isBengali(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, isBn ? "bn-BD" : "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, isBn ? "bn-BD" : "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, isBn ? "bn-BD" : "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, isBn ? "আপনার আইনি সমস্যাটি বলুন..." : "Speak your legal query...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            stopMicPulse();
            Toast.makeText(this, isBn
                    ? "আপনার ডিভাইসে ভয়েস রিকগনিশন সুবিধা পাওয়া যায়নি।"
                    : "Voice recognition service not available on device.", Toast.LENGTH_SHORT).show();
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
        boolean isBn = LanguageHelper.isBengali(this);
        String[] topics = isBn ? new String[]{
                "জিডি (সাধারণ ডায়েরি) করার নিয়ম ও ড্রাফট ফরম্যাট",
                "দেনমোহর ও তালাক সংক্রান্ত আইনি অধিকার",
                "বাড়িভাড়া আইন ও উচ্ছেদ নোটিশ বিধিমালা",
                "মোটরযান আইন ও ট্রাফিক মামলার জরিমানা তালিকা",
                "চেক ডিজঅনার (এনআই এক্ট ১৩৮ ধারা) মামলা"
        } : new String[]{
                "General Diary (GD) draft format & guidelines",
                "Dower & divorce legal rights and family law",
                "Tenancy Act & eviction notice procedures",
                "Motor Vehicles Act & traffic fine schedule",
                "Dishonour of Cheque (NI Act Section 138) cases"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(isBn ? "দ্রুত আইনি বিষয় ও সংযুক্তি নির্বাচন করুন" : "Select Quick Legal Topic / Attachment")
                .setItems(topics, (dialog, which) -> {
                    currentAttachmentContext = topics[which];
                    if (layoutAttachmentBar != null && tvAttachmentTitle != null) {
                        tvAttachmentTitle.setText(currentAttachmentContext);
                        layoutAttachmentBar.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton(isBn ? "বাতিল" : "Cancel", null)
                .show();
    }

    private void removeAttachment() {
        currentAttachmentContext = null;
        if (layoutAttachmentBar != null) {
            layoutAttachmentBar.setVisibility(View.GONE);
        }
    }

    private void startNewChat() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
        if (chatAdapter != null) {
            chatAdapter.setCurrentlySpeakingPosition(-1);
        }
        messageList.clear();
        addInitialWelcomeMessage();
        chatAdapter.notifyDataSetChanged();
        boolean isBn = LanguageHelper.isBengali(this);
        Toast.makeText(this, isBn ? "নতুন আইনি জিজ্ঞাসা শুরু হয়েছে" : "Started new legal inquiry", Toast.LENGTH_SHORT).show();
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
                boolean isBn = LanguageHelper.isBengali(this);
                Toast.makeText(this, isBn 
                        ? "ভয়েস ইনপুটের জন্য অডিও রেকর্ডিং পারমিশন প্রয়োজন।" 
                        : "Audio recording permission required for voice input.", Toast.LENGTH_SHORT).show();
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
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            if (chatAdapter != null) {
                chatAdapter.setCurrentlySpeakingPosition(-1);
            }
        }
        try {
            unregisterReceiver(networkReceiver);
        } catch (IllegalArgumentException ignored) {}
        try {
            unregisterReceiver(airplaneModeReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    private void updateConnectivityUI() {
        runOnUiThread(() -> {
            boolean isBn = LanguageHelper.isBengali(this);
            if (isAirplaneModeOn) {
                if (layoutOfflineBanner != null) {
                    layoutOfflineBanner.setVisibility(View.VISIBLE);
                    layoutOfflineBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.warning_amber_bg));
                }
                if (tvOfflineBannerText != null) {
                    tvOfflineBannerText.setText(isBn
                            ? "বিমান মোড সক্রিয় রয়েছে। ইন্টারনেট সংযোগ বন্ধ।"
                            : "Airplane mode is active. Internet connection off.");
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
                    tvOfflineBannerText.setText(isBn
                            ? "ইন্টারনেট সংযোগ পাওয়া যাচ্ছে না। অনুগ্রহ করে নেটওয়ার্ক চেক করুন।"
                            : "No internet connection found. Please check network.");
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
        boolean isBn = LanguageHelper.isBengali(this);
        String welcome = isBn
                ? "আসসালামু আলাইকুম! আমি 'উকিল ভাই' এআই। আপনার কি ধরনের আইনি পরামর্শ বা তথ্য প্রয়োজন? দয়া করে আপনার সমস্যাটি লিখুন।"
                : "Hello! I am Ukil Bhai AI. What kind of legal advice or information do you need? Please describe your query.";
        messageList.add(new ChatMessage(welcome, ChatMessage.SENDER_AI));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
    }

    private void handleSendMessage() {
        String prompt = etUserPrompt.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            return;
        }

        boolean isBn = LanguageHelper.isBengali(this);
        if (isAirplaneModeOn) {
            Toast.makeText(this, isBn
                    ? "বিমান মোড চালু থাকায় বার্তা পাঠানো সম্ভব নয়।"
                    : "Cannot send message while Airplane mode is on.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkConnected) {
            Toast.makeText(this, isBn
                    ? "ইন্টারনেট সংযোগ নেই। অনুগ্রহ করে নেটওয়ার্ক চালু করুন।"
                    : "No internet connection. Please turn on network.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop any active TTS when user sends a new message
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            if (chatAdapter != null) {
                chatAdapter.setCurrentlySpeakingPosition(-1);
            }
        }

        // Attach legal context if selected
        String finalPrompt = prompt;
        if (!TextUtils.isEmpty(currentAttachmentContext)) {
            finalPrompt = isBn
                    ? "[সংযুক্তি/আইনি বিষয়: " + currentAttachmentContext + "]\n" + prompt
                    : "[Attachment/Legal Topic: " + currentAttachmentContext + "]\n" + prompt;
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
                String errPrefix = isBn ? "ত্রুটি: " : "Error: ";
                messageList.add(new ChatMessage(errPrefix + errorMessage, ChatMessage.SENDER_AI, true));
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
