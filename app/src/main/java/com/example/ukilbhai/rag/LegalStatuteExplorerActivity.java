package com.example.ukilbhai.rag;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ukilbhai.R;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LegalStatuteExplorerActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_CATEGORY = "extra_initial_category";

    private String currentCategory = "all";
    private String currentQuery = "";

    private EditText etSearchStatutes;
    private ImageButton btnClearSearch;
    private TextView tvTotalCountBadge;
    private TextView tvResultCount;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private MaterialButton btnResetSearch;
    private RecyclerView rvStatutes;

    private LegalSectionAdapter adapter;
    private LegalDatabaseHelper dbHelper;

    private final Map<String, MaterialButton> categoryButtons = new HashMap<>();

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statute_explorer);

        dbHelper = LegalDatabaseHelper.getInstance(this);

        String initialCat = getIntent().getStringExtra(EXTRA_INITIAL_CATEGORY);
        if (initialCat != null && !initialCat.trim().isEmpty()) {
            currentCategory = initialCat.trim().toLowerCase();
        }

        initViews();
        setupCategoryChips();
        setupSearchInput();
        setupRecyclerView();

        loadStatutes();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etSearchStatutes = findViewById(R.id.etSearchStatutes);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvTotalCountBadge = findViewById(R.id.tvTotalCountBadge);
        tvResultCount = findViewById(R.id.tvResultCount);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnResetSearch = findViewById(R.id.btnResetSearch);
        rvStatutes = findViewById(R.id.rvStatutes);

        // Update total database sections badge
        int totalSections = dbHelper.getSectionCount();
        if (totalSections > 0) {
            tvTotalCountBadge.setText(String.format(Locale.getDefault(), "%,dটি ধারা", totalSections));
        }

        btnResetSearch.setOnClickListener(v -> {
            etSearchStatutes.setText("");
            currentCategory = "all";
            highlightSelectedCategory();
            loadStatutes();
        });

        btnClearSearch.setOnClickListener(v -> etSearchStatutes.setText(""));
    }

    private void setupCategoryChips() {
        categoryButtons.put("all", findViewById(R.id.chipAll));
        categoryButtons.put("traffic", findViewById(R.id.chipTraffic));
        categoryButtons.put("family", findViewById(R.id.chipFamily));
        categoryButtons.put("tenant", findViewById(R.id.chipTenant));
        categoryButtons.put("criminal", findViewById(R.id.chipCriminal));
        categoryButtons.put("labour", findViewById(R.id.chipLabour));

        for (Map.Entry<String, MaterialButton> entry : categoryButtons.entrySet()) {
            String key = entry.getKey();
            MaterialButton btn = entry.getValue();
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    if (!currentCategory.equalsIgnoreCase(key)) {
                        currentCategory = key;
                        highlightSelectedCategory();
                        loadStatutes();
                    }
                });
            }
        }

        highlightSelectedCategory();
    }

    private void highlightSelectedCategory() {
        int colorEmerald = ContextCompat.getColor(this, R.color.primary_emerald);
        int colorWhite = ContextCompat.getColor(this, R.color.white);
        int colorSecondary = ContextCompat.getColor(this, R.color.text_secondary);
        int colorCardStroke = ContextCompat.getColor(this, R.color.card_stroke);
        int colorSurfaceCard = ContextCompat.getColor(this, R.color.surface_card);

        for (Map.Entry<String, MaterialButton> entry : categoryButtons.entrySet()) {
            String key = entry.getKey();
            MaterialButton btn = entry.getValue();
            if (btn == null) continue;

            if (key.equalsIgnoreCase(currentCategory)) {
                // Active style
                btn.setBackgroundColor(colorEmerald);
                btn.setTextColor(colorWhite);
                btn.setStrokeWidth(0);
            } else {
                // Inactive outline style
                btn.setBackgroundColor(colorSurfaceCard);
                btn.setTextColor(colorSecondary);
                btn.setStrokeColor(ColorStateList.valueOf(colorCardStroke));
                btn.setStrokeWidth(2);
            }
        }
    }

    private void setupSearchInput() {
        etSearchStatutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchDebounceRunnable != null) {
                    mainHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> loadStatutes();
                mainHandler.postDelayed(searchDebounceRunnable, 180);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        rvStatutes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LegalSectionAdapter(new ArrayList<>(), section -> {
            Intent intent = new Intent(LegalStatuteExplorerActivity.this, LegalSectionDetailActivity.class);
            intent.putExtra(LegalSectionDetailActivity.EXTRA_LEGAL_SECTION, section);
            startActivity(intent);
        });
        rvStatutes.setAdapter(adapter);
    }

    private void loadStatutes() {
        progressBar.setVisibility(View.VISIBLE);

        final String query = currentQuery;
        final String category = currentCategory;

        backgroundExecutor.execute(() -> {
            List<LegalSection> results = dbHelper.searchStatuteExplorer(query, category, 250);

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);

                if (results.isEmpty()) {
                    rvStatutes.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    tvResultCount.setText("০টি ধারা পাওয়া গেছে");
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvStatutes.setVisibility(View.VISIBLE);
                    adapter.updateData(results);
                    rvStatutes.scrollToPosition(0);

                    tvResultCount.setText(String.format(Locale.getDefault(), "%dটি ধারা প্রদর্শিত হচ্ছে", results.size()));
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchDebounceRunnable != null) {
            mainHandler.removeCallbacks(searchDebounceRunnable);
        }
        backgroundExecutor.shutdown();
    }
}
