package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.model.DirectChatMessage;
import com.example.ukilbhai.util.ImageStorageHelper;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectChatActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "extra_room_id";
    public static final String EXTRA_OTHER_UID = "extra_other_uid";
    public static final String EXTRA_OTHER_NAME = "extra_other_name";
    public static final String EXTRA_OTHER_PHOTO = "extra_other_photo";
    public static final String EXTRA_CATEGORY = "extra_category";

    private String roomId;
    private String otherUid;
    private String otherName;
    private String otherPhoto;
    private String caseCategory;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String currentUid;
    private String currentUserName;

    private ImageButton btnBack;
    private ImageView ivUserAvatar;
    private View viewOnlineIndicator;
    private TextView tvChatName;
    private TextView tvChatStatus;
    private MaterialButton btnEndConsultation;
    private TextView tvCaseCategoryBadge;

    private RecyclerView rvDirectMessages;
    private DirectChatAdapter chatAdapter;
    private EditText etMessageInput;
    private MaterialCardView btnSend;
    private ImageButton btnAttach;

    private ListenerRegistration messagesListener;
    private ListenerRegistration presenceListener;
    private ActivityResultLauncher<String> attachmentPickerLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_chat);

        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        otherUid = getIntent().getStringExtra(EXTRA_OTHER_UID);
        otherName = getIntent().getStringExtra(EXTRA_OTHER_NAME);
        otherPhoto = getIntent().getStringExtra(EXTRA_OTHER_PHOTO);
        caseCategory = getIntent().getStringExtra(EXTRA_CATEGORY);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUid = currentUser.getUid();
            currentUserName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "ইউজার";
        }

        initViews();
        setupListeners();
        subscribeToMessages();
        subscribeToOtherUserPresence();
        markMessagesAsRead();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        viewOnlineIndicator = findViewById(R.id.viewOnlineIndicator);
        tvChatName = findViewById(R.id.tvChatName);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        btnEndConsultation = findViewById(R.id.btnEndConsultation);
        tvCaseCategoryBadge = findViewById(R.id.tvCaseCategoryBadge);

        rvDirectMessages = findViewById(R.id.rvDirectMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);

        tvChatName.setText(otherName != null ? otherName : "আইনজীবী চ্যাট");
        if (!TextUtils.isEmpty(caseCategory)) {
            tvCaseCategoryBadge.setVisibility(View.VISIBLE);
            tvCaseCategoryBadge.setText("বিষয়: " + caseCategory);
        } else {
            tvCaseCategoryBadge.setVisibility(View.GONE);
        }

        ImageStorageHelper.loadAvatar(this, ivUserAvatar, otherPhoto);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvDirectMessages.setLayoutManager(layoutManager);

        chatAdapter = new DirectChatAdapter(this, currentUid);
        rvDirectMessages.setAdapter(chatAdapter);

        attachmentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        sendAttachmentMessage(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = etMessageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text, null, "none");
                etMessageInput.setText("");
            }
        });

        btnAttach.setOnClickListener(v -> {
            if (attachmentPickerLauncher != null) {
                attachmentPickerLauncher.launch("image/*");
            }
        });

        btnEndConsultation.setOnClickListener(v -> confirmEndConsultation());
    }

    private void sendMessage(String text, String attachmentUrl, String attachmentType) {
        if (TextUtils.isEmpty(roomId) || currentUid == null) return;

        long now = System.currentTimeMillis();
        String messageId = mFirestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .document().getId();

        DirectChatMessage msg = new DirectChatMessage(
                messageId,
                currentUid,
                currentUserName,
                text,
                now,
                false,
                attachmentUrl,
                attachmentType
        );

        mFirestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .document(messageId)
                .set(msg.toMap());

        // Update chat room metadata
        Map<String, Object> roomUpdates = new HashMap<>();
        roomUpdates.put("lastMessage", text != null && !text.isEmpty() ? text : "[ছবি]");
        roomUpdates.put("lastMessageTime", now);
        roomUpdates.put("lastSenderId", currentUid);
        roomUpdates.put("isActive", true);

        mFirestore.collection("chat_rooms").document(roomId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String citizenId = doc.getString("citizenId");
                        if (currentUid.equals(citizenId)) {
                            roomUpdates.put("unreadLawyerCount", FieldValue.increment(1));
                        } else {
                            roomUpdates.put("unreadCitizenCount", FieldValue.increment(1));
                        }
                    }
                    mFirestore.collection("chat_rooms")
                            .document(roomId)
                            .set(roomUpdates, SetOptions.merge());
                })
                .addOnFailureListener(e -> {
                    mFirestore.collection("chat_rooms")
                            .document(roomId)
                            .set(roomUpdates, SetOptions.merge());
                });
    }

    private void sendAttachmentMessage(Uri uri) {
        String localPath = ImageStorageHelper.saveImageToInternalStorage(this, uri);
        String finalPath = localPath != null ? localPath : uri.toString();
        sendMessage("[আইনি নথি/ছবি সংযুক্ত করা হয়েছে]", finalPath, "image");
    }

    private void subscribeToMessages() {
        if (TextUtils.isEmpty(roomId)) return;

        messagesListener = mFirestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<DirectChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        DirectChatMessage msg = doc.toObject(DirectChatMessage.class);
                        if (msg != null) {
                            list.add(msg);
                        }
                    }
                    chatAdapter.setMessages(list);
                    if (!list.isEmpty()) {
                        rvDirectMessages.scrollToPosition(list.size() - 1);
                    }
                });
    }

    private void subscribeToOtherUserPresence() {
        if (TextUtils.isEmpty(otherUid)) return;

        presenceListener = mFirestore.collection("users")
                .document(otherUid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) {
                        // Check lawyers collection if user document didn't have it
                        mFirestore.collection("lawyers").document(otherUid).get()
                                .addOnSuccessListener(ldoc -> {
                                    if (ldoc.exists()) {
                                        Boolean isOnline = ldoc.getBoolean("isOnline");
                                        updatePresenceUI(isOnline != null && isOnline);
                                    }
                                });
                        return;
                    }

                    Boolean isOnline = doc.getBoolean("isOnline");
                    updatePresenceUI(isOnline != null && isOnline);
                });
    }

    private void updatePresenceUI(boolean isOnline) {
        if (isOnline) {
            viewOnlineIndicator.setVisibility(View.VISIBLE);
            tvChatStatus.setText("🟢 অনলাইনে আছেন");
            tvChatStatus.setTextColor(getResources().getColor(R.color.mint_accent));
        } else {
            viewOnlineIndicator.setVisibility(View.GONE);
            tvChatStatus.setText("অফলাইন");
            tvChatStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void markMessagesAsRead() {
        if (TextUtils.isEmpty(roomId) || currentUid == null) return;

        mFirestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        if (senderId != null && !senderId.equals(currentUid)) {
                            doc.getReference().update("read", true);
                        }
                    }
                });

        mFirestore.collection("chat_rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String citizenId = doc.getString("citizenId");
                        Map<String, Object> reset = new HashMap<>();
                        if (currentUid.equals(citizenId)) {
                            reset.put("unreadCitizenCount", 0);
                        } else {
                            reset.put("unreadLawyerCount", 0);
                        }
                        mFirestore.collection("chat_rooms").document(roomId).set(reset, SetOptions.merge());
                    }
                });
    }

    private void confirmEndConsultation() {
        new AlertDialog.Builder(this)
                .setTitle("পরামর্শ সমাপ্ত করুন")
                .setMessage("আপনি কি এই কনসাল্টেশন সেশনটি সমাপ্ত করতে চান?")
                .setPositiveButton("হ্যাঁ, সমাপ্ত করুন", (dialog, which) -> {
                    if (!TextUtils.isEmpty(roomId)) {
                        mFirestore.collection("chat_rooms")
                                .document(roomId)
                                .update("isActive", false);
                        Toast.makeText(DirectChatActivity.this, "পরামর্শ সেশন সমাপ্ত হয়েছে", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("না", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMyPresence(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateMyPresence(false);
    }

    private void updateMyPresence(boolean online) {
        if (currentUid != null) {
            Map<String, Object> presence = new HashMap<>();
            presence.put("isOnline", online);
            presence.put("lastSeen", System.currentTimeMillis());

            mFirestore.collection("users").document(currentUid).set(presence, SetOptions.merge());
            mFirestore.collection("lawyers").document(currentUid).set(presence, SetOptions.merge());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
        if (presenceListener != null) presenceListener.remove();
        updateMyPresence(false);
    }
}
