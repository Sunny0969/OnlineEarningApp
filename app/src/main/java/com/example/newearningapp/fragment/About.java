package com.example.newearningapp.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.newearningapp.R;
import com.karumi.dexter.BuildConfig;

public class About extends Fragment {

    private TextView nameTV, versionTV, notesTV;

    public About() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameTV = view.findViewById(R.id.nameTV);
        versionTV = view.findViewById(R.id.version);
        notesTV = view.findViewById(R.id.notesTV);

        String versionName = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
        String version = "Version " + versionName + "." + versionCode;
        versionTV.setText(version);
        nameTV.setText("CoinFlow");

        String notes = "Important Notes:\n\n" +
                "• Withdrawals will be processed within 24-48 hours\n" +
                "• Keep your email and password secure\n" +
                "• Use one account per device only\n" +
                "• Complete tasks carefully to earn more\n" +
                "• Invite friends to earn referral bonuses\n" +
                "• Check daily for new earning opportunities\n" +
                "• Contact support for any account issues\n\n" +
                "Thank you for using CoinFlow!";

        notesTV.setText(notes);
    }
}