package com.example.ukilbhai.lawyer;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.model.ConsultationRequest;
import com.example.ukilbhai.model.LawyerProfile;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LawyerDirectoryActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvLawyerCount;
    private EditText etSearchLawyer;
    private Spinner spinnerDistrictFilter;
    private SwitchMaterial switchOnlineOnly;
    private ProgressBar progressLawyers;
    private View layoutEmptyLawyers;
    private RecyclerView rvLawyers;

    private LawyerAdapter lawyerAdapter;
    private final List<LawyerProfile> allLawyers = new ArrayList<>();

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private ListenerRegistration lawyersListener;

    private String selectedDistrict = "সকল জেলা (All)";
    private boolean onlineOnly = false;
    private String searchQuery = "";

    private static final String[] DISTRICTS = {
            "সকল জেলা (All)",
            "ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী", "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ",
            "কুমিল্লা", "গাজীপুর", "নারায়ণগঞ্জ", "বগুড়া", "কক্সবাজার", "নোয়াখালী", "ফরিদপুর"
    };

    private static final String[] CATEGORIES = {
            "জমিজমা ও দেওয়ানি বিরোধ",
            "পারিবারিক ও দেনমোহর",
            "ফৌজদারি মামলা ও জামিন",
            "চেক ডিজঅনার (NI Act)",
            "শ্রম আইন ও কারখানা বিরোধ",
            "সাইবার ক্রাইম ও ডিজিটাল নিরাপত্তা",
            "ভাড়াটিয়া ও বাড়িওয়ালা বিরোধ",
            "অন্যান্য আইনি সহায়তা"
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawyer_directory);

        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupFilters();
        loadLawyersFromFirestore();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvLawyerCount = findViewById(R.id.tvLawyerCount);
        etSearchLawyer = findViewById(R.id.etSearchLawyer);
        spinnerDistrictFilter = findViewById(R.id.spinnerDistrictFilter);
        switchOnlineOnly = findViewById(R.id.switchOnlineOnly);
        progressLawyers = findViewById(R.id.progressLawyers);
        layoutEmptyLawyers = findViewById(R.id.layoutEmptyLawyers);
        rvLawyers = findViewById(R.id.rvLawyers);

        btnBack.setOnClickListener(v -> finish());

        rvLawyers.setLayoutManager(new LinearLayoutManager(this));
        lawyerAdapter = new LawyerAdapter(this, this::showApplyDialog);
        rvLawyers.setAdapter(lawyerAdapter);
    }

    private void setupFilters() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, DISTRICTS);
        spinnerDistrictFilter.setAdapter(districtAdapter);

        spinnerDistrictFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDistrict = DISTRICTS[position];
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        switchOnlineOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlineOnly = isChecked;
            applyFilter();
        });

        etSearchLawyer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadLawyersFromFirestore() {
        // 1. Instant local render (0 ms latency) from resilient cache
        List<LawyerProfile> cached = LawyerLocalStore.getAllCachedLawyers(this);
        if (!cached.isEmpty()) {
            allLawyers.clear();
            allLawyers.addAll(cached);
            applyFilter();
        }

        progressLawyers.setVisibility(View.VISIBLE);

        lawyersListener = mFirestore.collection("lawyers")
                .addSnapshotListener((snapshots, error) -> {
                    progressLawyers.setVisibility(View.GONE);
                    if (error != null) {
                        android.util.Log.e("LawyerDirectory", "Firestore snapshot error: " + error.getMessage(), error);
                        // If error occurred (e.g. PERMISSION_DENIED / SERVICE_DISABLED / offline),
                        // ensure we still have our rich cached and default verified lawyers!
                        if (allLawyers.isEmpty()) {
                            allLawyers.addAll(LawyerLocalStore.getAllCachedLawyers(LawyerDirectoryActivity.this));
                            applyFilter();
                        }
                        return;
                    }
                    if (snapshots == null) return;

                    // Merge remote Firestore lawyers with local cache so newly registered lawyers appear
                    Map<String, LawyerProfile> lawyerMap = new LinkedHashMap<>();

                    // Start with local store
                    for (LawyerProfile lp : LawyerLocalStore.getAllCachedLawyers(LawyerDirectoryActivity.this)) {
                        if (lp != null && lp.getUid() != null) {
                            lawyerMap.put(lp.getUid(), lp);
                        }
                    }

                    // Firestore updates take precedence
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        LawyerProfile lawyer = doc.toObject(LawyerProfile.class);
                        if (lawyer != null && !TextUtils.isEmpty(lawyer.getUid())) {
                            lawyerMap.put(lawyer.getUid(), lawyer);
                        }
                    }

                    allLawyers.clear();
                    allLawyers.addAll(lawyerMap.values());

                    if (allLawyers.isEmpty()) {
                        allLawyers.addAll(LawyerLocalStore.getDefaultVerifiedLawyers());
                    }

                    // Save merged set back to local store
                    LawyerLocalStore.saveAllLawyers(LawyerDirectoryActivity.this, allLawyers);

                    applyFilter();
                });
    }

    private void applyFilter() {
        List<LawyerProfile> filtered = new ArrayList<>();

        for (LawyerProfile lawyer : allLawyers) {
            // Online filter
            if (onlineOnly && !lawyer.isOnline()) {
                continue;
            }

            // District filter
            if (!"সকল জেলা (All)".equals(selectedDistrict)) {
                if (lawyer.getDistrict() == null || !lawyer.getDistrict().contains(selectedDistrict)) {
                    continue;
                }
            }

            // Search query filter
            if (!TextUtils.isEmpty(searchQuery)) {
                String name = lawyer.getName() != null ? lawyer.getName().toLowerCase() : "";
                String bar = lawyer.getBarAssociation() != null ? lawyer.getBarAssociation().toLowerCase() : "";
                String bio = lawyer.getBio() != null ? lawyer.getBio().toLowerCase() : "";
                String tags = lawyer.getPracticeAreas() != null ? lawyer.getPracticeAreas().toString().toLowerCase() : "";

                if (!name.contains(searchQuery) && !bar.contains(searchQuery) && !bio.contains(searchQuery) && !tags.contains(searchQuery)) {
                    continue;
                }
            }

            filtered.add(lawyer);
        }

        lawyerAdapter.setLawyers(filtered);
        tvLawyerCount.setText(filtered.size() + " জন আইনজীবী");

        if (filtered.isEmpty()) {
            layoutEmptyLawyers.setVisibility(View.VISIBLE);
            rvLawyers.setVisibility(View.GONE);
        } else {
            layoutEmptyLawyers.setVisibility(View.GONE);
            rvLawyers.setVisibility(View.VISIBLE);
        }
    }

    private void showApplyDialog(LawyerProfile lawyer) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "পরামর্শের জন্য আগে লগইন বা নিবন্ধন করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_apply_consultation, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogLawyerInfo = dialogView.findViewById(R.id.tvDialogLawyerInfo);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText etCaseSummary = dialogView.findViewById(R.id.etCaseSummary);
        EditText etContactPhone = dialogView.findViewById(R.id.etContactPhone);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelApply);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitApply);

        tvDialogLawyerInfo.setText("আইনজীবী: " + lawyer.getName() + " (" + lawyer.getBarAssociation() + ")");

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
        spinnerCategory.setAdapter(catAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String category = (String) spinnerCategory.getSelectedItem();
            String summary = etCaseSummary.getText().toString().trim();
            String phone = etContactPhone.getText().toString().trim();

            if (TextUtils.isEmpty(summary)) {
                etCaseSummary.setError("আপনার সমস্যার সারসংক্ষেপ লিখুন");
                return;
            }

            submitConsultationRequest(lawyer, category, summary, phone, dialog);
        });

        dialog.show();
    }

    private void submitConsultationRequest(LawyerProfile lawyer, String category, String summary, String phone, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String requestId = mFirestore.collection("consultation_requests").document().getId();
        String citizenName = user.getDisplayName() != null ? user.getDisplayName() : "নাগরিক";

        ConsultationRequest req = new ConsultationRequest(
                requestId,
                user.getUid(),
                citizenName,
                phone,
                selectedDistrict,
                "",
                lawyer.getUid(),
                lawyer.getName(),
                lawyer.getPhotoUrl(),
                category,
                summary,
                System.currentTimeMillis()
        );

        mFirestore.collection("consultation_requests")
                .document(requestId)
                .set(req.toMap())
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    Toast.makeText(this, "আবেদন সফলভাবে পাঠানো হয়েছে! আইনজীবী গ্রহণ করলে চ্যাট শুরু হবে।", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "আবেদন পাঠাতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lawyersListener != null) {
            lawyersListener.remove();
        }
    }
}
