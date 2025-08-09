package com.example.optimaai;

import android.app.Application;
import android.util.Log; // <-- Pastikan import ini ada

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class MyApplication extends Application {

    // Kita buat TAG khusus agar mudah dicari di Logcat
    private static final String TAG = "APP_CHECK_DEBUG";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

        // Kode ini secara manual meminta token debug dan mencetaknya ke Logcat
        firebaseAppCheck.getAppCheckToken(true)
                .addOnSuccessListener(appCheckToken -> {
                    if (appCheckToken != null && appCheckToken.getToken() != null) {
                        Log.d(TAG, "Enter this debug token in the Firebase console:\n" + appCheckToken.getToken());
                    }
                });

        // Kita tetap menginstal provider debug untuk memastikan semuanya berjalan
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
    }
}