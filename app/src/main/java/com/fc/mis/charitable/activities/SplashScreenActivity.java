package com.fc.mis.charitable.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.fc.mis.charitable.R;
import com.google.firebase.FirebaseApp;

public class SplashScreenActivity extends AppCompatActivity {
    // duration of watit
    private final int SPLASH_DISPLAY_LENGTH = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        FirebaseApp.initializeApp(this);

        /* new Handler to start the Menu-Activity
           and close this splash-screen after some seconds */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
