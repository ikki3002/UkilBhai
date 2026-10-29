package com.example.ukilbhai.rag;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-Device Legal RAG Database Helper.
 * Pre-indexes core Bangladesh statutory laws (Penal Code, Family, Road Transport, Rent, Labour, etc.)
 * in a local SQLite FTS5 database for instant, offline legal section retrieval.
 */
public class LegalDatabaseHelper {

    private static final String TAG = "LegalDatabaseHelper";
    private static final String DB_NAME = "legal_acts.db";
    private static LegalDatabaseHelper instance;

    private final Context context;
    private SQLiteDatabase database;

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

    private void ensureDatabaseCopied() {
        File dbFile = context.getDatabasePath(DB_NAME);
        if (dbFile.exists() && dbFile.length() > 100000) {
            // Already copied
            return;
        }

        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
        }

        Log.i(TAG, "Copying legal_acts.db from assets to app storage...");
        try (InputStream is = context.getAssets().open(DB_NAME);
             OutputStream os = new FileOutputStream(dbFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            Log.i(TAG, "legal_acts.db copied successfully. Size: " + dbFile.length() + " bytes");
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy legal_acts.db: " + e.getMessage(), e);
        }
    }

    private void openDatabase() {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (dbFile.exists()) {
                database = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                Log.i(TAG, "Legal database opened in READONLY mode.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening legal database: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes Bengali vowels and decomposed characters for search consistency.
     */
    private String normalizeBengali(String text) {
        if (text == null) return "";
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase();
        return nfc.replace('ী', 'ি').replace('ূ', 'ু');
    }

    /**
     * Search relevant legal sections for a citizen query using SQLite FTS5.
     */
    public List<LegalSection> searchStatutes(String query, int maxResults) {
        List<LegalSection> results = new ArrayList<>();
        if (database == null || !database.isOpen() || query == null || query.trim().isEmpty()) {
            return results;
        }

        String normalized = normalizeBengali(query.trim());

        // Extract meaningful tokens (alphanumeric and Bengali characters)
        Pattern pattern = Pattern.compile("[\\u0980-\\u09ffa-zA-Z0-9]+");
        Matcher matcher = pattern.matcher(normalized);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group();
            // Filter out trivial 1-letter words
            if (token.length() > 1) {
                tokens.add(token);
            }
        }

        if (tokens.isEmpty()) {
            return results;
        }

        // 1. Try multi-word AND match first
        StringBuilder matchBuilder = new StringBuilder();
        int count = 0;
        for (String t : tokens) {
            if (count > 0) matchBuilder.append(" ");
            matchBuilder.append(t);
            count++;
            if (count >= 5) break; // Limit search clause to top 5 tokens
        }

        String matchQuery = matchBuilder.toString();
        results = executeFtsSearch(matchQuery, maxResults);

        // 2. If no results, fallback to individual token matching (OR)
        if (results.isEmpty() && tokens.size() > 1) {
            StringBuilder orBuilder = new StringBuilder();
            for (int i = 0; i < Math.min(tokens.size(), 3); i++) {
                if (i > 0) orBuilder.append(" OR ");
                orBuilder.append(tokens.get(i));
            }
            results = executeFtsSearch(orBuilder.toString(), maxResults);
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

    public synchronized void close() {
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }
}
