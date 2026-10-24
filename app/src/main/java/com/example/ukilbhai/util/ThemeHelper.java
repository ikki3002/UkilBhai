package com.example.ukilbhai.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    public static final String PREFS_NAME = "ukilbhai_theme_preferences";
    public static final String KEY_THEME_MODE = "key_theme_mode";

    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_SYSTEM = 3;

    public static void applyTheme(Context context) {
        int mode = getSavedTheme(context);
        applyThemeMode(mode);
    }

    public static void applyThemeMode(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void saveTheme(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyThemeMode(mode);
    }

    public static int getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default to Dark Mode as per Ukil Bhai Linear-Dark baseline
        return prefs.getInt(KEY_THEME_MODE, THEME_DARK);
    }

    public static String getThemeDisplayName(Context context, int mode) {
        boolean isBn = LanguageHelper.isBengali(context);
        switch (mode) {
            case THEME_LIGHT:
                return isBn ? "লাইট" : "Light";
            case THEME_DARK:
                return isBn ? "ডার্ক" : "Dark";
            case THEME_SYSTEM:
            default:
                return isBn ? "সিস্টেম ডিফল্ট" : "System Default";
        }
    }
}
