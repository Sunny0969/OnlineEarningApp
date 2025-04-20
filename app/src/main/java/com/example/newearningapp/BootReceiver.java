package com.example.newearningapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.newearningapp.BackgroundMiningService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREF_NAME = "MiningPrefs";
    private static final String KEY_IS_MINING = "is_mining";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, checking mining status");

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean isMining = prefs.getBoolean(KEY_IS_MINING, false);

            if (isMining) {
                Log.d(TAG, "Mining was in progress, restarting service");
                Intent serviceIntent = new Intent(context, BackgroundMiningService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}