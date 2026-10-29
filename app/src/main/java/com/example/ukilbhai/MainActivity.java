package com.example.ukilbhai;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.SettingsDialogHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupProfileAvatar();
        setupMainMenuMoreButton();
        setupEmergencyButtons();
        setupAiChatButton();
    }

    private void setupProfileAvatar() {
        View cardProfileAvatar = findViewById(R.id.cardProfileAvatar);
        View ivProfileAvatar = findViewById(R.id.ivProfileAvatar);

        View.OnClickListener clickListener = v -> showProfileDialog();

        if (cardProfileAvatar != null) {
            cardProfileAvatar.setClickable(true);
            cardProfileAvatar.setFocusable(true);
            applyPressScale(cardProfileAvatar);
            cardProfileAvatar.setOnClickListener(clickListener);
        }
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(clickListener);
        }
    }

    private void showProfileDialog() {
        boolean isBn = LanguageHelper.isBengali(this);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_guest_profile, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        View btnClose = dialogView.findViewById(R.id.btnDialogClose);
        TextView tvName = dialogView.findViewById(R.id.tvDialogName);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        MaterialButton btnSettings = dialogView.findViewById(R.id.btnDialogSettings);
        MaterialButton btnDismiss = dialogView.findViewById(R.id.btnDialogDismiss);

        if (tvTitle != null) {
            tvTitle.setText(isBn ? "ব্যবহারকারী প্রোফাইল" : "Citizen Profile");
        }
        if (tvName != null) {
            tvName.setText(isBn ? "অতিথি নাগরিক" : "Guest Citizen");
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText(isBn ? "উকিল ভাই এআই এক্সেস সক্রিয়" : "Ukil Bhai AI Legal Assistant Active");
        }
        if (btnSettings != null) {
            btnSettings.setText(isBn ? "ভাষা ও থিম পরিবর্তন" : "Change Language & Theme");
            btnSettings.setOnClickListener(v -> {
                dialog.dismiss();
                SettingsDialogHelper.showSettingsDialog(this, this::recreate);
            });
        }
        if (btnDismiss != null) {
            btnDismiss.setText(isBn ? "ঠিক আছে" : "Got It");
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void setupMainMenuMoreButton() {
        View btnMainMenuMore = findViewById(R.id.btnMainMenuMore);
        if (btnMainMenuMore != null) {
            applyPressScale(btnMainMenuMore);
            btnMainMenuMore.setOnClickListener(v -> {
                SettingsDialogHelper.showSettingsDialog(this, this::recreate);
            });
        }
    }

    private void setupEmergencyButtons() {
        // 999 Police & Emergency Hotline
        View btnEmergency999 = findViewById(R.id.btnEmergency999);
        if (btnEmergency999 != null) {
            applyPressScale(btnEmergency999);
            btnEmergency999.setOnClickListener(v -> dialPhoneNumber("999"));
        }

        // 16699 National Legal Aid Services Hotline
        View btnLegalAid16699 = findViewById(R.id.btnLegalAid16699);
        if (btnLegalAid16699 != null) {
            applyPressScale(btnLegalAid16699);
            btnLegalAid16699.setOnClickListener(v -> dialPhoneNumber("16699"));
        }
    }

    private void setupAiChatButton() {
        // Floating Button: "এআই পরামর্শ নিন"
        View fabAiChat = findViewById(R.id.fabAiChat);
        if (fabAiChat != null) {
            applyPressScale(fabAiChat);
            fabAiChat.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
            });
        }
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

    private void dialPhoneNumber(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "কল করার অপশন পাওয়া যায়নি: " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }
}
