package com.example.ukilbhai.rag;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-Device Legal RAG Database Helper.
 * Pre-indexes core Bangladesh statutory laws (Penal Code, CrPC, Family, Rent, Labour, etc.)
 * in a local SQLite database for instant, offline legal section retrieval.
 */
public class LegalDatabaseHelper {

    private static final String TAG = "LegalDatabaseHelper";
    private static final String DB_NAME = "legal_acts.db";
    private static final int CURRENT_DB_VERSION = 3; // Incremented for Legal Aid Handbook integration
    private static final String PREF_NAME = "legal_rag_prefs";
    private static final String KEY_DB_VERSION = "copied_db_version";

    private static LegalDatabaseHelper instance;
    private final Context context;
    private SQLiteDatabase database;

    // Common Bengali & English conversational stopwords to ignore during statute matching
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "আমার", "আমাদের", "তোমার", "আপনার", "কি", "কী", "করতে", "পারি", "পারব",
            "হলে", "যদি", "কোন", "কোনো", "নিয়ে", "দিলে", "হবে", "আছে", "না", "করে",
            "করা", "জন্য", "একটি", "এবং", "বা", "থেকে", "কে", "তে", "এ", "র", "কিভাবে",
            "উচিত", "চাই", "চাচ্ছি", "বলুন", "hello", "hi", "how", "what", "can", "i",
            "do", "the", "a", "an", "is", "in", "to", "for", "of", "with", "my"
    ));

    private LegalDatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
        ensureDatabaseCopied();
        openDatabase();
    }

    public static synchronized LegalDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LegalDatabaseHelper(context);
        }
        return instance;
    }

    private synchronized void ensureDatabaseCopied() {
        File dbFile = context.getDatabasePath(DB_NAME);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int savedVersion = prefs.getInt(KEY_DB_VERSION, 0);

        // Check if database needs copy or upgrade
        boolean needsCopy = !dbFile.exists() || savedVersion < CURRENT_DB_VERSION || dbFile.length() < 1000000;
        if (!needsCopy) {
            return;
        }

        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }

        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
        }

        Log.i(TAG, "Copying legal_acts.db (v" + CURRENT_DB_VERSION + ") from assets to app storage...");
        try (InputStream is = context.getAssets().open(DB_NAME);
             OutputStream os = new FileOutputStream(dbFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            prefs.edit().putInt(KEY_DB_VERSION, CURRENT_DB_VERSION).apply();
            Log.i(TAG, "legal_acts.db copied successfully. Size: " + dbFile.length() + " bytes");
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy legal_acts.db: " + e.getMessage(), e);
        }
    }

    private synchronized void openDatabase() {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (dbFile.exists()) {
                database = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                Log.i(TAG, "Legal database opened in READONLY mode. Ready: " + isDatabaseReady());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening legal database: " + e.getMessage(), e);
        }
    }

    public boolean isDatabaseReady() {
        return database != null && database.isOpen();
    }

    public int getSectionCount() {
        if (!isDatabaseReady()) return 0;
        try (Cursor c = database.rawQuery("SELECT count(*) FROM sections", null)) {
            if (c.moveToFirst()) return c.getInt(0);
        } catch (Exception ignored) {}
        return 0;
    }

    public int getActCount() {
        if (!isDatabaseReady()) return 0;
        try (Cursor c = database.rawQuery("SELECT count(DISTINCT act_title) FROM sections", null)) {
            if (c.moveToFirst()) return c.getInt(0);
        } catch (Exception ignored) {}
        return 0;
    }

    private String normalizeBengali(String text) {
        if (text == null) return "";
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase();
        return nfc.replace('ী', 'ি').replace('ূ', 'ু');
    }

    /**
     * Extracts meaningful legal search terms and performs legal concept expansion.
     */
    private List<String> extractSearchTerms(String rawQuery) {
        List<String> terms = new ArrayList<>();
        if (rawQuery == null) return terms;

        String normalized = normalizeBengali(rawQuery.trim());

        // Extract word tokens
        Pattern pattern = Pattern.compile("[\\u0980-\\u09ffa-zA-Z0-9]+");
        Matcher matcher = pattern.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > 1 && !STOPWORDS.contains(token)) {
                terms.add(token);
            }
        }

        // Conversational legal concept expansion for common citizen queries
        String lower = rawQuery.toLowerCase();
        if (lower.contains("ময়লা") || lower.contains("আবর্জনা") || lower.contains("পাবলিক ন্যুসেন্স")
                || lower.contains("উপদ্রব") || lower.contains("দুর্গন্ধ") || lower.contains("বর্জ্য")) {
            terms.add("পাবলিক");
            terms.add("উপদ্রব");
            terms.add("নু্যসেন্স");
            terms.add("nuisance");
        }
        if (lower.contains("বাড়ি") || lower.contains("ভাড়া") || lower.contains("উচ্ছেদ") || lower.contains("জামানত")) {
            terms.add("ভাড়া");
            terms.add("উচ্ছেদ");
            terms.add("বাড়িভাড়া");
        }
        if (lower.contains("চেক") || lower.contains("বাউন্স") || lower.contains("ডিজঅনার")) {
            terms.add("চেক");
            terms.add("১৩৮");
            terms.add("dishonour");
        }
        if (lower.contains("যৌতুক")) {
            terms.add("যৌতুক");
            terms.add("যৌতুক নিরোধ");
        }
        if (lower.contains("তালাক") || lower.contains("দেনমোহর") || lower.contains("খোরপোশ") || lower.contains("বিয়ে")) {
            terms.add("তালাক");
            terms.add("দেনমোহর");
            terms.add("পারিবারিক");
        }
        if (lower.contains("খুন") || lower.contains("মার্ডার") || lower.contains("হত্যা")) {
            terms.add("খুন");
            terms.add("হত্যা");
            terms.add("৩০২");
        }
        if (lower.contains("মারধর") || lower.contains("মারামারি") || lower.contains("আঘাত") || lower.contains("জখম")) {
            terms.add("মারধর");
            terms.add("আঘাত");
            terms.add("hurt");
        }
        if (lower.contains("চুরি") || lower.contains("ছিনতাই") || lower.contains("ডাকাতি")) {
            terms.add("চুরি");
            terms.add("ছিনতাই");
            terms.add("theft");
        }
        if (lower.contains("প্রতারণা") || lower.contains("ঠকানো") || lower.contains("জালিয়াতি") || lower.contains("চিটিং")) {
            terms.add("প্রতারণা");
            terms.add("জালিয়াতি");
            terms.add("cheating");
        }
        if (lower.contains("লিগ্যাল") || lower.contains("সহায়তা") || lower.contains("১৬৬৯৯")
                || lower.contains("সরকারি উকিল") || lower.contains("বিনা খরচে") || lower.contains("বিনা মূল্যে")
                || lower.contains("উকিল খরচ") || lower.contains("গরিব") || lower.contains("অসচ্ছল")
                || lower.contains("মধ্যস্থতা") || lower.contains("মেডিয়েশন") || lower.contains("সালিশ")) {
            terms.add("লিগ্যাল");
            terms.add("সহায়তা");
            terms.add("১৬৬৯৯");
            terms.add("যোগ্যতা");
            terms.add("মধ্যস্থতা");
        }

        return terms;
    }

    /**
     * Search relevant legal sections for a citizen query.
     * Uses FTS5 primary search with automatic LIKE fallback for 100% reliability across all devices.
     */
    public List<LegalSection> searchStatutes(String query, int maxResults) {
        List<LegalSection> results = new ArrayList<>();
        if (!isDatabaseReady() || query == null || query.trim().isEmpty()) {
            return results;
        }

        List<String> terms = extractSearchTerms(query);
        if (terms.isEmpty()) {
            return results;
        }

        // Build FTS query with OR
        StringBuilder ftsBuilder = new StringBuilder();
        int added = 0;
        for (String term : terms) {
            if (added > 0) ftsBuilder.append(" OR ");
            ftsBuilder.append("\"").append(term).append("\"");
            added++;
            if (added >= 5) break; // Limit search clause to top 5 distinctive terms
        }

        String matchQuery = ftsBuilder.toString();
        results = executeFtsSearch(matchQuery, maxResults);

        // Fallback to SQL LIKE query if FTS returns 0 or fails
        if (results.isEmpty() && !terms.isEmpty()) {
            results = executeLikeFallback(terms, maxResults);
        }

        return results;
    }

    private List<LegalSection> executeFtsSearch(String matchClause, int maxResults) {
        List<LegalSection> list = new ArrayList<>();
        String sql = "SELECT s.act_title, s.act_year, s.section_title, s.section_content " +
                "FROM sections_fts f " +
                "JOIN sections s ON f.rowid = s.id " +
                "WHERE sections_fts MATCH ? " +
                "ORDER BY rank " +
                "LIMIT ?";

        try (Cursor cursor = database.rawQuery(sql, new String[]{matchClause, String.valueOf(maxResults)})) {
            while (cursor.moveToNext()) {
                String actTitle = cursor.getString(0);
                String actYear = cursor.getString(1);
                String sectionTitle = cursor.getString(2);
                String sectionContent = cursor.getString(3);

                list.add(new LegalSection(actTitle, actYear, sectionTitle, sectionContent));
            }
        } catch (Exception e) {
            Log.w(TAG, "FTS query failed for [" + matchClause + "]: " + e.getMessage());
        }
        return list;
    }

    private List<LegalSection> executeLikeFallback(List<String> terms, int maxResults) {
        List<LegalSection> list = new ArrayList<>();
        if (!isDatabaseReady() || terms.isEmpty()) return list;

        String primary = terms.get(0);
        String sql = "SELECT act_title, act_year, section_title, section_content " +
                "FROM sections " +
                "WHERE search_keywords LIKE ? OR section_content LIKE ? OR section_title LIKE ? " +
                "ORDER BY id " +
                "LIMIT ?";

        String pattern = "%" + primary + "%";
        try (Cursor cursor = database.rawQuery(sql, new String[]{pattern, pattern, pattern, String.valueOf(maxResults)})) {
            while (cursor.moveToNext()) {
                String actTitle = cursor.getString(0);
                String actYear = cursor.getString(1);
                String sectionTitle = cursor.getString(2);
                String sectionContent = cursor.getString(3);

                list.add(new LegalSection(actTitle, actYear, sectionTitle, sectionContent));
            }
        } catch (Exception e) {
            Log.w(TAG, "Fallback LIKE query failed: " + e.getMessage());
        }
        return list;
    }

    public synchronized void close() {
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }
}