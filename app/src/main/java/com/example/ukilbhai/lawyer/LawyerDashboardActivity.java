package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.AuthActivity;
import com.example.ukilbhai.ChatActivity;
import com.example.ukilbhai.R;
import com.example.ukilbhai.model.ConsultationRequest;
import com.example.ukilbhai.model.DirectChatRoom;
import com.example.ukilbhai.model.LawyerProfile;
import com.example.ukilbhai.rag.LegalStatuteExplorerActivity;
import com.example.ukilbhai.util.ImageStorageHelper;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.SettingsDialogHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LawyerDashboardActivity extends AppCompatActivity {

    private ImageView ivDashboardAvatar;
    private TextView tvDashboardName;
    private TextView tvDashboardBar;
    private ImageButton btnDashboardMenu;
    private SwitchMaterial switchLawyerOnline;
    private TextView tvOnlineStatusLabel;

    private TextView tabIncomingRequests;
    private TextView tabActiveChats;

    private LinearLayout layoutRequestsContainer;
    private ProgressBar progressRequests;
    private View layoutEmptyRequests;
    private RecyclerView rvIncomingRequests;
    private ConsultationRequestAdapter requestAdapter;

    private LinearLayout layoutActiveChatsContainer;
    private ProgressBar progressChats;
    private View layoutEmptyChats;
    private RecyclerView rvActiveChats;
    private DirectChatRoomAdapter chatRoomAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String currentUid;
    private LawyerProfile myProfile;

    private ListenerRegistration requestsListener;
    private ListenerRegistration chatsListener;
    private boolean isRequestsTab = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawyer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        currentUid = user.getUid();

        initViews();
        setupTabs();
        loadMyProfile();
        setupRequestsRecyclerView();
        setupChatsRecyclerView();
        subscribeToRequests();
        subscribeToChatRooms();
    }

    private void initViews() {
        ivDashboardAvatar = findViewById(R.id.ivDashboardAvatar);
        tvDashboardName = findViewById(R.id.tvDashboardName);
        tvDashboardBar = findViewById(R.id.tvDashboardBar);
        btnDashboardMenu = findViewById(R.id.btnDashboardMenu);
        switchLawyerOnline = findViewById(R.id.switchLawyerOnline);
        tvOnlineStatusLabel = findViewById(R.id.tvOnlineStatusLabel);

        tabIncomingRequests = findViewById(R.id.tabIncomingRequests);
        tabActiveChats = findViewById(R.id.tabActiveChats);

        layoutRequestsContainer = findViewById(R.id.layoutRequestsContainer);
        progressRequests = findViewById(R.id.progressRequests);
        layoutEmptyRequests = findViewById(R.id.layoutEmptyRequests);
        rvIncomingRequests = findViewById(R.id.rvIncomingRequests);

        layoutActiveChatsContainer = findViewById(R.id.layoutActiveChatsContainer);
        progressChats = findViewById(R.id.progressChats);
        layoutEmptyChats = findViewById(R.id.layoutEmptyChats);
        rvActiveChats = findViewById(R.id.rvActiveChats);

        View btnStatuteExplorer = findViewById(R.id.btnAdvocateStatuteExplorer);
        if (btnStatuteExplorer != null) {
            btnStatuteExplorer.setOnClickListener(v -> {
                Intent intent = new Intent(this, LegalStatuteExplorerActivity.class);
                intent.putExtra(LegalStatuteExplorerActivity.EXTRA_INITIAL_CATEGORY, "all");
                startActivity(intent);
            });
        }

        View btnAiAssistant = findViewById(R.id.btnAdvocateAiAssistant);
        if (btnAiAssistant != null) {
            btnAiAssistant.setOnClickListener(v -> {
                startActivity(new Intent(this, ChatActivity.class));
            });
        }

        switchLawyerOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateOnlineStatus(isChecked);
            if (isChecked) {
                tvOnlineStatusLabel.setText("🟢 ক্লায়েন্টের জন্য অনলাইন আছেন");
                tvOnlineStatusLabel.setTextColor(getResources().getColor(R.color.mint_accent));
            } else {
                tvOnlineStatusLabel.setText("⚪ অফলাইন (পরামর্শ বন্ধ)");
                tvOnlineStatusLabel.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        });

        btnDashboardMenu.setOnClickListener(v -> showDashboardMenu());
        ivDashboardAvatar.setOnClickListener(v -> showEditProfileDialog());
    }

    private void setupTabs() {
        tabIncomingRequests.setOnClickListener(v -> switchTab(true));
        tabActiveChats.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean requests) {
        isRequestsTab = requests;
        if (requests) {
            tabIncomingRequests.setBackgroundResource(R.drawable.bg_pill_emergency_green);
            tabIncomingRequests.setTextColor(getResources().getColor(R.color.mint_accent));

            tabActiveChats.setBackground(null);
            tabActiveChats.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutRequestsContainer.setVisibility(View.VISIBLE);
            layoutActiveChatsContainer.setVisibility(View.GONE);
        } else {
            tabActiveChats.setBackgroundResource(R.drawable.bg_pill_emergency_green);
            tabActiveChats.setTextColor(getResources().getColor(R.color.mint_accent));

            tabIncomingRequests.setBackground(null);
            tabIncomingRequests.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutActiveChatsContainer.setVisibility(View.VISIBLE);
            layoutRequestsContainer.setVisibility(View.GONE);
        }
    }

    private void loadMyProfile() {
        LawyerProfile cached = LawyerLocalStore.getLawyer(this, currentUid);
        if (cached != null) {
            myProfile = cached;
            bindProfileToHeader(myProfile);
        }

        mFirestore.collection("lawyers").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        myProfile = doc.toObject(LawyerProfile.class);
                        if (myProfile != null) {
                            bindProfileToHeader(myProfile);
                            LawyerLocalStore.saveLawyer(this, myProfile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LawyerDashboard", "Failed to fetch lawyer profile: " + e.getMessage());
                });
    }

    private void bindProfileToHeader(LawyerProfile profile) {
        tvDashboardName.setText(profile.getName() != null ? profile.getName() : "আইনজীবী");
        tvDashboardBar.setText((profile.getDesignation() != null ? profile.getDesignation() : "অ্যাডভোকেট")
                + " • " + (profile.getBarAssociation() != null ? profile.getBarAssociation() : "বার সমিতি"));
        ImageStorageHelper.loadAvatar(this, ivDashboardAvatar, profile.getPhotoUrl());
        switchLawyerOnline.setChecked(profile.isOnline());
    }

    private void setupRequestsRecyclerView() {
        rvIncomingRequests.setLayoutManager(new LinearLayoutManager(this));
        requestAdapter = new ConsultationRequestAdapter(this, new ConsultationRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(ConsultationRequest request) {
                acceptConsultationRequest(request);
            }

            @Override
            public void onDecline(ConsultationRequest request) {
                declineConsultationRequest(request);
            }
        });
        rvIncomingRequests.setAdapter(requestAdapter);
    }

    private void setupChatsRecyclerView() {
        rvActiveChats.setLayoutManager(new LinearLayoutManager(this));
        chatRoomAdapter = new DirectChatRoomAdapter(this, currentUid, true, room -> {
            Intent intent = new Intent(LawyerDashboardActivity.this, DirectChatActivity.class);
            intent.putExtra(DirectChatActivity.EXTRA_ROOM_ID, room.getRoomId());
            intent.putExtra(DirectChatActivity.EXTRA_OTHER_UID, room.getCitizenId());
            intent.putExtra(DirectChatActivity.EXTRA_OTHER_NAME, room.getCitizenName());
            intent.putExtra(DirectChatActivity.EXTRA_OTHER_PHOTO, room.getCitizenPhoto());
            intent.putExtra(DirectChatActivity.EXTRA_CATEGORY, room.getCaseCategory());
            startActivity(intent);
        });
        rvActiveChats.setAdapter(chatRoomAdapter);
    }

    private void subscribeToRequests() {
        progressRequests.setVisibility(View.VISIBLE);

        requestsListener = mFirestore.collection("consultation_requests")
                .whereEqualTo("lawyerId", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    progressRequests.setVisibility(View.GONE);
                    if (error != null || snapshots == null) return;

                    List<ConsultationRequest> list = new ArrayList<>();
                    int pendingCount = 0;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ConsultationRequest req = doc.toObject(ConsultationRequest.class);
                        if (req != null) {
                            list.add(req);
                            if (ConsultationRequest.STATUS_PENDING.equalsIgnoreCase(req.getStatus())) {
                                pendingCount++;
                            }
                        }
                    }

                    if (tabIncomingRequests != null) {
                        tabIncomingRequests.setText(pendingCount > 0 ? "নতুন আবেদন (" + pendingCount + ")" : "নতুন আবেদন");
                    }

                    requestAdapter.setRequests(list);
                    if (list.isEmpty()) {
                        layoutEmptyRequests.setVisibility(View.VISIBLE);
                        rvIncomingRequests.setVisibility(View.GONE);
                    } else {
                        layoutEmptyRequests.setVisibility(View.GONE);
                        rvIncomingRequests.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void subscribeToChatRooms() {
        progressChats.setVisibility(View.VISIBLE);

        chatsListener = mFirestore.collection("chat_rooms")
                .whereEqualTo("lawyerId", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    progressChats.setVisibility(View.GONE);
                    if (error != null || snapshots == null) return;

                    List<DirectChatRoom> list = new ArrayList<>();
                    int unreadTotal = 0;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        DirectChatRoom room = doc.toObject(DirectChatRoom.class);
                        if (room != null && room.isActive()) {
                            list.add(room);
                            unreadTotal += room.getUnreadLawyerCount();
                        }
                    }

                    if (tabActiveChats != null) {
                        tabActiveChats.setText(unreadTotal > 0 ? "সক্রিয় কনসাল্টেশন (" + unreadTotal + ")" : "সক্রিয় কনসাল্টেশন");
                    }

                    chatRoomAdapter.setRooms(list);
                    if (list.isEmpty()) {
                        layoutEmptyChats.setVisibility(View.VISIBLE);
                        rvActiveChats.setVisibility(View.GONE);
                    } else {
                        layoutEmptyChats.setVisibility(View.GONE);
                        rvActiveChats.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void acceptConsultationRequest(ConsultationRequest req) {
        String roomId = "chat_" + req.getCitizenId() + "_" + currentUid;

        DirectChatRoom room = new DirectChatRoom(
                roomId,
                req.getCitizenId(),
                req.getCitizenName(),
                req.getCitizenPhoto(),
                currentUid,
                myProfile != null ? myProfile.getName() : "আইনজীবী",
                myProfile != null ? myProfile.getPhotoUrl() : "",
                req.getCaseCategory()
        );

        mFirestore.collection("chat_rooms")
                .document(roomId)
                .set(room.toMap(), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", ConsultationRequest.STATUS_ACCEPTED);
                    updates.put("chatRoomId", roomId);
                    updates.put("respondedAt", System.currentTimeMillis());

                    mFirestore.collection("consultation_requests")
                            .document(req.getRequestId())
                            .update(updates);

                    Toast.makeText(this, "আবেদন গৃহীত হয়েছে! চ্যাট শুরু করা হচ্ছে...", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, DirectChatActivity.class);
                    intent.putExtra(DirectChatActivity.EXTRA_ROOM_ID, roomId);
                    intent.putExtra(DirectChatActivity.EXTRA_OTHER_UID, req.getCitizenId());
                    intent.putExtra(DirectChatActivity.EXTRA_OTHER_NAME, req.getCitizenName());
                    intent.putExtra(DirectChatActivity.EXTRA_OTHER_PHOTO, req.getCitizenPhoto());
                    intent.putExtra(DirectChatActivity.EXTRA_CATEGORY, req.getCaseCategory());
                    startActivity(intent);
                });
    }

    private void declineConsultationRequest(ConsultationRequest req) {
        mFirestore.collection("consultation_requests")
                .document(req.getRequestId())
                .update("status", ConsultationRequest.STATUS_DECLINED, "respondedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "আবেদন প্রত্যাখ্যান করা হয়েছে", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateOnlineStatus(boolean isOnline) {
        if (currentUid == null) return;
        Map<String, Object> presence = new HashMap<>();
        presence.put("isOnline", isOnline);
        presence.put("lastSeen", System.currentTimeMillis());

        if (myProfile != null) {
            myProfile.setOnline(isOnline);
            myProfile.setLastSeen((Long) presence.get("lastSeen"));
            LawyerLocalStore.saveLawyer(this, myProfile);
        }

        mFirestore.collection("lawyers").document(currentUid).set(presence, SetOptions.merge());
        mFirestore.collection("users").document(currentUid).set(presence, SetOptions.merge());
    }

    private void showDashboardMenu() {
        String[] options = {
                "প্রোফাইল ও পরামর্শ ফি সম্পাদনা",
                "📚 আইন ও ধারা ডিরেক্টরি (৩,৫১৮টি ধারা)",
                "⚙️ সেটিংস (থিম ও ভাষা)",
                "লগআউট"
        };
        new AlertDialog.Builder(this)
                .setTitle("আইনজীবী ড্যাশবোর্ড মেনু")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditProfileDialog();
                    } else if (which == 1) {
                        Intent intent = new Intent(this, LegalStatuteExplorerActivity.class);
                        intent.putExtra(LegalStatuteExplorerActivity.EXTRA_INITIAL_CATEGORY, "all");
                        startActivity(intent);
                    } else if (which == 2) {
                        SettingsDialogHelper.showSettingsDialog(this, this::recreate);
                    } else if (which == 3) {
                        performLogout();
                    }
                })
                .show();
    }

    private void showEditProfileDialog() {
        if (myProfile == null) return;

        int pad = (int) (getResources().getDisplayMetrics().density * 18);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad, pad, pad);
        scrollView.addView(container);

        final EditText etName = new EditText(this);
        etName.setHint("নাম");
        etName.setText(myProfile.getName());
        container.addView(etName);

        final EditText etBarAssoc = new EditText(this);
        etBarAssoc.setHint("বার সমিতি (যেমন: ঢাকা বার অ্যাসোসিয়েশন)");
        etBarAssoc.setText(myProfile.getBarAssociation());
        container.addView(etBarAssoc);

        final EditText etDistrict = new EditText(this);
        etDistrict.setHint("জেলা (যেমন: ঢাকা)");
        etDistrict.setText(myProfile.getDistrict());
        container.addView(etDistrict);

        final EditText etFee = new EditText(this);
        etFee.setHint("পরামর্শ ফি (৳)");
        etFee.setInputType(InputType.TYPE_CLASS_NUMBER);
        etFee.setText(String.valueOf((int) myProfile.getConsultationFee()));
        container.addView(etFee);

        final EditText etExp = new EditText(this);
        etExp.setHint("অভিজ্ঞতা (বছর)");
        etExp.setInputType(InputType.TYPE_CLASS_NUMBER);
        etExp.setText(String.valueOf(myProfile.getExperienceYears()));
        container.addView(etExp);

        final EditText etBio = new EditText(this);
        etBio.setHint("সংক্ষিপ্ত পরিচিতি ও চেম্বার ঠিকানা");
        etBio.setText(myProfile.getBio());
        container.addView(etBio);

        new AlertDialog.Builder(this)
                .setTitle("প্রোফাইল ও পরামর্শ ফি সম্পাদনা")
                .setView(scrollView)
                .setPositiveButton("সংরক্ষণ করুন", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newBar = etBarAssoc.getText().toString().trim();
                    String newDist = etDistrict.getText().toString().trim();
                    String feeStr = etFee.getText().toString().trim();
                    String expStr = etExp.getText().toString().trim();
                    String newBio = etBio.getText().toString().trim();

                    if (!newName.isEmpty()) myProfile.setName(newName);
                    if (!newBar.isEmpty()) myProfile.setBarAssociation(newBar);
                    if (!newDist.isEmpty()) myProfile.setDistrict(newDist);
                    if (!newBio.isEmpty()) myProfile.setBio(newBio);
                    try {
                        myProfile.setConsultationFee(Double.parseDouble(feeStr));
                    } catch (Exception ignored) {}
                    try {
                        myProfile.setExperienceYears(Integer.parseInt(expStr));
                    } catch (Exception ignored) {}

                    bindProfileToHeader(myProfile);
                    LawyerLocalStore.saveLawyer(this, myProfile);
                    mFirestore.collection("lawyers").document(currentUid)
                            .set(myProfile.toMap(), SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "প্রোফাইল আপডেট হয়েছে ✓", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void performLogout() {
        updateOnlineStatus(false);
        mAuth.signOut();
        getSharedPreferences("ukilbhai_local_session", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (switchLawyerOnline != null && switchLawyerOnline.isChecked()) {
            updateOnlineStatus(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
        if (chatsListener != null) chatsListener.remove();
        updateOnlineStatus(false);
    }
}
