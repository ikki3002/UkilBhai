package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.AuthActivity;
import com.example.ukilbhai.R;
import com.example.ukilbhai.model.ConsultationRequest;
import com.example.ukilbhai.model.DirectChatRoom;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CitizenConsultationsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tabActiveChats;
    private TextView tabSentRequests;

    private LinearLayout layoutActiveChatsContainer;
    private ProgressBar progressChats;
    private View layoutEmptyChats;
    private RecyclerView rvActiveChats;
    private MaterialButton btnFindLawyerEmpty;
    private DirectChatRoomAdapter chatRoomAdapter;

    private LinearLayout layoutRequestsContainer;
    private ProgressBar progressRequests;
    private View layoutEmptyRequests;
    private RecyclerView rvSentRequests;
    private ConsultationRequestAdapter requestAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String currentUid;

    private ListenerRegistration chatsListener;
    private ListenerRegistration requestsListener;
    private boolean isChatsTab = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_consultations);

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
        setupChatsRecyclerView();
        setupRequestsRecyclerView();
        subscribeToActiveChats();
        subscribeToSentRequests();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tabActiveChats = findViewById(R.id.tabActiveChats);
        tabSentRequests = findViewById(R.id.tabSentRequests);

        layoutActiveChatsContainer = findViewById(R.id.layoutActiveChatsContainer);
        progressChats = findViewById(R.id.progressChats);
        layoutEmptyChats = findViewById(R.id.layoutEmptyChats);
        rvActiveChats = findViewById(R.id.rvActiveChats);
        btnFindLawyerEmpty = findViewById(R.id.btnFindLawyerEmpty);

        layoutRequestsContainer = findViewById(R.id.layoutRequestsContainer);
        progressRequests = findViewById(R.id.progressRequests);
        layoutEmptyRequests = findViewById(R.id.layoutEmptyRequests);
        rvSentRequests = findViewById(R.id.rvSentRequests);

        btnBack.setOnClickListener(v -> finish());
        btnFindLawyerEmpty.setOnClickListener(v -> {
            startActivity(new Intent(CitizenConsultationsActivity.this, LawyerDirectoryActivity.class));
            finish();
        });
    }

    private void setupTabs() {
        tabActiveChats.setOnClickListener(v -> switchTab(true));
        tabSentRequests.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean chats) {
        isChatsTab = chats;
        if (chats) {
            tabActiveChats.setBackgroundResource(R.drawable.bg_pill_emergency_green);
            tabActiveChats.setTextColor(getResources().getColor(R.color.mint_accent));

            tabSentRequests.setBackground(null);
            tabSentRequests.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutActiveChatsContainer.setVisibility(View.VISIBLE);
            layoutRequestsContainer.setVisibility(View.GONE);
        } else {
            tabSentRequests.setBackgroundResource(R.drawable.bg_pill_emergency_green);
            tabSentRequests.setTextColor(getResources().getColor(R.color.mint_accent));

            tabActiveChats.setBackground(null);
            tabActiveChats.setTextColor(getResources().getColor(R.color.text_secondary));

            layoutRequestsContainer.setVisibility(View.VISIBLE);
            layoutActiveChatsContainer.setVisibility(View.GONE);
        }
    }

    private void setupChatsRecyclerView() {
        rvActiveChats.setLayoutManager(new LinearLayoutManager(this));
        chatRoomAdapter = new DirectChatRoomAdapter(this, currentUid, false, room -> {
            Intent intent = new Intent(CitizenConsultationsActivity.this, DirectChatActivity.class);
            intent.putExtra(DirectChatActivity.EXTRA_ROOM_ID, room.getRoomId());
            intent.putExtra(DirectChatActivity.EXTRA_OTHER_UID, room.getLawyerId());
            intent.putExtra(DirectChatActivity.EXTRA_OTHER_NAME, room.getLawyerName());
            intent.putExtra(DirectChatActivity.EXTRA_OTHER_PHOTO, room.getLawyerPhoto());
            intent.putExtra(DirectChatActivity.EXTRA_CATEGORY, room.getCaseCategory());
            startActivity(intent);
        });
        rvActiveChats.setAdapter(chatRoomAdapter);
    }

    private void setupRequestsRecyclerView() {
        rvSentRequests.setLayoutManager(new LinearLayoutManager(this));
        requestAdapter = new ConsultationRequestAdapter(this, true, req -> {
            if (ConsultationRequest.STATUS_ACCEPTED.equalsIgnoreCase(req.getStatus())) {
                String roomId = req.getChatRoomId();
                if (TextUtils.isEmpty(roomId)) {
                    roomId = "chat_" + currentUid + "_" + req.getLawyerId();
                }
                Intent intent = new Intent(CitizenConsultationsActivity.this, DirectChatActivity.class);
                intent.putExtra(DirectChatActivity.EXTRA_ROOM_ID, roomId);
                intent.putExtra(DirectChatActivity.EXTRA_OTHER_UID, req.getLawyerId());
                intent.putExtra(DirectChatActivity.EXTRA_OTHER_NAME, req.getLawyerName());
                intent.putExtra(DirectChatActivity.EXTRA_OTHER_PHOTO, req.getLawyerPhoto());
                intent.putExtra(DirectChatActivity.EXTRA_CATEGORY, req.getCaseCategory());
                startActivity(intent);
            } else if (ConsultationRequest.STATUS_PENDING.equalsIgnoreCase(req.getStatus())) {
                Toast.makeText(this, "আইনজীবী এখনও আবেদনটি গ্রহণ করেননি। অনুগ্রহ করে অপেক্ষা করুন।", Toast.LENGTH_SHORT).show();
            } else if (ConsultationRequest.STATUS_DECLINED.equalsIgnoreCase(req.getStatus())) {
                Toast.makeText(this, "এই মুহূর্তে আইনজীবী আবেদনটি গ্রহণ করতে পারেননি। অন্য আইনজীবী খুঁজুন।", Toast.LENGTH_SHORT).show();
            }
        });
        rvSentRequests.setAdapter(requestAdapter);
    }

    private void subscribeToActiveChats() {
        progressChats.setVisibility(View.VISIBLE);

        chatsListener = mFirestore.collection("chat_rooms")
                .whereEqualTo("citizenId", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    progressChats.setVisibility(View.GONE);
                    if (error != null) {
                        android.util.Log.e("CitizenConsultations", "Error loading chats: " + error.getMessage(), error);
                        return;
                    }
                    if (snapshots == null) return;

                    List<DirectChatRoom> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        DirectChatRoom room = doc.toObject(DirectChatRoom.class);
                        if (room != null && room.isActive()) {
                            list.add(room);
                        }
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

    private void subscribeToSentRequests() {
        progressRequests.setVisibility(View.VISIBLE);

        requestsListener = mFirestore.collection("consultation_requests")
                .whereEqualTo("citizenId", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    progressRequests.setVisibility(View.GONE);
                    if (error != null) {
                        android.util.Log.e("CitizenConsultations", "Error loading requests: " + error.getMessage(), error);
                        return;
                    }
                    if (snapshots == null) return;

                    List<ConsultationRequest> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ConsultationRequest req = doc.toObject(ConsultationRequest.class);
                        if (req != null) {
                            list.add(req);
                        }
                    }

                    requestAdapter.setRequests(list);
                    if (list.isEmpty()) {
                        layoutEmptyRequests.setVisibility(View.VISIBLE);
                        rvSentRequests.setVisibility(View.GONE);
                    } else {
                        layoutEmptyRequests.setVisibility(View.GONE);
                        rvSentRequests.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) chatsListener.remove();
        if (requestsListener != null) requestsListener.remove();
    }
}
