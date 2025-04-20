package com.example.newearningapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.newearningapp.MainActivity;
import com.example.newearningapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundMiningService extends Service {
    private static final String TAG = "BackgroundMiningService";
    private static final String NOTIFICATION_CHANNEL_ID = "MiningServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long MINING_DURATION = 86400000; // 24 hours in milliseconds
    private static final String PREF_NAME = "MiningPrefs";
    private static final String KEY_MINING_START_TIME = "mining_start_time";
    private static final String KEY_IS_MINING = "is_mining";

    private Timer miningTimer;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BackgroundMiningService onCreate called");

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        }

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BackgroundMiningService onStartCommand called");

        // IMPORTANT: Start as a foreground service immediately
        startForeground(NOTIFICATION_ID, createNotification("Mining in progress", "Earning coins..."));

        // Check if mining is already in progress
        if (!prefs.getBoolean(KEY_IS_MINING, false)) {
            // Start new mining session
            startMining();
        } else {
            // Mining is already in progress, check if it should be completed
            checkMiningProgress();
        }

        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY;
    }

    private void startMining() {
        Log.d(TAG, "Starting mining process");
        long startTime = System.currentTimeMillis();

        // Save mining start time and state
        prefs.edit()
                .putLong(KEY_MINING_START_TIME, startTime)
                .putBoolean(KEY_IS_MINING, true)
                .apply();

        // Schedule periodic checks (every 15 minutes)
        miningTimer = new Timer();
        miningTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkMiningProgress();
            }
        }, 60000, 900000); // First check after 1 minute, then every 15 minutes
    }

    private void checkMiningProgress() {
        long startTime = prefs.getLong(KEY_MINING_START_TIME, 0);
        if (startTime == 0) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        Log.d(TAG, "Mining progress check - elapsed time: " + elapsedTime + "ms");

        if (elapsedTime >= MINING_DURATION) {
            // Mining is complete
            completeMining();
        } else {
            // Update notification with progress
            double progress = (double) elapsedTime / MINING_DURATION * 100;
            updateNotification("Mining: " + String.format("%.1f", progress) + "% complete");
        }
    }

    private void completeMining() {
        Log.d(TAG, "Mining complete, updating user coins");

        // Reset mining state
        prefs.edit()
                .putBoolean(KEY_IS_MINING, false)
                .apply();

        // Add coins to user's account
        if (currentUser != null) {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        double currentCoins = 0;
                        if (snapshot.child("coins").exists()) {
                            Object coinsObj = snapshot.child("coins").getValue();
                            if (coinsObj instanceof Double) {
                                currentCoins = (Double) coinsObj;
                            } else if (coinsObj instanceof Long) {
                                currentCoins = ((Long) coinsObj).doubleValue();
                            }
                        }

                        double updatedCoins = currentCoins + 1.0; // Add 1 coin

                        HashMap<String, Object> updateMap = new HashMap<>();
                        updateMap.put("coins", updatedCoins);

                        userRef.updateChildren(updateMap).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Added 1.0 coin to user account");
                                updateNotification("Mining complete! Added 1.0 coin to your account");
                            } else {
                                Log.e(TAG, "Failed to add coins", task.getException());
                            }

                            // Stop the service after a delay to ensure user sees the completion notification
                            new Handler(getMainLooper()).postDelayed(() -> {
                                stopSelf();
                            }, 10000); // Wait 10 seconds before stopping
                        });
                    } else {
                        stopSelf();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    stopSelf();
                }
            });
        } else {
            stopSelf();
        }
    }

    private Notification createNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void updateNotification(String content) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = createNotification("Mining in progress", content);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Mining Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for the mining service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BackgroundMiningService onDestroy called");
        if (miningTimer != null) {
            miningTimer.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}