package com.example.ukilbhai.rag;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ukilbhai.ChatActivity;
import com.example.ukilbhai.R;
import com.example.ukilbhai.lawyer.LawyerDirectoryActivity;
import com.example.ukilbhai.util.LanguageHelper;
import com.example.ukilbhai.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;

public class LegalSectionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LEGAL_SECTION = "extra_legal_section";

    private LegalSection section;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LanguageHelper.applyLanguageToApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_section_detail);

        section = (LegalSection) getIntent().getSerializableExtra(EXTRA_LEGAL_SECTION);
        if (section == null) {
            Toast.makeText(this, "আইনি তথ্য লোড করা যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnBookmark = findViewById(R.id.btnBookmarkSection);
        ImageButton btnCopy = findViewById(R.id.btnCopySection);
        ImageButton btnShare = findViewById(R.id.btnShareSection);

        TextView tvDetailActBadge = findViewById(R.id.tvDetailActBadge);
        TextView tvDetailSectionTitle = findViewById(R.id.tvDetailSectionTitle);
        TextView tvDetailYear = findViewById(R.id.tvDetailYear);
        TextView tvDetailContent = findViewById(R.id.tvDetailContent);

        MaterialButton btnAskAiExplanation = findViewById(R.id.btnAskAiExplanation);
        MaterialButton btnConsultLawyer = findViewById(R.id.btnConsultLawyer);

        tvDetailActBadge.setText(section.getCitationHeader());
        tvDetailSectionTitle.setText(section.getDisplayTitle());

        String year = section.getActYear();
        if (year != null && !year.trim().isEmpty()) {
            tvDetailYear.setText("সন: " + year.trim());
        } else {
            tvDetailYear.setVisibility(View.GONE);
        }

        tvDetailContent.setText(section.getSectionContent());

        if (btnBookmark != null) {
            updateBookmarkIcon(btnBookmark, BookmarkManager.isBookmarked(this, section));
            btnBookmark.setOnClickListener(v -> {
                boolean nowBookmarked = BookmarkManager.toggleBookmark(this, section);
                updateBookmarkIcon(btnBookmark, nowBookmarked);
                Toast.makeText(this,
                        nowBookmarked ? "ধারাটি সংরক্ষিত তালিকায় যুক্ত হয়েছে 🔖" : "সংরক্ষিত তালিকা থেকে সরানো হয়েছে",
                        Toast.LENGTH_SHORT).show();
            });
        }

        btnBack.setOnClickListener(v -> finish());

        btnCopy.setOnClickListener(v -> copyToClipboard());

        btnShare.setOnClickListener(v -> shareSectionText());

        btnAskAiExplanation.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            String prompt = "উক্ত আইনি ধারাটি সাধারণ মানুষের বোধগম্য সহজ বাংলায় ব্যাখ্যা করুন এবং এর মূল তাৎপর্য বলুন:\n\n"
                    + "আইন: " + section.getCleanActTitle() + "\n"
                    + "ধারা: " + section.getDisplayTitle() + "\n"
                    + "মূল পাঠ:\n" + section.getSectionContent();
            chatIntent.putExtra(ChatActivity.EXTRA_INITIAL_PROMPT, prompt);
            startActivity(chatIntent);
        });

        btnConsultLawyer.setOnClickListener(v -> {
            Intent lawyerIntent = new Intent(this, LawyerDirectoryActivity.class);
            String handoffBrief = "আইনি ধারা সংক্রান্ত পরামর্শ: " + section.getCleanActTitle() + " (" + section.getDisplayTitle() + ")";
            lawyerIntent.putExtra(LawyerDirectoryActivity.EXTRA_CASE_SUMMARY, handoffBrief);
            startActivity(lawyerIntent);
        });
    }

    private void updateBookmarkIcon(ImageButton btnBookmark, boolean isBookmarked) {
        btnBookmark.setImageResource(isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
    }

    private void copyToClipboard() {
        String fullText = section.getCitationHeader() + "\n"
                + section.getDisplayTitle() + "\n\n"
                + section.getSectionContent();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Legal Section", fullText);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "আইনি ধারাটি ক্লিপবোর্ডে কপি করা হয়েছে 📋", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSectionText() {
        String fullText = "【 " + section.getCitationHeader() + " 】\n"
                + section.getDisplayTitle() + "\n\n"
                + section.getSectionContent() + "\n\n"
                + "— সংগৃহীত: উকিল ভাই (UkilBhai Legal AI Platform)";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, section.getDisplayTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, fullText);
        startActivity(Intent.createChooser(shareIntent, "আইনি ধারা শেয়ার করুন"));
    }
}
