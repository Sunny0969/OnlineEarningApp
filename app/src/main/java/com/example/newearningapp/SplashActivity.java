package com.example.newearningapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    // UI elements
    private ImageView logoImageView;
    private TextView appNameTextView;

    // Firebase
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the splash screen full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        // Initialize UI elements
        logoImageView = findViewById(R.id.splash_logo);
        appNameTextView = findViewById(R.id.splash_app_name);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        logoImageView.startAnimation(fadeIn);
        appNameTextView.startAnimation(fadeIn);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Delay for splash screen then navigate based on auth state
        new Handler().postDelayed(this::checkUserAndNavigate, SPLASH_DURATION);
    }

    /**
     * Check user authentication status and navigate to appropriate screen
     */
    private void checkUserAndNavigate() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        Intent intent;

        if (currentUser != null) {
            // User is logged in
            if (currentUser.isEmailVerified()) {
                // User email is verified, go to main activity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // User email is not verified, go to verification prompt
                intent = new Intent(SplashActivity.this, LoginActivity.class);
                intent.putExtra("user_email", currentUser.getEmail());
            }
        } else {
            // No user logged in, go to login activity
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Close the splash activity
    }
}