package com.example.ukilbhai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ukilbhai.model.UserProfile;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private EditText etRegisterName;
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private TextView tvLegalHeader;
    private TextView tvOptionalTag;
    private EditText etRegisterNid;
    private EditText etRegisterLocation;
    private EditText etRegisterAddress;
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

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // If user is already authenticated, route directly to MainActivity
        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_auth);

        initViews();
        setupTabSwitching();
        setupLanguageToggle();
        setupAuthActions();
        updateLanguageUI();
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
        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        tvLegalHeader = findViewById(R.id.tvLegalHeader);
        tvOptionalTag = findViewById(R.id.tvOptionalTag);
        etRegisterNid = findViewById(R.id.etRegisterNid);
        etRegisterLocation = findViewById(R.id.etRegisterLocation);
        etRegisterAddress = findViewById(R.id.etRegisterAddress);
        btnRegister = findViewById(R.id.btnRegister);

        tvOrLabel = findViewById(R.id.tvOrLabel);
        btnGuestSignIn = findViewById(R.id.btnGuestSignIn);
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

            tvLegalHeader.setText("আইনি তথ্য ও পরিচয়");
            tvOptionalTag.setText("ঐচ্ছিক");
            etRegisterNid.setHint("জাতীয় পরিচয়পত্র (NID) নম্বর (ঐচ্ছিক)");
            etRegisterLocation.setHint("জেলা ও থানা (যেমন: ঢাকা, ধানমন্ডি) (ঐচ্ছিক)");
            etRegisterAddress.setHint("বর্তমান ঠিকানা / গ্রাম (ঐচ্ছিক)");
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

            tvLegalHeader.setText("Legal Identity & Profile");
            tvOptionalTag.setText("Optional");
            etRegisterNid.setHint("National ID (NID) Number (Optional)");
            etRegisterLocation.setHint("District & Thana (e.g. Dhaka, Dhanmondi) (Optional)");
            etRegisterAddress.setHint("Present Address / Village (Optional)");
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
                        Toast.makeText(AuthActivity.this,
                                isBn ? "সফলভাবে লগইন হয়েছে" : "Logged in successfully",
                                Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Auth Failed";
                        Toast.makeText(AuthActivity.this,
                                (isBn ? "লগইন ব্যর্থ হয়েছে: " : "Login failed: ") + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performRegistration() {
        boolean isBn = LanguageHelper.isBengali(this);
        String name = etRegisterName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        String nid = etRegisterNid.getText().toString().trim();
        String location = etRegisterLocation.getText().toString().trim();
        String address = etRegisterAddress.getText().toString().trim();

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
                            // Update display name on Firebase Auth Profile
                            UserProfileChangeRequest updateProfile = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(updateProfile);

                            // Store legal profile document in Cloud Firestore
                            UserProfile profile = new UserProfile(
                                    user.getUid(),
                                    name,
                                    email,
                                    nid,
                                    location,
                                    address,
                                    System.currentTimeMillis(),
                                    false
                            );

                            mFirestore.collection("users")
                                    .document(user.getUid())
                                    .set(profile.toMap())
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
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(AuthActivity.this,
                                (isBn ? "নিবন্ধন ব্যর্থ হয়েছে: " : "Registration failed: ") + errorMsg,
                                Toast.LENGTH_LONG).show();
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
                            // Record guest user entry in Firestore
                            UserProfile guestProfile = new UserProfile(
                                    user.getUid(),
                                    isBn ? "অতিথি ব্যবহারকারী" : "Guest User",
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
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error";
                        Toast.makeText(AuthActivity.this,
                                (isBn ? "গেস্ট লগইন ব্যর্থ হয়েছে: " : "Guest login failed: ") + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
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
