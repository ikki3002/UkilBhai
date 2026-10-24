package com.example.ukilbhai.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageHelper {

    public static final String PREFS_NAME = "ukilbhai_language_preferences";
    public static final String KEY_LANGUAGE = "key_app_language";

    public static final String LANG_BN = "bn";
    public static final String LANG_EN = "en";

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_BN);
    }

    public static void saveLanguage(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply();
        applyLanguageToApp(context);
    }

    public static boolean isBengali(Context context) {
        return LANG_BN.equals(getSavedLanguage(context));
    }

    public static Context applyLocale(Context context) {
        String lang = getSavedLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    @SuppressWarnings("deprecation")
    public static void applyLanguageToApp(Context context) {
        String lang = getSavedLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}
