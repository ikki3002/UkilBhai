package com.example.ukilbhai;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class UkilBhaiApp extends Application {

    private static final String TAG = "UkilBhaiApp";
    private static UkilBhaiApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        ensureFirebaseInitialized();
    }

    public static UkilBhaiApp getInstance() {
        return instance;
    }

    public static android.content.Context getAppContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }

    private void ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("1:730595914038:android:7afab680a52a9166770397")
                        .setApiKey("AIzaSyBRscW2eUNKlUbUqlZZyv-0Nr-43ps8Nuc")
                        .setProjectId("ukilbhai-9414f")
                        .setStorageBucket("ukilbhai-9414f.firebasestorage.app")
                        .setGcmSenderId("730595914038")
                        .build();
                FirebaseApp.initializeApp(this, options);
                Log.i(TAG, "Firebase initialized programmatically with FirebaseOptions");
            } else {
                Log.i(TAG, "Firebase initialized automatically via provider");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    }
}
