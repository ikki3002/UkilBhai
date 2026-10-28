package com.example.ukilbhai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
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
        SharedPreferences prefs = getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE);
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
        MaterialButton btnDialogEditProfile = dialogView.findViewById(R.id.btnDialogEditProfile);
        MaterialButton btnDialogLogout = dialogView.findViewById(R.id.btnDialogLogout);

        tvDialogTitle.setText(isBn ? "ব্যবহারকারী প্রোফাইল" : "Citizen Profile");
        btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        boolean isAnonymous = isLocalGuest || (currentUser != null && currentUser.isAnonymous());
        if (isAnonymous) {
            tvDialogName.setText(isBn ? "অতিথি নাগরিক" : "Guest Citizen");
            tvDialogEmail.setText(isBn ? "গেস্ট সেশন সক্রিয়" : "Guest Session Active");
            tvDialogBadge.setText(isBn ? "গেস্ট এক্সেস" : "Guest Access");
            layoutLegalDetails.setVisibility(View.GONE);

            btnDialogEditProfile.setVisibility(View.GONE);
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
            btnDialogEditProfile.setVisibility(View.VISIBLE);

            tvLabelNid.setText(isBn ? "জাতীয় পরিচয়পত্র:" : "National ID:");
            tvLabelLocation.setText(isBn ? "থানা ও জেলা:" : "Thana & District:");
            tvLabelAddress.setText(isBn ? "ঠিকানা:" : "Address:");
            btnDialogEditProfile.setText(isBn ? "তথ্য পরিবর্তন করুন" : "Edit Profile");

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
                            String photoUrl = doc.getString("photoUrl");

                            if (!TextUtils.isEmpty(nid)) tvDialogNid.setText(nid);
                            if (!TextUtils.isEmpty(location)) tvDialogLocation.setText(location);
                            if (!TextUtils.isEmpty(address)) tvDialogAddress.setText(address);
                            if (!TextUtils.isEmpty(photoUrl)) {
                                try {
                                    ivDialogAvatar.setImageURI(Uri.parse(photoUrl));
                                } catch (Exception ignored) {}
                            }
                        }
                    });

            btnDialogEditProfile.setOnClickListener(v -> {
                showEditProfileDialog(currentUser, tvDialogName, tvDialogNid, tvDialogLocation, tvDialogAddress, ivDialogAvatar);
            });

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

        dialog.show();
    }

    private void showEditProfileDialog(FirebaseUser user, TextView tvParentName, TextView tvParentNid,
                                       TextView tvParentLocation, TextView tvParentAddress, ImageView ivParentAvatar) {
        boolean isBn = LanguageHelper.isBengali(this);

        View editView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        AlertDialog editDialog = new AlertDialog.Builder(this)
                .setView(editView)
                .create();

        if (editDialog.getWindow() != null) {
            editDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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

        tvEditDialogTitle.setText(isBn ? "প্রোফাইল তথ্য পরিবর্তন" : "Edit Profile Details");
        btnEditDialogClose.setOnClickListener(v -> editDialog.dismiss());

        etEditName.setText(user.getDisplayName());

        // Pre-fill existing data from Firestore
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

                        if (!TextUtils.isEmpty(name)) etEditName.setText(name);
                        if (!TextUtils.isEmpty(nid)) etEditNid.setText(nid);
                        if (!TextUtils.isEmpty(district)) etEditDistrict.setText(district);
                        if (!TextUtils.isEmpty(thana)) etEditThana.setText(thana);
                        if (!TextUtils.isEmpty(unionWard)) etEditUnion.setText(unionWard);
                        if (!TextUtils.isEmpty(postOffice)) etEditPost.setText(postOffice);
                        if (!TextUtils.isEmpty(villageRoad)) etEditVillageRoad.setText(villageRoad);

                        if (!TextUtils.isEmpty(photoUrl)) {
                            try {
                                ivEditPhoto.setImageURI(Uri.parse(photoUrl));
                            } catch (Exception ignored) {}
                        }
                    }
                });

        cardEditPhoto.setOnClickListener(v -> {
            if (editPhotoPickerLauncher != null) {
                editPhotoPickerLauncher.launch("image/*");
            }
        });

        btnSaveEditProfile.setText(isBn ? "সংরক্ষণ করুন" : "Save Changes");
        btnSaveEditProfile.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                etEditName.setError(isBn ? "নাম খালি রাখা যাবে না" : "Name cannot be empty");
                return;
            }

            String newNid = etEditNid.getText().toString().trim();
            String newDistrict = etEditDistrict.getText().toString().trim();
            String newThana = etEditThana.getText().toString().trim();
            String newUnion = etEditUnion.getText().toString().trim();
            String newPost = etEditPost.getText().toString().trim();
            String newVillageRoad = etEditVillageRoad.getText().toString().trim();
            String photoUriString = tempEditPhotoUri != null ? tempEditPhotoUri.toString() : "";

            btnSaveEditProfile.setEnabled(false);

            UserProfile updatedProfile = new UserProfile(
                    user.getUid(),
                    newName,
                    user.getEmail(),
                    newNid,
                    photoUriString,
                    newDistrict,
                    newThana,
                    newUnion,
                    newPost,
                    newVillageRoad,
                    System.currentTimeMillis(),
                    false
            );

            // Update Auth display name
            UserProfileChangeRequest.Builder updateReq = new UserProfileChangeRequest.Builder().setDisplayName(newName);
            if (tempEditPhotoUri != null) {
                updateReq.setPhotoUri(tempEditPhotoUri);
            }
            user.updateProfile(updateReq.build());

            // Save to Firestore
            mFirestore.collection("users").document(user.getUid())
                    .update(updatedProfile.toMap())
                    .addOnCompleteListener(task -> {
                        btnSaveEditProfile.setEnabled(true);
                        editDialog.dismiss();

                        // Refresh parent profile dialog views
                        tvParentName.setText(newName);
                        if (!TextUtils.isEmpty(newNid)) tvParentNid.setText(newNid);
                        if (!TextUtils.isEmpty(updatedProfile.getLocation())) tvParentLocation.setText(updatedProfile.getLocation());
                        if (!TextUtils.isEmpty(updatedProfile.getAddress())) tvParentAddress.setText(updatedProfile.getAddress());
                        if (tempEditPhotoUri != null) {
                            ivParentAvatar.setImageURI(tempEditPhotoUri);
                        }

                        Toast.makeText(MainActivity.this,
                                isBn ? "প্রোফাইল সফলভাবে আপডেট হয়েছে!" : "Profile updated successfully!",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        editDialog.show();
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
