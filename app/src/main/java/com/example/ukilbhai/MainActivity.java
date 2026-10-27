package com.example.ukilbhai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.SettingsDialogHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);

        ensureFirebase();
        try {
            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        android.content.SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);

        if ((mAuth == null || mAuth.getCurrentUser() == null) && !isGuestMode) {
            redirectToAuth();
            return;
        }

        setContentView(R.layout.activity_main);

        setupProfileAvatar();
        setupMainMenuMoreButton();
        setupEmergencyButtons();
        setupAiChatButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.content.SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
        if ((mAuth == null || mAuth.getCurrentUser() == null) && !isGuestMode) {
            redirectToAuth();
        }
    }

    private void ensureFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("1:730595914038:android:7afab680a52a9166770397")
                        .setApiKey("AIzaSyBRscW2eUNKlUbUqlZZyv-0Nr-43ps8Nuc")
                        .setProjectId("ukilbhai-9414f")
                        .setStorageBucket("ukilbhai-9414f.firebasestorage.app")
                        .setGcmSenderId("730595914038")
                        .build();
                FirebaseApp.initializeApp(this, options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void redirectToAuth() {
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupProfileAvatar() {
        View cardProfileAvatar = findViewById(R.id.cardProfileAvatar);
        if (cardProfileAvatar != null) {
            cardProfileAvatar.setOnClickListener(v -> showProfileDialog());
        }
    }

    private void showProfileDialog() {
        FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
        android.content.SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isLocalGuest = prefs.getBoolean("is_guest_mode", false);

        if (currentUser == null && !isLocalGuest) {
            redirectToAuth();
            return;
        }

        boolean isBn = LanguageHelper.isBengali(this);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_profile, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        View btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);
        TextView tvDialogName = dialogView.findViewById(R.id.tvDialogName);
        TextView tvDialogEmail = dialogView.findViewById(R.id.tvDialogEmail);
        TextView tvDialogBadge = dialogView.findViewById(R.id.tvDialogBadge);
        View layoutLegalDetails = dialogView.findViewById(R.id.layoutLegalDetails);
        TextView tvLabelNid = dialogView.findViewById(R.id.tvLabelNid);
        TextView tvDialogNid = dialogView.findViewById(R.id.tvDialogNid);
        TextView tvLabelLocation = dialogView.findViewById(R.id.tvLabelLocation);
        TextView tvDialogLocation = dialogView.findViewById(R.id.tvDialogLocation);
        TextView tvLabelAddress = dialogView.findViewById(R.id.tvLabelAddress);
        TextView tvDialogAddress = dialogView.findViewById(R.id.tvDialogAddress);
        MaterialButton btnDialogLogout = dialogView.findViewById(R.id.btnDialogLogout);

        tvDialogTitle.setText(isBn ? "ব্যবহারকারী প্রোফাইল" : "Citizen Profile");
        btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        boolean isAnonymous = isLocalGuest || (currentUser != null && currentUser.isAnonymous());
        if (isAnonymous) {
            tvDialogName.setText(isBn ? "অতিথি নাগরিক" : "Guest Citizen");
            tvDialogEmail.setText(isBn ? "গেস্ট সেশন সক্রিয়" : "Guest Session Active");
            tvDialogBadge.setText(isBn ? "গেস্ট এক্সেস" : "Guest Access");
            layoutLegalDetails.setVisibility(View.GONE);

            btnDialogLogout.setText(isBn ? "লগইন বা নিবন্ধন করুন" : "Sign In or Register");
            btnDialogLogout.setOnClickListener(v -> {
                dialog.dismiss();
                prefs.edit().clear().apply();
                if (mAuth != null) mAuth.signOut();
                redirectToAuth();
            });
        } else {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            tvDialogName.setText(!TextUtils.isEmpty(displayName) ? displayName : (isBn ? "নাগরিক" : "Citizen"));
            tvDialogEmail.setText(!TextUtils.isEmpty(email) ? email : "--");
            tvDialogBadge.setText(isBn ? "নিবন্ধিত নাগরিক" : "Registered Citizen");
            layoutLegalDetails.setVisibility(View.VISIBLE);

            tvLabelNid.setText(isBn ? "জাতীয় পরিচয়পত্র:" : "National ID:");
            tvLabelLocation.setText(isBn ? "থানা ও জেলা:" : "Thana & District:");
            tvLabelAddress.setText(isBn ? "ঠিকানা:" : "Address:");

            String notSpecified = isBn ? "উল্লেখ নেই" : "Not specified";
            tvDialogNid.setText(notSpecified);
            tvDialogLocation.setText(notSpecified);
            tvDialogAddress.setText(notSpecified);

            // Fetch profile data from Cloud Firestore
            mFirestore.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot doc = task.getResult();
                            String nid = doc.getString("nid");
                            String location = doc.getString("location");
                            String address = doc.getString("address");

                            if (!TextUtils.isEmpty(nid)) tvDialogNid.setText(nid);
                            if (!TextUtils.isEmpty(location)) tvDialogLocation.setText(location);
                            if (!TextUtils.isEmpty(address)) tvDialogAddress.setText(address);
                        }
                    });

            btnDialogLogout.setText(isBn ? "লগআউট করুন" : "Log Out");
            btnDialogLogout.setOnClickListener(v -> {
                dialog.dismiss();
                mAuth.signOut();
                Toast.makeText(MainActivity.this,
                        isBn ? "লগআউট সফল হয়েছে" : "Logged out successfully",
                        Toast.LENGTH_SHORT).show();
                redirectToAuth();
            });
        }

        dialog.show();
    }

    private void setupMainMenuMoreButton() {
        View btnMainMenuMore = findViewById(R.id.btnMainMenuMore);
        if (btnMainMenuMore != null) {
            btnMainMenuMore.setOnClickListener(v -> {
                SettingsDialogHelper.showSettingsDialog(this, this::recreate);
            });
        }
    }

    private void setupEmergencyButtons() {
        // 999 Police & Emergency Hotline
        View btnEmergency999 = findViewById(R.id.btnEmergency999);
        if (btnEmergency999 != null) {
            btnEmergency999.setOnClickListener(v -> dialPhoneNumber("999"));
        }

        // 16699 National Legal Aid Services Hotline
        View btnLegalAid16699 = findViewById(R.id.btnLegalAid16699);
        if (btnLegalAid16699 != null) {
            btnLegalAid16699.setOnClickListener(v -> dialPhoneNumber("16699"));
        }
    }

    private void setupAiChatButton() {
        // Floating Button: "এআই পরামর্শ নিন"
        View fabAiChat = findViewById(R.id.fabAiChat);
        if (fabAiChat != null) {
            fabAiChat.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
            });
        }
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
