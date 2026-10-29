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
import android.widget.ImageButton;

import android.widget.LinearLayout;

import android.widget.ProgressBar;

import android.widget.TextView;

import android.widget.Toast;



import androidx.activity.result.ActivityResultLauncher;

import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;



import java.util.ArrayList;

import java.util.List;



import com.example.ukilbhai.model.UserProfile;

import com.example.ukilbhai.model.LawyerProfile;

import com.example.ukilbhai.lawyer.LawyerDashboardActivity;

import com.example.ukilbhai.lawyer.LawyerLocalStore;

import android.widget.AutoCompleteTextView;

import android.widget.ArrayAdapter;

import com.example.ukilbhai.util.SavedAccountManager;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import com.example.ukilbhai.util.LanguageHelper;

import com.example.ukilbhai.util.ImageStorageHelper;

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

    private AutoCompleteTextView etLoginEmail;

    private EditText etLoginPassword;

    private TextView tvForgotPassword;

    private MaterialButton btnSignIn;



    // Saved Account Suggestion views (Facebook-style)

    private View cardSavedAccount;

    private ImageView ivSavedAccountAvatar;

    private TextView tvSavedAccountName;

    private TextView tvSavedAccountEmail;

    private TextView tvSavedAccountRoleBadge;

    private MaterialButton btnContinueSavedAccount;

    private ImageButton btnDismissSavedAccount;

    private SavedAccountManager.SavedAccount currentSavedAccount;



    // Register Form views

    private LinearLayout layoutRegisterForm;

    private MaterialCardView cardRegisterPhoto;

    private ImageView ivRegisterPhoto;

    private Uri selectedPhotoUri = null;

    private String selectedPhotoPath = null;

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



    // Role Selection

    private MaterialCardView cardRoleCitizen;

    private MaterialCardView cardRoleLawyer;

    private TextView tvRoleCitizenTitle;

    private TextView tvRoleCitizenSub;

    private TextView tvRoleLawyerTitle;

    private TextView tvRoleLawyerSub;

    private LinearLayout layoutCitizenFields;

    private LinearLayout layoutLawyerFields;



    // Lawyer Specific Inputs

    private EditText etLawyerBarNo;

    private EditText etLawyerBarAssociation;

    private EditText etLawyerDistrict;

    private EditText etLawyerTags;

    private EditText etLawyerExperience;

    private EditText etLawyerFee;

    private EditText etLawyerBio;



    private String selectedRole = UserProfile.ROLE_CITIZEN;

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

            navigateToRoleDestination();

            return;

        }



        // Image picker launcher for profile photo

        photoPickerLauncher = registerForActivityResult(

                new ActivityResultContracts.GetContent(),

                uri -> {

                    if (uri != null) {

                        selectedPhotoUri = uri;

                        String localPath = ImageStorageHelper.saveImageToInternalStorage(this, uri);

                        selectedPhotoPath = localPath != null ? localPath : uri.toString();

                        if (ivRegisterPhoto != null) {

                            ImageStorageHelper.loadAvatar(this, ivRegisterPhoto, selectedPhotoPath);

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



        // Saved Account Quick Login views

        cardSavedAccount = findViewById(R.id.cardSavedAccount);

        ivSavedAccountAvatar = findViewById(R.id.ivSavedAccountAvatar);

        tvSavedAccountName = findViewById(R.id.tvSavedAccountName);

        tvSavedAccountEmail = findViewById(R.id.tvSavedAccountEmail);

        tvSavedAccountRoleBadge = findViewById(R.id.tvSavedAccountRoleBadge);

        btnContinueSavedAccount = findViewById(R.id.btnContinueSavedAccount);

        btnDismissSavedAccount = findViewById(R.id.btnDismissSavedAccount);

        setupSavedAccountUI();



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



        // Role Selection views

        cardRoleCitizen = findViewById(R.id.cardRoleCitizen);

        cardRoleLawyer = findViewById(R.id.cardRoleLawyer);

        tvRoleCitizenTitle = findViewById(R.id.tvRoleCitizenTitle);

        tvRoleCitizenSub = findViewById(R.id.tvRoleCitizenSub);

        tvRoleLawyerTitle = findViewById(R.id.tvRoleLawyerTitle);

        tvRoleLawyerSub = findViewById(R.id.tvRoleLawyerSub);

        layoutCitizenFields = findViewById(R.id.layoutCitizenFields);

        layoutLawyerFields = findViewById(R.id.layoutLawyerFields);



        // Lawyer specific inputs

        etLawyerBarNo = findViewById(R.id.etLawyerBarNo);

        etLawyerBarAssociation = findViewById(R.id.etLawyerBarAssociation);

        etLawyerDistrict = findViewById(R.id.etLawyerDistrict);

        etLawyerTags = findViewById(R.id.etLawyerTags);

        etLawyerExperience = findViewById(R.id.etLawyerExperience);

        etLawyerFee = findViewById(R.id.etLawyerFee);

        etLawyerBio = findViewById(R.id.etLawyerBio);



        setupRoleSwitching();

    }



    private void setupSavedAccountUI() {

        currentSavedAccount = SavedAccountManager.getRecentAccount(this);

        if (currentSavedAccount != null && cardSavedAccount != null) {

            cardSavedAccount.setVisibility(View.VISIBLE);

            if (tvSavedAccountName != null) tvSavedAccountName.setText(currentSavedAccount.name);

            if (tvSavedAccountEmail != null) tvSavedAccountEmail.setText(currentSavedAccount.email);

            if (ivSavedAccountAvatar != null) {

                ImageStorageHelper.loadAvatar(this, ivSavedAccountAvatar, currentSavedAccount.photoUrl);

            }

            if (tvSavedAccountRoleBadge != null) {

                boolean isLawyer = UserProfile.ROLE_LAWYER.equalsIgnoreCase(currentSavedAccount.role);

                tvSavedAccountRoleBadge.setText(isLawyer ? "আইনজীবী" : "নাগরিক");

            }



            View.OnClickListener clickListener = v -> {

                if (etLoginEmail != null) etLoginEmail.setText(currentSavedAccount.email);

                if (!TextUtils.isEmpty(currentSavedAccount.savedPassword)) {

                    if (etLoginPassword != null) etLoginPassword.setText(currentSavedAccount.savedPassword);

                    performLogin();

                } else {

                    if (etLoginPassword != null) etLoginPassword.requestFocus();

                }

            };



            if (btnContinueSavedAccount != null) btnContinueSavedAccount.setOnClickListener(clickListener);

            cardSavedAccount.setOnClickListener(clickListener);



            if (btnDismissSavedAccount != null) {

                btnDismissSavedAccount.setOnClickListener(v -> {

                    SavedAccountManager.removeAccount(this, currentSavedAccount.email);

                    cardSavedAccount.setVisibility(View.GONE);

                });

            }

        } else if (cardSavedAccount != null) {

            cardSavedAccount.setVisibility(View.GONE);

        }



        if (etLoginEmail != null) {

            List<String> emails = SavedAccountManager.getAllSavedEmails(this);

            if (!emails.isEmpty()) {

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,

                        android.R.layout.simple_dropdown_item_1line, emails);

                etLoginEmail.setAdapter(adapter);

                etLoginEmail.setThreshold(1);

            }

        }

    }



    private void setupRoleSwitching() {

        if (cardRoleCitizen != null && cardRoleLawyer != null) {

            cardRoleCitizen.setOnClickListener(v -> selectRole(UserProfile.ROLE_CITIZEN));

            cardRoleLawyer.setOnClickListener(v -> selectRole(UserProfile.ROLE_LAWYER));

        }

    }



    private void selectRole(String role) {

        selectedRole = role;

        if (UserProfile.ROLE_LAWYER.equals(role)) {

            cardRoleLawyer.setStrokeColor(getResources().getColor(R.color.mint_accent));

            cardRoleLawyer.setStrokeWidth(4);

            tvRoleLawyerTitle.setTextColor(getResources().getColor(R.color.mint_accent));



            cardRoleCitizen.setStrokeColor(getResources().getColor(R.color.card_stroke));

            cardRoleCitizen.setStrokeWidth(2);

            tvRoleCitizenTitle.setTextColor(getResources().getColor(R.color.text_primary));



            if (layoutLawyerFields != null) layoutLawyerFields.setVisibility(View.VISIBLE);

            if (layoutCitizenFields != null) layoutCitizenFields.setVisibility(View.GONE);

        } else {

            cardRoleCitizen.setStrokeColor(getResources().getColor(R.color.mint_accent));

            cardRoleCitizen.setStrokeWidth(4);

            tvRoleCitizenTitle.setTextColor(getResources().getColor(R.color.mint_accent));



            cardRoleLawyer.setStrokeColor(getResources().getColor(R.color.card_stroke));

            cardRoleLawyer.setStrokeWidth(2);

            tvRoleLawyerTitle.setTextColor(getResources().getColor(R.color.text_primary));



            if (layoutCitizenFields != null) layoutCitizenFields.setVisibility(View.VISIBLE);

            if (layoutLawyerFields != null) layoutLawyerFields.setVisibility(View.GONE);

        }

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

                        FirebaseUser loggedUser = mAuth.getCurrentUser();

                        if (loggedUser != null) {

                            mFirestore.collection("users").document(loggedUser.getUid()).get()

                                    .addOnCompleteListener(roleTask -> {

                                        String role = UserProfile.ROLE_CITIZEN;

                                        if (roleTask.isSuccessful() && roleTask.getResult() != null && roleTask.getResult().exists()) {

                                            String storedRole = roleTask.getResult().getString("role");

                                            if (storedRole != null) role = storedRole;

                                        } else {

                                            if (LawyerLocalStore.isUserLawyer(AuthActivity.this, loggedUser.getUid())) {

                                                role = UserProfile.ROLE_LAWYER;

                                            }

                                        }



                                        String loggedName = loggedUser.getDisplayName() != null ? loggedUser.getDisplayName() : "";

                                        String loggedPhoto = loggedUser.getPhotoUrl() != null ? loggedUser.getPhotoUrl().toString() : "";

                                        SavedAccountManager.saveAccount(AuthActivity.this, email, loggedName, role, loggedPhoto, password);



                                        getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE)

                                                .edit()

                                                .putBoolean("is_guest_mode", false)

                                                .putString("user_role", role)

                                                .apply();



                                        Toast.makeText(AuthActivity.this,

                                                isBn ? "সফলভাবে লগইন হয়েছে" : "Logged in successfully",

                                                Toast.LENGTH_SHORT).show();

                                        navigateToRoleDestination();

                                    });

                        } else {

                            navigateToRoleDestination();

                        }

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



        // Lawyer Specific

        String barNo = etLawyerBarNo != null ? etLawyerBarNo.getText().toString().trim() : "";

        String barAssociation = etLawyerBarAssociation != null ? etLawyerBarAssociation.getText().toString().trim() : "";

        String lawyerDistrict = etLawyerDistrict != null ? etLawyerDistrict.getText().toString().trim() : "";

        String tagsInput = etLawyerTags != null ? etLawyerTags.getText().toString().trim() : "";

        String expInput = etLawyerExperience != null ? etLawyerExperience.getText().toString().trim() : "0";

        String feeInput = etLawyerFee != null ? etLawyerFee.getText().toString().trim() : "0";

        String bio = etLawyerBio != null ? etLawyerBio.getText().toString().trim() : "";



        String nid = etRegisterNid.getText().toString().trim();

        String tempDistrict = etRegisterDistrict != null ? etRegisterDistrict.getText().toString().trim() : "";

        if (UserProfile.ROLE_LAWYER.equals(selectedRole) && !TextUtils.isEmpty(lawyerDistrict)) {

            tempDistrict = lawyerDistrict;

        }

        final String district = tempDistrict;

        String thana = etRegisterThana != null ? etRegisterThana.getText().toString().trim() : "";

        String unionWard = etRegisterUnion != null ? etRegisterUnion.getText().toString().trim() : "";

        String postOffice = etRegisterPost != null ? etRegisterPost.getText().toString().trim() : "";

        String villageRoad = etRegisterVillageRoad != null ? etRegisterVillageRoad.getText().toString().trim() : "";

        String photoUrl = selectedPhotoPath != null ? selectedPhotoPath : (selectedPhotoUri != null ? selectedPhotoUri.toString() : "");



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



        if (UserProfile.ROLE_LAWYER.equals(selectedRole)) {

            if (TextUtils.isEmpty(barNo)) {

                if (etLawyerBarNo != null) etLawyerBarNo.setError(isBn ? "বার কাউন্সিল সনদ নং দিন" : "Enter Bar Council Sanad No");

                return;

            }

            if (TextUtils.isEmpty(barAssociation)) {

                if (etLawyerBarAssociation != null) etLawyerBarAssociation.setError(isBn ? "বার সমিতি ও আদালতের নাম দিন" : "Enter Bar Association");

                return;

            }

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

                                    false,

                                    selectedRole

                            );



                            SavedAccountManager.saveAccount(AuthActivity.this, email, name, selectedRole, photoUrl, password);



                            getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE)

                                    .edit()

                                    .putBoolean("is_guest_mode", false)

                                    .putString("user_role", selectedRole)

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

                                    .set(profile.toMap(), SetOptions.merge());



                            if (UserProfile.ROLE_LAWYER.equals(selectedRole)) {

                                List<String> tagsList = new ArrayList<>();

                                if (!TextUtils.isEmpty(tagsInput)) {

                                    for (String t : tagsInput.split("[,|、]")) {

                                        String clean = t.trim();

                                        if (!clean.isEmpty()) tagsList.add(clean);

                                    }

                                }

                                int expYears = 0;

                                try { expYears = Integer.parseInt(expInput); } catch (Exception ignored) {}

                                double fee = 0.0;

                                try { fee = Double.parseDouble(feeInput); } catch (Exception ignored) {}



                                LawyerProfile lawyer = new LawyerProfile(

                                        user.getUid(),

                                        name,

                                        email,

                                        "",

                                        photoUrl,

                                        district,

                                        barNo,

                                        barAssociation,

                                        barAssociation,

                                        "অ্যাডভোকেট",

                                        tagsList,

                                        bio,

                                        expYears,

                                        fee,

                                        true,

                                        true,

                                        System.currentTimeMillis()

                                );



                                LawyerLocalStore.saveLawyer(AuthActivity.this, lawyer);

                                mFirestore.collection("lawyers")

                                        .document(user.getUid())

                                        .set(lawyer.toMap(), SetOptions.merge());

                            }



                            setLoading(false);

                            Toast.makeText(AuthActivity.this,

                                    isBn ? "নিবন্ধন সম্পন্ন হয়েছে!" : "Registration completed!",

                                    Toast.LENGTH_SHORT).show();

                            navigateToRoleDestination();

                        } else {

                            setLoading(false);

                            navigateToRoleDestination();

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

                        navigateToRoleDestination();

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

        navigateToRoleDestination();

    }



    private void handleAuthError(Exception e, String actionName) {

        boolean isBn = LanguageHelper.isBengali(this);

        String rawMsg = e != null ? e.getMessage() : "";



        if (rawMsg != null && (rawMsg.contains("CONFIGURATION_NOT_FOUND") || rawMsg.contains("OPERATION_NOT_ALLOWED"))) {

            new AlertDialog.Builder(this)

                    .setTitle(isBn ? "Firebase Authentication সক্রিয় নেই" : "Firebase Auth Not Activated")

                    .setMessage(isBn

                            ? "ফায়ারবেস প্রজেক্টে Email/Password Authentication চালু করা হয়নি। Firebase Console থেকে Authentication > Sign-in method-এ Email/Password চালু করুন।"

                            : "Email/Password Authentication is not enabled in Firebase Console. Please enable it in Authentication > Sign-in method.")

                    .setPositiveButton(isBn ? "বুঝেছি" : "OK", (d, w) -> d.dismiss())

                    .setNeutralButton(isBn ? "গেস্ট হিসেবে চালিয়ে যান" : "Continue as Guest", (d, w) -> {

                        d.dismiss();

                        enterGuestOfflineMode();

                    })

                    .show();

        } else if (e instanceof FirebaseAuthInvalidCredentialsException || (rawMsg != null && (rawMsg.contains("INVALID_LOGIN_CREDENTIALS") || rawMsg.contains("wrong-password") || rawMsg.contains("INVALID_PASSWORD")))) {

            String msg = isBn ? "ইমেইল অথবা পাসওয়ার্ড সঠিক নয়। অনুগ্রহ করে আবার চেষ্টা করুন।" : "Incorrect email or password. Please try again.";

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

            if (etLoginPassword != null) {

                etLoginPassword.setError(msg);

                etLoginPassword.requestFocus();

            }

        } else if (e instanceof FirebaseAuthInvalidUserException || (rawMsg != null && rawMsg.contains("USER_NOT_FOUND"))) {

            String msg = isBn ? "এই ইমেইলে কোনো অ্যাকাউন্ট পাওয়া যায়নি। অনুগ্রহ করে নিবন্ধন করুন।" : "No account found with this email. Please register.";

            new AlertDialog.Builder(this)

                    .setTitle(isBn ? "অ্যাকাউন্ট নেই" : "Account Not Found")

                    .setMessage(msg)

                    .setPositiveButton(isBn ? "নিবন্ধন করুন" : "Register", (dialog, which) -> {

                        switchTab(false);

                        if (etRegisterEmail != null && etLoginEmail != null) {

                            etRegisterEmail.setText(etLoginEmail.getText().toString().trim());

                        }

                    })

                    .setNegativeButton(isBn ? "বাতিল" : "Cancel", (dialog, which) -> dialog.dismiss())

                    .show();

        } else if (e instanceof FirebaseAuthUserCollisionException || (rawMsg != null && rawMsg.contains("EMAIL_EXISTS"))) {

            String msg = isBn ? "এই ইমেইলে ইতিমধ্যে একটি অ্যাকাউন্ট রয়েছে। অনুগ্রহ করে লগইন করুন।" : "An account already exists with this email. Please sign in.";

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

            switchTab(true);

            if (etLoginEmail != null && etRegisterEmail != null) {

                etLoginEmail.setText(etRegisterEmail.getText().toString().trim());

            }

        } else if (e instanceof FirebaseAuthWeakPasswordException || (rawMsg != null && rawMsg.contains("WEAK_PASSWORD"))) {

            String msg = isBn ? "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।" : "Password must be at least 6 characters.";

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

            if (etRegisterPassword != null) {

                etRegisterPassword.setError(msg);

                etRegisterPassword.requestFocus();

            }

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



    private void navigateToRoleDestination() {

        android.content.SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);

        String role = prefs.getString("user_role", UserProfile.ROLE_CITIZEN);



        Intent intent;

        if (UserProfile.ROLE_LAWYER.equalsIgnoreCase(role)) {

            intent = new Intent(AuthActivity.this, LawyerDashboardActivity.class);

        } else {

            intent = new Intent(AuthActivity.this, MainActivity.class);

        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish();

    }

}

