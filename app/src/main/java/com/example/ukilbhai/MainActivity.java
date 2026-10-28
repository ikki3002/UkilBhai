package com.example.ukilbhai;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ukilbhai.model.UserProfile;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.SettingsDialogHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import android.widget.ScrollView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    private ActivityResultLauncher<String> editPhotoPickerLauncher;
    private Uri tempEditPhotoUri = null;
    private ImageView currentEditDialogImageView = null;

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

        SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);

        if ((mAuth == null || mAuth.getCurrentUser() == null) && !isGuestMode) {
            redirectToAuth();
            return;
        }

        // Image picker for Edit Profile dialog
        editPhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        tempEditPhotoUri = uri;
                        if (currentEditDialogImageView != null) {
                            currentEditDialogImageView.setImageURI(uri);
                        }
                    }
                }
        );

        setContentView(R.layout.activity_main);

        setupProfileAvatar();
        setupMainMenuMoreButton();
        setupEmergencyButtons();
        setupAiChatButton();
        updateGuestBanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
        if ((mAuth == null || mAuth.getCurrentUser() == null) && !isGuestMode) {
            redirectToAuth();
        } else {
            updateGuestBanner();
        }
    }

    private void updateGuestBanner() {
        View layoutGuestBanner = findViewById(R.id.layoutGuestBanner);
        MaterialButton btnGuestBannerLogin = findViewById(R.id.btnGuestBannerLogin);
        TextView tvGuestBannerTitle = findViewById(R.id.tvGuestBannerTitle);
        TextView tvGuestBannerSubtitle = findViewById(R.id.tvGuestBannerSubtitle);

        FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
        SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        boolean isLocalGuest = prefs.getBoolean("is_guest_mode", false);
        boolean isAnonymous = (currentUser == null) || isLocalGuest || currentUser.isAnonymous();

        boolean isBn = LanguageHelper.isBengali(this);

        if (layoutGuestBanner != null) {
            if (isAnonymous) {
                layoutGuestBanner.setVisibility(View.VISIBLE);
                if (tvGuestBannerTitle != null) {
                    tvGuestBannerTitle.setText(isBn ? "অতিথি হিসেবে ব্যবহার করছেন" : "Browsing as Guest");
                }
                if (tvGuestBannerSubtitle != null) {
                    tvGuestBannerSubtitle.setText(isBn ? "আইনি প্রোফাইল সংরক্ষণে লগইন বা নিবন্ধন করুন" : "Sign in or register to save legal profile");
                }
                if (btnGuestBannerLogin != null) {
                    btnGuestBannerLogin.setText(isBn ? "লগইন / সাইন আপ" : "Login / Sign Up");
                    applyPressScale(btnGuestBannerLogin);
                    btnGuestBannerLogin.setOnClickListener(v -> redirectToAuth());
                }
                applyPressScale(layoutGuestBanner);
                layoutGuestBanner.setOnClickListener(v -> redirectToAuth());
            } else {
                layoutGuestBanner.setVisibility(View.GONE);
            }
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
        ImageView ivProfileAvatar = findViewById(R.id.ivProfileAvatar);

        SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
        String cachedPhoto = prefs.getString("profile_photo", "");
        if (ivProfileAvatar != null && !TextUtils.isEmpty(cachedPhoto)) {
            try {
                ivProfileAvatar.setImageURI(Uri.parse(cachedPhoto));
            } catch (Exception ignored) {}
        }

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
        try {
            FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
            SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
            if (currentUser != null && !currentUser.isAnonymous()) {
                prefs.edit().putBoolean("is_guest_mode", false).apply();
            }
            boolean isLocalGuest = prefs.getBoolean("is_guest_mode", false);
            boolean isAnonymous = (currentUser == null) || isLocalGuest || currentUser.isAnonymous();

            boolean isBn = LanguageHelper.isBengali(this);

            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_profile, null);
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
            View btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);
            ImageView ivDialogAvatar = dialogView.findViewById(R.id.ivDialogAvatar);
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
            MaterialButton btnDialogLoginRegister = dialogView.findViewById(R.id.btnDialogLoginRegister);
            MaterialButton btnDialogEditProfile = dialogView.findViewById(R.id.btnDialogEditProfile);
            MaterialButton btnDialogLogout = dialogView.findViewById(R.id.btnDialogLogout);

            if (tvDialogTitle != null) {
                tvDialogTitle.setText(isBn ? "ব্যবহারকারী প্রোফাইল" : "Citizen Profile");
            }
            if (btnDialogClose != null) {
                btnDialogClose.setOnClickListener(v -> dialog.dismiss());
            }

            // Immediately load cached profile data from local storage
            String cachedName = prefs.getString("profile_name", "");
            String cachedNid = prefs.getString("profile_nid", "");
            String cachedLocation = prefs.getString("profile_location", "");
            String cachedAddress = prefs.getString("profile_address", "");
            String cachedPhoto = prefs.getString("profile_photo", "");

            if (tvLabelNid != null) tvLabelNid.setText(isBn ? "জাতীয় পরিচয়পত্র:" : "National ID:");
            if (tvLabelLocation != null) tvLabelLocation.setText(isBn ? "থানা ও জেলা:" : "Thana & District:");
            if (tvLabelAddress != null) tvLabelAddress.setText(isBn ? "ঠিকানা:" : "Address:");

            String notSpecified = isBn ? "উল্লেখ নেই" : "Not specified";
            if (tvDialogNid != null) tvDialogNid.setText(!TextUtils.isEmpty(cachedNid) ? cachedNid : notSpecified);
            if (tvDialogLocation != null) tvDialogLocation.setText(!TextUtils.isEmpty(cachedLocation) ? cachedLocation : notSpecified);
            if (tvDialogAddress != null) tvDialogAddress.setText(!TextUtils.isEmpty(cachedAddress) ? cachedAddress : notSpecified);
            if (ivDialogAvatar != null && !TextUtils.isEmpty(cachedPhoto)) {
                try {
                    ivDialogAvatar.setImageURI(Uri.parse(cachedPhoto));
                } catch (Exception ignored) {}
            }

            // Legal details are available for viewing & editing
            if (layoutLegalDetails != null) layoutLegalDetails.setVisibility(View.VISIBLE);

            // Edit Profile Button is fully functional for all citizens
            if (btnDialogEditProfile != null) {
                btnDialogEditProfile.setVisibility(View.VISIBLE);
                btnDialogEditProfile.setText(isBn ? "তথ্য পরিবর্তন করুন" : "Edit Profile");
                btnDialogEditProfile.setOnClickListener(v -> {
                    showEditProfileDialog(currentUser, tvDialogName, tvDialogNid, tvDialogLocation, tvDialogAddress, ivDialogAvatar);
                });
            }

            if (isAnonymous) {
                if (tvDialogName != null) {
                    tvDialogName.setText(!TextUtils.isEmpty(cachedName) ? cachedName : (isBn ? "অতিথি নাগরিক" : "Guest Citizen"));
                }
                if (tvDialogEmail != null) tvDialogEmail.setText(isBn ? "গেস্ট সেশন সক্রিয়" : "Guest Session Active");
                if (tvDialogBadge != null) tvDialogBadge.setText(isBn ? "গেস্ট এক্সেস" : "Guest Access");

                // Prominent Login / Register Button for Guests
                if (btnDialogLoginRegister != null) {
                    btnDialogLoginRegister.setVisibility(View.VISIBLE);
                    btnDialogLoginRegister.setText(isBn ? "লগইন বা নিবন্ধন করুন" : "Sign In or Register");
                    btnDialogLoginRegister.setOnClickListener(v -> {
                        dialog.dismiss();
                        redirectToAuth();
                    });
                }

                if (btnDialogLogout != null) {
                    btnDialogLogout.setText(isBn ? "অতিথি সেশন বন্ধ করুন" : "Exit Guest Session");
                    btnDialogLogout.setOnClickListener(v -> {
                        dialog.dismiss();
                        prefs.edit().clear().apply();
                        if (mAuth != null) mAuth.signOut();
                        redirectToAuth();
                    });
                }
            } else {
                String displayName = !TextUtils.isEmpty(cachedName) ? cachedName : (currentUser != null ? currentUser.getDisplayName() : "");
                String email = currentUser != null ? currentUser.getEmail() : "";

                if (btnDialogLoginRegister != null) btnDialogLoginRegister.setVisibility(View.GONE);
                if (tvDialogName != null) tvDialogName.setText(!TextUtils.isEmpty(displayName) ? displayName : (isBn ? "নাগরিক" : "Citizen"));
                if (tvDialogEmail != null) tvDialogEmail.setText(!TextUtils.isEmpty(email) ? email : "--");
                if (tvDialogBadge != null) tvDialogBadge.setText(isBn ? "নিবন্ধিত নাগরিক" : "Registered Citizen");

                if (btnDialogLogout != null) {
                    btnDialogLogout.setText(isBn ? "লগআউট করুন" : "Log Out");
                    btnDialogLogout.setOnClickListener(v -> {
                        dialog.dismiss();
                        prefs.edit().clear().apply();
                        if (mAuth != null) mAuth.signOut();
                        Toast.makeText(MainActivity.this,
                                isBn ? "লগআউট সফল হয়েছে" : "Logged out successfully",
                                Toast.LENGTH_SHORT).show();
                        redirectToAuth();
                    });
                }
            }

            // Sync with Firestore in background if authenticated
            if (mFirestore != null && currentUser != null && !TextUtils.isEmpty(currentUser.getUid())) {
                mFirestore.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                DocumentSnapshot doc = task.getResult();
                                String name = doc.getString("name");
                                String nid = doc.getString("nid");
                                String location = doc.getString("location");
                                String address = doc.getString("address");
                                String photoUrl = doc.getString("photoUrl");

                                if (tvDialogName != null && !TextUtils.isEmpty(name)) tvDialogName.setText(name);
                                if (tvDialogNid != null && !TextUtils.isEmpty(nid)) tvDialogNid.setText(nid);
                                if (tvDialogLocation != null && !TextUtils.isEmpty(location)) tvDialogLocation.setText(location);
                                if (tvDialogAddress != null && !TextUtils.isEmpty(address)) tvDialogAddress.setText(address);
                                if (ivDialogAvatar != null && !TextUtils.isEmpty(photoUrl)) {
                                    try {
                                        ivDialogAvatar.setImageURI(Uri.parse(photoUrl));
                                    } catch (Exception ignored) {}
                                }

                                prefs.edit()
                                        .putString("profile_name", name != null ? name : "")
                                        .putString("profile_nid", nid != null ? nid : "")
                                        .putString("profile_location", location != null ? location : "")
                                        .putString("profile_address", address != null ? address : "")
                                        .putString("profile_photo", photoUrl != null ? photoUrl : "")
                                        .apply();
                            }
                        });
            }

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "প্রোফাইল খুলতে সমস্যা হয়েছে: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditProfileDialog(FirebaseUser user, TextView tvParentName, TextView tvParentNid,
                                       TextView tvParentLocation, TextView tvParentAddress, ImageView ivParentAvatar) {
        try {
            boolean isBn = LanguageHelper.isBengali(this);
            SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);

            View editView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
            AlertDialog editDialog = new MaterialAlertDialogBuilder(this)
                    .setView(editView)
                    .create();

            if (editDialog.getWindow() != null) {
                editDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            ScrollView editScrollView = editView.findViewById(R.id.editScrollView);
            TextView tvEditDialogTitle = editView.findViewById(R.id.tvEditDialogTitle);
            View btnEditDialogClose = editView.findViewById(R.id.btnEditDialogClose);
            MaterialCardView cardEditPhoto = editView.findViewById(R.id.cardEditPhoto);
            ImageView ivEditPhoto = editView.findViewById(R.id.ivEditPhoto);
            currentEditDialogImageView = ivEditPhoto;
            tempEditPhotoUri = null;

            EditText etEditName = editView.findViewById(R.id.etEditName);
            EditText etEditNid = editView.findViewById(R.id.etEditNid);
            EditText etEditDistrict = editView.findViewById(R.id.etEditDistrict);
            EditText etEditThana = editView.findViewById(R.id.etEditThana);
            EditText etEditUnion = editView.findViewById(R.id.etEditUnion);
            EditText etEditPost = editView.findViewById(R.id.etEditPost);
            EditText etEditVillageRoad = editView.findViewById(R.id.etEditVillageRoad);
            MaterialButton btnSaveEditProfile = editView.findViewById(R.id.btnSaveEditProfile);

            if (tvEditDialogTitle != null) {
                tvEditDialogTitle.setText(isBn ? "প্রোফাইল তথ্য পরিবর্তন" : "Edit Profile Details");
            }
            if (btnEditDialogClose != null) {
                btnEditDialogClose.setOnClickListener(v -> editDialog.dismiss());
            }

            // Pre-fill immediately from local cache
            String cachedName = prefs.getString("profile_name", "");
            String cachedNid = prefs.getString("profile_nid", "");
            String cachedDistrict = prefs.getString("profile_district", "");
            String cachedThana = prefs.getString("profile_thana", "");
            String cachedUnion = prefs.getString("profile_union", "");
            String cachedPost = prefs.getString("profile_post", "");
            String cachedVillage = prefs.getString("profile_village", "");
            String cachedPhoto = prefs.getString("profile_photo", "");

            if (etEditName != null) {
                if (!TextUtils.isEmpty(cachedName)) {
                    etEditName.setText(cachedName);
                } else if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
                    etEditName.setText(user.getDisplayName());
                }
            }
            if (etEditNid != null && !TextUtils.isEmpty(cachedNid)) etEditNid.setText(cachedNid);
            if (etEditDistrict != null && !TextUtils.isEmpty(cachedDistrict)) etEditDistrict.setText(cachedDistrict);
            if (etEditThana != null && !TextUtils.isEmpty(cachedThana)) etEditThana.setText(cachedThana);
            if (etEditUnion != null && !TextUtils.isEmpty(cachedUnion)) etEditUnion.setText(cachedUnion);
            if (etEditPost != null && !TextUtils.isEmpty(cachedPost)) etEditPost.setText(cachedPost);
            if (etEditVillageRoad != null && !TextUtils.isEmpty(cachedVillage)) etEditVillageRoad.setText(cachedVillage);

            if (ivEditPhoto != null && !TextUtils.isEmpty(cachedPhoto)) {
                try {
                    ivEditPhoto.setImageURI(Uri.parse(cachedPhoto));
                    tempEditPhotoUri = Uri.parse(cachedPhoto);
                } catch (Exception ignored) {}
            }

            // Pre-fill existing data from Firestore safely if online
            if (mFirestore != null && user != null && !TextUtils.isEmpty(user.getUid())) {
                mFirestore.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc != null && doc.exists()) {
                                String name = doc.getString("name");
                                String nid = doc.getString("nid");
                                String district = doc.getString("district");
                                String thana = doc.getString("thana");
                                String unionWard = doc.getString("unionWard");
                                String postOffice = doc.getString("postOffice");
                                String villageRoad = doc.getString("villageRoad");
                                String photoUrl = doc.getString("photoUrl");

                                if (etEditName != null && TextUtils.isEmpty(etEditName.getText()) && !TextUtils.isEmpty(name)) etEditName.setText(name);
                                if (etEditNid != null && TextUtils.isEmpty(etEditNid.getText()) && !TextUtils.isEmpty(nid)) etEditNid.setText(nid);
                                if (etEditDistrict != null && TextUtils.isEmpty(etEditDistrict.getText()) && !TextUtils.isEmpty(district)) etEditDistrict.setText(district);
                                if (etEditThana != null && TextUtils.isEmpty(etEditThana.getText()) && !TextUtils.isEmpty(thana)) etEditThana.setText(thana);
                                if (etEditUnion != null && TextUtils.isEmpty(etEditUnion.getText()) && !TextUtils.isEmpty(unionWard)) etEditUnion.setText(unionWard);
                                if (etEditPost != null && TextUtils.isEmpty(etEditPost.getText()) && !TextUtils.isEmpty(postOffice)) etEditPost.setText(postOffice);
                                if (etEditVillageRoad != null && TextUtils.isEmpty(etEditVillageRoad.getText()) && !TextUtils.isEmpty(villageRoad)) etEditVillageRoad.setText(villageRoad);

                                if (ivEditPhoto != null && tempEditPhotoUri == null && !TextUtils.isEmpty(photoUrl)) {
                                    try {
                                        ivEditPhoto.setImageURI(Uri.parse(photoUrl));
                                        tempEditPhotoUri = Uri.parse(photoUrl);
                                    } catch (Exception ignored) {}
                                }
                            }
                        });
            }

            if (cardEditPhoto != null) {
                cardEditPhoto.setOnClickListener(v -> {
                    if (editPhotoPickerLauncher != null) {
                        editPhotoPickerLauncher.launch("image/*");
                    }
                });
            }

            if (btnSaveEditProfile != null) {
                btnSaveEditProfile.setText(isBn ? "সংরক্ষণ করুন" : "Save Changes");
                btnSaveEditProfile.setOnClickListener(v -> {
                    String newName = etEditName != null ? etEditName.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        if (etEditName != null) {
                            etEditName.setError(isBn ? "নাম খালি রাখা যাবে না" : "Name cannot be empty");
                            etEditName.requestFocus();
                        }
                        if (editScrollView != null) {
                            editScrollView.smoothScrollTo(0, 0);
                        }
                        Toast.makeText(MainActivity.this,
                                isBn ? "অনুগ্রহ করে আপনার নাম লিখুন" : "Please enter your name",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newNid = etEditNid != null ? etEditNid.getText().toString().trim() : "";
                    String newDistrict = etEditDistrict != null ? etEditDistrict.getText().toString().trim() : "";
                    String newThana = etEditThana != null ? etEditThana.getText().toString().trim() : "";
                    String newUnion = etEditUnion != null ? etEditUnion.getText().toString().trim() : "";
                    String newPost = etEditPost != null ? etEditPost.getText().toString().trim() : "";
                    String newVillageRoad = etEditVillageRoad != null ? etEditVillageRoad.getText().toString().trim() : "";
                    String photoUriString = tempEditPhotoUri != null ? tempEditPhotoUri.toString() : prefs.getString("profile_photo", "");

                    String uid = user != null ? user.getUid() : "guest_" + System.currentTimeMillis();
                    String email = user != null && user.getEmail() != null ? user.getEmail() : "";
                    boolean isGuest = (user == null || user.isAnonymous());

                    UserProfile updatedProfile = new UserProfile(
                            uid,
                            newName,
                            email,
                            newNid,
                            photoUriString,
                            newDistrict,
                            newThana,
                            newUnion,
                            newPost,
                            newVillageRoad,
                            System.currentTimeMillis(),
                            isGuest
                    );

                    // 1. Persist locally immediately so changes are saved offline and instant
                    prefs.edit()
                            .putString("profile_name", newName)
                            .putString("profile_nid", newNid)
                            .putString("profile_district", newDistrict)
                            .putString("profile_thana", newThana)
                            .putString("profile_union", newUnion)
                            .putString("profile_post", newPost)
                            .putString("profile_village", newVillageRoad)
                            .putString("profile_location", updatedProfile.getLocation())
                            .putString("profile_address", updatedProfile.getAddress())
                            .putString("profile_photo", photoUriString)
                            .apply();

                    // 2. Refresh parent profile dialog views immediately
                    if (tvParentName != null) tvParentName.setText(newName);
                    if (tvParentNid != null) tvParentNid.setText(!TextUtils.isEmpty(newNid) ? newNid : (isBn ? "উল্লেখ নেই" : "Not specified"));
                    if (tvParentLocation != null) tvParentLocation.setText(!TextUtils.isEmpty(updatedProfile.getLocation()) ? updatedProfile.getLocation() : (isBn ? "উল্লেখ নেই" : "Not specified"));
                    if (tvParentAddress != null) tvParentAddress.setText(!TextUtils.isEmpty(updatedProfile.getAddress()) ? updatedProfile.getAddress() : (isBn ? "উল্লেখ নেই" : "Not specified"));
                    if (ivParentAvatar != null && !TextUtils.isEmpty(photoUriString)) {
                        try {
                            ivParentAvatar.setImageURI(Uri.parse(photoUriString));
                        } catch (Exception ignored) {}
                    }

                    // Update top bar avatar if exists
                    ImageView ivTopAvatar = findViewById(R.id.ivProfileAvatar);
                    if (ivTopAvatar != null && !TextUtils.isEmpty(photoUriString)) {
                        try {
                            ivTopAvatar.setImageURI(Uri.parse(photoUriString));
                        } catch (Exception ignored) {}
                    }

                    // 3. Update Auth display name & photo in background
                    if (user != null) {
                        try {
                            UserProfileChangeRequest.Builder updateReq = new UserProfileChangeRequest.Builder().setDisplayName(newName);
                            if (!TextUtils.isEmpty(photoUriString)) {
                                updateReq.setPhotoUri(Uri.parse(photoUriString));
                            }
                            user.updateProfile(updateReq.build());
                        } catch (Exception ignored) {}
                    }

                    // 4. Save to Firestore using resilient SetOptions.merge()
                    if (mFirestore != null && user != null && !TextUtils.isEmpty(user.getUid())) {
                        try {
                            mFirestore.collection("users").document(user.getUid())
                                    .set(updatedProfile.toMap(), SetOptions.merge());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // 5. Dismiss dialog and notify user with immediate success feedback
                    editDialog.dismiss();
                    Toast.makeText(MainActivity.this,
                            isBn ? "প্রোফাইল সফলভাবে আপডেট হয়েছে!" : "Profile updated successfully!",
                            Toast.LENGTH_SHORT).show();
                });
            }

            editDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "এডিট ডায়ালগ খুলতে সমস্যা হয়েছে: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
