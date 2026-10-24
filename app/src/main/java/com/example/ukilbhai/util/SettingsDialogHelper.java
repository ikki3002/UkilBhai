package com.example.ukilbhai.util;

import android.app.Activity;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsDialogHelper {

    public interface OnSettingsChangeListener {
        void onSettingsChanged();
    }

    public static void showSettingsDialog(Activity activity, OnSettingsChangeListener listener) {
        boolean isBn = LanguageHelper.isBengali(activity);

        String title = isBn ? "সেটিংস" : "Settings";
        String themeLabel = isBn ? "থিম: " : "Theme: ";
        String langLabel = isBn ? "ভাষা: " : "Language: ";
        String currentThemeName = ThemeHelper.getThemeDisplayName(activity, ThemeHelper.getSavedTheme(activity));
        String currentLangName = isBn ? "বাংলা" : "English";
        String closeBtn = isBn ? "বন্ধ করুন" : "Close";

        String[] options = new String[]{
                themeLabel + currentThemeName,
                langLabel + currentLangName
        };

        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showThemeDialog(activity, listener);
                    } else if (which == 1) {
                        showLanguageDialog(activity, listener);
                    }
                })
                .setNegativeButton(closeBtn, null)
                .show();
    }

    public static void showThemeDialog(Activity activity, OnSettingsChangeListener listener) {
        boolean isBn = LanguageHelper.isBengali(activity);

        String title = isBn ? "থিম নির্বাচন করুন" : "Choose Theme";
        String cancelBtn = isBn ? "বাতিল" : "Cancel";

        String[] themeOptions = isBn ?
                new String[]{"লাইট", "ডার্ক", "সিস্টেম ডিফল্ট"} :
                new String[]{"Light", "Dark", "System Default"};

        int currentTheme = ThemeHelper.getSavedTheme(activity);
        int checkedIndex = 1; // Default dark
        if (currentTheme == ThemeHelper.THEME_LIGHT) {
            checkedIndex = 0;
        } else if (currentTheme == ThemeHelper.THEME_DARK) {
            checkedIndex = 1;
        } else if (currentTheme == ThemeHelper.THEME_SYSTEM) {
            checkedIndex = 2;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setSingleChoiceItems(themeOptions, checkedIndex, (dialog, which) -> {
                    int newMode = ThemeHelper.THEME_SYSTEM;
                    if (which == 0) {
                        newMode = ThemeHelper.THEME_LIGHT;
                    } else if (which == 1) {
                        newMode = ThemeHelper.THEME_DARK;
                    } else if (which == 2) {
                        newMode = ThemeHelper.THEME_SYSTEM;
                    }

                    ThemeHelper.saveTheme(activity, newMode);
                    dialog.dismiss();
                    Toast.makeText(activity, isBn ? "থিম পরিবর্তিত হয়েছে" : "Theme updated", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onSettingsChanged();
                    }
                })
                .setNegativeButton(cancelBtn, null)
                .show();
    }

    public static void showLanguageDialog(Activity activity, OnSettingsChangeListener listener) {
        boolean isBn = LanguageHelper.isBengali(activity);

        String title = isBn ? "ভাষা নির্বাচন করুন" : "Choose Language";
        String cancelBtn = isBn ? "বাতিল" : "Cancel";

        String[] langOptions = new String[]{"বাংলা", "English"};
        int checkedIndex = isBn ? 0 : 1;

        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setSingleChoiceItems(langOptions, checkedIndex, (dialog, which) -> {
                    String selectedLang = (which == 0) ? LanguageHelper.LANG_BN : LanguageHelper.LANG_EN;
                    LanguageHelper.saveLanguage(activity, selectedLang);
                    dialog.dismiss();
                    boolean newIsBn = LanguageHelper.isBengali(activity);
                    Toast.makeText(activity, newIsBn ? "ভাষা পরিবর্তিত হয়েছে" : "Language updated", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onSettingsChanged();
                    }
                })
                .setNegativeButton(cancelBtn, null)
                .show();
    }
}
