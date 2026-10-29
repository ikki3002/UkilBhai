package com.example.ukilbhai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ukilbhai.model.UserProfile;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    // Header views
    private TextView tvAuthTitle;
    private TextView tvAuthTagline;
    private MaterialCardView btnAuthLang;
    private TextView tvAuthLangLabel;

    // Segmented tabs
    private TextView tabLogin;
    private TextView tabRegister;
    private ProgressBar progressAuth;

    // Login Form views
    private LinearLayout layoutLoginForm;
    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private TextView tvForgotPassword;
    private MaterialButton btnSignIn;

    // Register Form views
    private LinearLayout layoutRegisterForm;
    private MaterialCardView cardRegisterPhoto;
    private ImageView ivRegisterPhoto;
    private Uri selectedPhotoUri = null;
    private ActivityResultLauncher<String> photoPickerLauncher;

    private EditText etRegisterName;
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private TextView tvLegalHeader;
    private TextView tvOptionalTag;
    private EditText etRegisterNid;

    // Structured Bangladesh Legal Address
    private EditText etRegisterDistrict;
    private EditText etRegisterThana;
    private EditText etRegisterUnion;
    private EditText etRegisterPost;
    private EditText etRegisterVillageRoad;

    private MaterialButton btnRegister;

    // Bottom action views
    private TextView tvOrLabel;
    private MaterialButton btnGuestSignIn;

    private boolean isLoginTab = true;

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

        // If user is already authenticated or in guest session, route directly to MainActivity
        android.content.SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
        if (isGuestMode || (mAuth != null && mAuth.getCurrentUser() != null)) {
            navigateToMain();
            return;
        }

        // Image picker launcher for profile photo
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        if (ivRegisterPhoto != null) {
                            ivRegisterPhoto.setImageURI(uri);
                        }
                    }
                }
        );

        setContentView(R.layout.activity_auth);

        initViews();
        setupTabSwitching();
        setupLanguageToggle();
        setupAuthActions();
        setupPhotoPicker();
        updateLanguageUI();
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

    private void initViews() {
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthTagline = findViewById(R.id.tvAuthTagline);
        btnAuthLang = findViewById(R.id.btnAuthLang);
        tvAuthLangLabel = findViewById(R.id.tvAuthLangLabel);

        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        progressAuth = findViewById(R.id.progressAuth);

        layoutLoginForm = findViewById(R.id.layoutLoginForm);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnSignIn = findViewById(R.id.btnSignIn);

        layoutRegisterForm = findViewById(R.id.layoutRegisterForm);
        cardRegisterPhoto = findViewById(R.id.cardRegisterPhoto);
        ivRegisterPhoto = findViewById(R.id.ivRegisterPhoto);

        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        tvLegalHeader = findViewById(R.id.tvLegalHeader);
        tvOptionalTag = findViewById(R.id.tvOptionalTag);
        etRegisterNid = findViewById(R.id.etRegisterNid);

        etRegisterDistrict = findViewById(R.id.etRegisterDistrict);
        etRegisterThana = findViewById(R.id.etRegisterThana);
        etRegisterUnion = findViewById(R.id.etRegisterUnion);
        etRegisterPost = findViewById(R.id.etRegisterPost);
        etRegisterVillageRoad = findViewById(R.id.etRegisterVillageRoad);

        btnRegister = findViewById(R.id.btnRegister);

        tvOrLabel = findViewById(R.id.tvOrLabel);
        btnGuestSignIn = findViewById(R.id.btnGuestSignIn);
    }

    private void setupPhotoPicker() {
        if (cardRegisterPhoto != null) {
            cardRegisterPhoto.setOnClickListener(v -> {
                if (photoPickerLauncher != null) {
                    photoPickerLauncher.launch("image/*");
                }
            });
        }
    }

    private void setupTabSwitching() {
        tabLogin.setOnClickListener(v -> switchTab(true));
        tabRegister.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean login) {
        isLoginTab = login;
        if (login) {
            tabLogin.setBackgroundResource(R.drawable.bg_pill_emergency_green);
            tabLogin.setTextColor(getResources().getColor(R.color.mint_accent));

            tabRegister.setBackground(null);
            tabRegister.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutLoginForm.setVisibility(View.VISIBLE);
            layoutRegisterForm.setVisibility(View.GONE);
        } else {
            tabRegister.setBackgroundResource(R.drawable.bg_pill_emergency_green);
            tabRegister.setTextColor(getResources().getColor(R.color.mint_accent));

            tabLogin.setBackground(null);
            tabLogin.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutRegisterForm.setVisibility(View.VISIBLE);
            layoutLoginForm.setVisibility(View.GONE);
        }
    }

    private void setupLanguageToggle() {
        btnAuthLang.setOnClickListener(v -> {
            boolean currentlyBn = LanguageHelper.isBengali(this);
            String nextLang = currentlyBn ? LanguageHelper.LANG_EN : LanguageHelper.LANG_BN;
            LanguageHelper.saveLanguage(this, nextLang);
            updateLanguageUI();
        });
    }

    private void updateLanguageUI() {
        boolean isBn = LanguageHelper.isBengali(this);

        if (isBn) {
            tvAuthTitle.setText("উকিল ভাই");
            tvAuthTagline.setText("আপনার ডিজিটাল আইনি সহায়ক");
            tvAuthLangLabel.setText("English");

            tabLogin.setText("লগইন");
            tabRegister.setText("নিবন্ধন");

            etLoginEmail.setHint("ইমেইল ঠিকানা লিখুন");
            etLoginPassword.setHint("পাসওয়ার্ড");
            tvForgotPassword.setText("পাসওয়ার্ড ভুলে গেছেন?");
            btnSignIn.setText("লগইন করুন");

            etRegisterName.setHint("আপনার পূর্ণ নাম");
            etRegisterEmail.setHint("ইমেইল ঠিকানা");
            etRegisterPassword.setHint("পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)");

            tvLegalHeader.setText("আইনি তথ্য ও ঠিকানা");
            tvOptionalTag.setText("ঐচ্ছিক");
            etRegisterNid.setHint("জাতীয় পরিচয়পত্র (NID) নম্বর (ঐচ্ছিক)");

            if (etRegisterDistrict != null) etRegisterDistrict.setHint("জেলা (Zilla)");
            if (etRegisterThana != null) etRegisterThana.setHint("থানা / উপজেলা");
            if (etRegisterUnion != null) etRegisterUnion.setHint("ওয়ার্ড / ইউনিয়ন");
            if (etRegisterPost != null) etRegisterPost.setHint("ডাকঘর ও পোস্টকোড");
            if (etRegisterVillageRoad != null) etRegisterVillageRoad.setHint("গ্রাম / সড়ক / বাসা বা হোল্ডিং নং (ঐচ্ছিক)");

            btnRegister.setText("নিবন্ধন সম্পন্ন করুন");

            tvOrLabel.setText("অথবা");
            btnGuestSignIn.setText("গেস্ট হিসেবে প্রবেশ করুন");
        } else {
            tvAuthTitle.setText("Ukil Bhai");
            tvAuthTagline.setText("Your Digital Legal Assistant");
            tvAuthLangLabel.setText("বাংলা");

            tabLogin.setText("Sign In");
            tabRegister.setText("Register");

            etLoginEmail.setHint("Enter your email");
            etLoginPassword.setHint("Password");
            tvForgotPassword.setText("Forgot Password?");
            btnSignIn.setText("Sign In");

            etRegisterName.setHint("Your Full Name");
            etRegisterEmail.setHint("Email Address");
            etRegisterPassword.setHint("Password (min 6 characters)");

            tvLegalHeader.setText("Legal Identity & Address");
            tvOptionalTag.setText("Optional");
            etRegisterNid.setHint("National ID (NID) Number (Optional)");

            if (etRegisterDistrict != null) etRegisterDistrict.setHint("District (Zilla)");
            if (etRegisterThana != null) etRegisterThana.setHint("Thana / Upazila");
            if (etRegisterUnion != null) etRegisterUnion.setHint("Union / Ward");
            if (etRegisterPost != null) etRegisterPost.setHint("Post Office & Code");
            if (etRegisterVillageRoad != null) etRegisterVillageRoad.setHint("Village / Road / Holding (Optional)");

            btnRegister.setText("Complete Registration");

            tvOrLabel.setText("OR");
            btnGuestSignIn.setText("Continue as Guest");
        }
    }

    private void setupAuthActions() {
        btnSignIn.setOnClickListener(v -> performLogin());
        btnRegister.setOnClickListener(v -> performRegistration());
        btnGuestSignIn.setOnClickListener(v -> performGuestSignIn());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void performLogin() {
        boolean isBn = LanguageHelper.isBengali(this);
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError(isBn ? "সঠিক ইমেইল লিখুন" : "Please enter a valid email");
            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etLoginPassword.setError(isBn ? "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে" : "Password must be at least 6 characters");
            etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE)
                                .edit()
                                .putBoolean("is_guest_mode", false)
                                .apply();
                        Toast.makeText(AuthActivity.this,
                                isBn ? "সফলভাবে লগইন হয়েছে" : "Logged in successfully",
                                Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        handleAuthError(task.getException(), isBn ? "লগইন" : "Login");
                    }
                });
    }

    private void performRegistration() {
        boolean isBn = LanguageHelper.isBengali(this);
        String name = etRegisterName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        String nid = etRegisterNid.getText().toString().trim();
        String district = etRegisterDistrict != null ? etRegisterDistrict.getText().toString().trim() : "";
        String thana = etRegisterThana != null ? etRegisterThana.getText().toString().trim() : "";
        String unionWard = etRegisterUnion != null ? etRegisterUnion.getText().toString().trim() : "";
        String postOffice = etRegisterPost != null ? etRegisterPost.getText().toString().trim() : "";
        String villageRoad = etRegisterVillageRoad != null ? etRegisterVillageRoad.getText().toString().trim() : "";
        String photoUrl = selectedPhotoUri != null ? selectedPhotoUri.toString() : "";

        if (TextUtils.isEmpty(name)) {
            etRegisterName.setError(isBn ? "আপনার নাম লিখুন" : "Please enter your name");
            etRegisterName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError(isBn ? "সঠিক ইমেইল লিখুন" : "Please enter a valid email");
            etRegisterEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etRegisterPassword.setError(isBn ? "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে" : "Password must be at least 6 characters");
            etRegisterPassword.requestFocus();
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update display name and photoUri on Firebase Auth Profile
                            UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name);
                            if (selectedPhotoUri != null) {
                                profileBuilder.setPhotoUri(selectedPhotoUri);
                            }
                            user.updateProfile(profileBuilder.build());

                            // Store legal profile document in Cloud Firestore
                            UserProfile profile = new UserProfile(
                                    user.getUid(),
                                    name,
                                    email,
                                    nid,
                                    photoUrl,
                                    district,
                                    thana,
                                    unionWard,
                                    postOffice,
                                    villageRoad,
                                    System.currentTimeMillis(),
                                    false
                            );

                            getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("is_guest_mode", false)
                                    .putString("profile_name", name)
                                    .putString("profile_nid", nid)
                                    .putString("profile_district", district)
                                    .putString("profile_thana", thana)
                                    .putString("profile_union", unionWard)
                                    .putString("profile_post", postOffice)
                                    .putString("profile_village", villageRoad)
                                    .putString("profile_location", profile.getLocation())
                                    .putString("profile_address", profile.getAddress())
                                    .putString("profile_photo", photoUrl)
                                    .apply();

                            mFirestore.collection("users")
                                    .document(user.getUid())
                                    .set(profile.toMap(), SetOptions.merge())
                                    .addOnCompleteListener(storeTask -> {
                                        setLoading(false);
                                        Toast.makeText(AuthActivity.this,
                                                isBn ? "নিবন্ধন সম্পন্ন হয়েছে!" : "Registration completed!",
                                                Toast.LENGTH_SHORT).show();
                                        navigateToMain();
                                    });
                        } else {
                            setLoading(false);
                            navigateToMain();
                        }
                    } else {
                        setLoading(false);
                        handleAuthError(task.getException(), isBn ? "নিবন্ধন" : "Registration");
                    }
                });
    }

    private void performGuestSignIn() {
        boolean isBn = LanguageHelper.isBengali(this);
        setLoading(true);

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfile guestProfile = new UserProfile(
                                    user.getUid(),
                                    isBn ? "অতিথি ব্যবহারকারী" : "Guest User",
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    System.currentTimeMillis(),
                                    true
                            );
                            mFirestore.collection("users")
                                    .document(user.getUid())
                                    .set(guestProfile.toMap());
                        }

                        Toast.makeText(AuthActivity.this,
                                isBn ? "গেস্ট হিসেবে প্রবেশ করেছেন" : "Entered as Guest",
                                Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        // If Firebase Anonymous is disabled or configuration missing, allow local guest access
                        enterGuestOfflineMode();
                    }
                });
    }

    private void enterGuestOfflineMode() {
        boolean isBn = LanguageHelper.isBengali(this);
        getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE)
                .edit()
                .putBoolean("is_guest_mode", true)
                .putString("guest_display_name", isBn ? "অতিথি নাগরিক" : "Guest Citizen")
                .apply();

        Toast.makeText(AuthActivity.this,
                isBn ? "গেস্ট হিসেবে প্রবেশ করেছেন" : "Entered as Guest",
                Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void handleAuthError(Exception e, String actionName) {
        boolean isBn = LanguageHelper.isBengali(this);
        String rawMsg = e != null ? e.getMessage() : "";

        if (rawMsg != null && (rawMsg.contains("CONFIGURATION_NOT_FOUND") || rawMsg.contains("internal error") || rawMsg.contains("INTERNAL_ERROR"))) {
            new AlertDialog.Builder(this)
                    .setTitle(isBn ? "Firebase Authentication সক্রিয় নেই" : "Firebase Auth Not Activated")
                    .setMessage(isBn
                            ? "ফায়ারবেস প্রজেক্টে Authentication এখনও চালু করা হয়নি। অনুগ্রহ করে নিচের ধাপগুলো সম্পন্ন করুন:\n\n" +
                              "১. Firebase Console-এ যান (console.firebase.google.com)\n" +
                              "২. Build > Authentication মেনুতে গিয়ে 'Get Started' চাপুন\n" +
                              "৩. 'Sign-in method' ট্যাবে গিয়ে 'Email/Password' সক্রিয় (Enable) করে Save করুন।"
                            : "Firebase Authentication has not been activated in your project yet:\n\n" +
                              "1. Go to Firebase Console (console.firebase.google.com)\n" +
                              "2. Navigate to Build > Authentication and click 'Get Started'\n" +
                              "3. Under 'Sign-in method', enable 'Email/Password' and click Save.")
                    .setPositiveButton(isBn ? "বুঝেছি" : "OK", (d, w) -> d.dismiss())
                    .setNeutralButton(isBn ? "গেস্ট হিসেবে চালিয়ে যান" : "Continue as Guest", (d, w) -> {
                        d.dismiss();
                        enterGuestOfflineMode();
                    })
                    .show();
        } else {
            Toast.makeText(this,
                    (isBn ? actionName + " ব্যর্থ হয়েছে: " : actionName + " failed: ") + (rawMsg != null ? rawMsg : ""),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showForgotPasswordDialog() {
        boolean isBn = LanguageHelper.isBengali(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isBn ? "পাসওয়ার্ড রিসেট" : "Reset Password");
        builder.setMessage(isBn ? "আপনার অ্যাকাউন্টের ইমেইল ঠিকানা দিন:" : "Enter your account email address:");

        final EditText input = new EditText(this);
        input.setHint("example@mail.com");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setHintTextColor(getResources().getColor(R.color.text_secondary));
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);

        builder.setPositiveButton(isBn ? "রিসেট লিংক পাঠান" : "Send Reset Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(AuthActivity.this,
                                        isBn ? "পাসওয়ার্ড রিসেট লিংক আপনার ইমেইলে পাঠানো হয়েছে" : "Password reset email sent successfully",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                String err = task.getException() != null ? task.getException().getMessage() : "Failed";
                                Toast.makeText(AuthActivity.this,
                                        (isBn ? "ব্যর্থ হয়েছে: " : "Failed: ") + err,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(AuthActivity.this,
                        isBn ? "সঠিক ইমেইল ঠিকানা লিখুন" : "Please enter a valid email address",
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(isBn ? "বাতিল" : "Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void setLoading(boolean loading) {
        progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        btnGuestSignIn.setEnabled(!loading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
