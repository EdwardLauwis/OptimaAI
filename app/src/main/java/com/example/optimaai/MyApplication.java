package com.example.optimaai;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class MyApplication extends Application {
    private static final String TAG = "APP_CHECK_DEBUG";

    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(() -> {
            FirebaseApp.initializeApp(this);
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

            firebaseAppCheck.getAppCheckToken(true)
                    .addOnSuccessListener(appCheckToken -> {
                        if (appCheckToken != null) {
                            appCheckToken.getToken();
                            Log.d(TAG, "Enter this debug token in the Firebase console:\n" + appCheckToken.getToken());
                        }
                    });

            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
        }).start();
    }
}