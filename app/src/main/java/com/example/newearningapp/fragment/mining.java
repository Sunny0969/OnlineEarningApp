package com.example.newearningapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.newearningapp.R;
import com.example.newearningapp.BackgroundMiningService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class mining extends Fragment {

    private static final String TAG = "MiningFragment";
    private static final String PREF_NAME = "MiningPrefs";
    private static final String KEY_MINING_START_TIME = "mining_start_time";
    private static final String KEY_IS_MINING = "is_mining";
    private static final long MINING_DURATION = 86400000; // 24 hours in milliseconds

    private ProgressBar miningProgress;
    private TextView coinsMinedText, timeRemainingText;
    private Button startMiningBtn;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private SharedPreferences prefs;
    private Handler handler;
    private Runnable updateRunnable;

    public mining() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mining, container, false);

        // Initialize views
        miningProgress = view.findViewById(R.id.mining_progress);
        coinsMinedText = view.findViewById(R.id.coins_mined_text);
        timeRemainingText = view.findViewById(R.id.time_remaining_text);
        startMiningBtn = view.findViewById(R.id.start_mining_btn);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        }

        // Initialize SharedPreferences
        prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Initialize Handler for UI updates
        handler = new Handler(Looper.getMainLooper());

        // Set up mining button
        startMiningBtn.setOnClickListener(v -> {
            if (!prefs.getBoolean(KEY_IS_MINING, false)) {
                startMining();
            } else {
                Toast.makeText(getContext(), "Mining already in progress", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMiningStatus();

        // Start periodic UI updates if mining is in progress
        if (prefs.getBoolean(KEY_IS_MINING, false)) {
            startUIUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopUIUpdates();
    }

    private void startMining() {
        try {
            // Manually update preferences first (in case service fails to start)
            prefs.edit()
                    .putLong(KEY_MINING_START_TIME, System.currentTimeMillis())
                    .putBoolean(KEY_IS_MINING, true)
                    .apply();

            // Create explicit intent
            Intent serviceIntent = new Intent(requireActivity(), BackgroundMiningService.class);

            // Log attempt to start service
            Log.d(TAG, "Attempting to start BackgroundMiningService");

            // Start service based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(serviceIntent);
                Log.d(TAG, "Called startForegroundService for Android O+");
            } else {
                requireActivity().startService(serviceIntent);
                Log.d(TAG, "Called startService for pre-Android O");
            }

            // Update UI
            updateMiningStatus();
            startUIUpdates();

            Toast.makeText(getContext(), "Mining started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Mining started successfully");
        } catch (Exception e) {
            // Log any exceptions
            Log.e(TAG, "Error starting mining service", e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Reset mining state on failure
            prefs.edit().putBoolean(KEY_IS_MINING, false).apply();
        }
    }

    private void updateMiningStatus() {
        boolean isMining = prefs.getBoolean(KEY_IS_MINING, false);
        long startTime = prefs.getLong(KEY_MINING_START_TIME, 0);

        if (isMining && startTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;

            if (elapsedTime >= MINING_DURATION) {
                // Mining should be complete, but service hasn't updated yet
                miningProgress.setProgress(100);
                startMiningBtn.setEnabled(false);
                startMiningBtn.setText("Mining Complete");
                coinsMinedText.setText("Coins Mined: 1.000");
                timeRemainingText.setText("Time Remaining: 00:00:00");
            } else {
                // Mining is in progress
                updateMiningUI(elapsedTime);
                startMiningBtn.setEnabled(false);
                startMiningBtn.setText("Mining in Progress");
            }
        } else {
            // Not mining
            resetMiningUI();
        }
    }

    private void updateMiningUI(long elapsedTime) {
        // Calculate remaining time
        long remainingTime = Math.max(0, MINING_DURATION - elapsedTime);

        // Calculate progress
        int progress = (int) (elapsedTime * 100 / MINING_DURATION);
        miningProgress.setProgress(progress);

        // Calculate mined coins (0 to 1.0)
        double minedCoins = Math.min(1.0, (double) elapsedTime / MINING_DURATION);

        // Update UI with formatted values
        NumberFormat formatter = new DecimalFormat("#0.000");
        coinsMinedText.setText("Coins Mined: " + formatter.format(minedCoins));

        // Format time remaining (HH:MM:SS)
        int hours = (int) (remainingTime / 3600000);
        int minutes = (int) (remainingTime % 3600000) / 60000;
        int seconds = (int) (remainingTime % 60000) / 1000;

        String timeLeft = String.format(Locale.getDefault(),
                "Time Remaining: %02d:%02d:%02d", hours, minutes, seconds);
        timeRemainingText.setText(timeLeft);
    }

    private void resetMiningUI() {
        miningProgress.setProgress(0);
        coinsMinedText.setText("Coins Mined: 0.000");
        timeRemainingText.setText("Time Remaining: 24:00:00");
        startMiningBtn.setEnabled(true);
        startMiningBtn.setText("Start Mining");
    }

    private void startUIUpdates() {
        stopUIUpdates(); // Stop any existing updates

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateMiningStatus();
                handler.postDelayed(this, 1000); // Update every second
            }
        };

        handler.post(updateRunnable);
    }

    private void stopUIUpdates() {
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUIUpdates();
    }
}