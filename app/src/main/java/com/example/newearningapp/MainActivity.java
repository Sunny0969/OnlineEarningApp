package com.example.newearningapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.newearningapp.fragment.FragmentReplacerActivity;
import com.example.newearningapp.model.ProfileModel;
import com.facebook.ads.AudienceNetworkAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private CardView luckySpinCard, taskCard, referCard, redeemCard, watchCard, aboutCard, MiningCard;
    private CircleImageView profileImage;
    private TextView nameTv, emailTv, coinsTv;
    private Toolbar toolbar;
    private DatabaseReference reference;
    private FirebaseUser user;
    private Dialog dialog;
    private ImageView dailyIcon;
    Internet internet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestNotificationPermission();

        init();
        internet = new Internet(MainActivity.this);

        AudienceNetworkAds.initialize(this);

        checkInternetConnection();
        setSupportActionBar(toolbar);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference().child("Users");
        checkDailyStatus();

        getDataFromDatabase();
        clickListener();
    }

    private void clickListener() {
        profileImage.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        if (referCard != null) {
            referCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, inviteActivity.class));
                }
            });
        }

        dailyIcon.setOnClickListener(v -> dailyCheck());

        if (redeemCard != null) {
            redeemCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, RedeemActivity.class));
                }
            });
        }

        if (luckySpinCard != null) {
            luckySpinCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, FragmentReplacerActivity.class);
                    intent.putExtra("position", 2);
                    startActivity(intent);
                }
            });
        }

        if (aboutCard != null) {
            aboutCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, FragmentReplacerActivity.class);
                    intent.putExtra("position", 3);
                    startActivity(intent);
                }
            });
        }

        if (MiningCard != null) {
            MiningCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, FragmentReplacerActivity.class);
                    intent.putExtra("position", 4);
                    startActivity(intent);
                }
            });
        }

        if (taskCard != null) {
            taskCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add your task activity intent here
                    Toast.makeText(MainActivity.this, "Task feature coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (watchCard != null) {
            watchCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add your watch activity intent here
                    Toast.makeText(MainActivity.this, "Watch feature coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void init() {
        MiningCard = findViewById(R.id.MiningCard);
        luckySpinCard = findViewById(R.id.luckySpinCard);
        taskCard = findViewById(R.id.taskCard);
        referCard = findViewById(R.id.referCard);
        redeemCard = findViewById(R.id.redeemCard);
        watchCard = findViewById(R.id.watchCard);
        aboutCard = findViewById(R.id.aboutCard);

        profileImage = findViewById(R.id.profileImage);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        coinsTv = findViewById(R.id.coinsTv);
        toolbar = findViewById(R.id.toolbar);
        dailyIcon = findViewById(R.id.dailyIcon);

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.loading_dialog);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            // Permission granted or denied
                            if (!isGranted) {
                                // Explain to the user that the feature may not work properly
                                Toast.makeText(this,
                                        "Mining notifications require permission to work properly",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void checkDailyStatus() {
        if (user == null) return;

        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        String dateString = dateFormat.format(currentDate);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("DailyCheck").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        String dbDateString = snapshot.child("date").getValue(String.class);
                        if (dbDateString != null) {
                            Date dbDate = dateFormat.parse(dbDateString);
                            Date today = dateFormat.parse(dateString);

                            // You can set a visual indicator that daily check is available
                            // But since there's no dailyBadge, we'll just use the dailyIcon visibility
                            if (today.after(dbDate)) {
                                dailyIcon.setVisibility(View.VISIBLE);
                            } else {
                                dailyIcon.setVisibility(View.VISIBLE); // Keep icon visible but maybe change its appearance
                            }
                        }
                    } else {
                        // First time user, daily check is available
                        dailyIcon.setVisibility(View.VISIBLE);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error checking daily status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDataFromDatabase() {
        dialog.show();
        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProfileModel model = snapshot.getValue(ProfileModel.class);
                if (model != null) {
                    nameTv.setText(model.getName());
                    emailTv.setText(model.getEmail());
                    coinsTv.setText(String.valueOf(model.getCoins()));
                    Glide.with(MainActivity.this).load(model.getImage()).timeout(6000).placeholder(R.drawable.profile).into(profileImage);
                    dialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkInternetConnection() {
        if (internet.isConnected()) {
            new isInternetActive().execute();
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void dailyCheck() {
        if (internet.isConnected()) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Loading");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Date currentDate = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            String dateString = dateFormat.format(currentDate);

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
            dbRef.child("DailyCheck").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            String dbDateString = snapshot.child("date").getValue(String.class);
                            if (dbDateString != null) {
                                Date dbDate = dateFormat.parse(dbDateString);
                                Date today = dateFormat.parse(dateString);

                                if (today.after(dbDate)) {
                                    updateDailyCheck(progressDialog, dateString);
                                } else {
                                    showAlreadyCheckedDialog(progressDialog);
                                }
                            }
                        } else {
                            updateDailyCheck(progressDialog, dateString);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Date parsing error", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }

    class isInternetActive extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            InputStream inputStream = null;
            String json = "";
            try {
                String strURL = "https://icons.iconarchive.com/";
                URL url = new URL(strURL);
                URLConnection urlConnection = url.openConnection();
                urlConnection.setDoInput(true);
                inputStream = urlConnection.getInputStream();
                json = "Success";
            } catch (Exception e) {
                e.printStackTrace();
                json = "Failed";
            }
            return json;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                if (s.equals("Success")) {
                    Toast.makeText(MainActivity.this, "Internet Connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "No Internet Access", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "No Internet", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "Validating Internet", Toast.LENGTH_SHORT).show();
            super.onPreExecute();
        }
    }

    private void updateDailyCheck(ProgressDialog dialog, String dateString) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProfileModel model = snapshot.getValue(ProfileModel.class);
                if (model != null) {
                    int currentCoins = model.getCoins();
                    int updateCoins = currentCoins + 10;
                    int spinC = model.getSpins();
                    int updateSpins = spinC + 2;

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put("coins", updateCoins);
                    updates.put("spins", updateSpins);

                    userRef.updateChildren(updates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            HashMap<String, Object> dateMap = new HashMap<>();
                            dateMap.put("date", dateString);

                            FirebaseDatabase.getInstance().getReference("DailyCheck").child(user.getUid()).setValue(dateMap).addOnCompleteListener(dateTask -> {
                                if (dateTask.isSuccessful()) {
                                    // Removed reference to null dailyBadge
                                    showSuccessDialog(dialog);
                                } else {
                                    showErrorDialog(dialog, "Failed to update date");
                                }
                            });
                        } else {
                            showErrorDialog(dialog, "Failed to update coins");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showErrorDialog(dialog, error.getMessage());
            }
        });
    }

    private void showSuccessDialog(ProgressDialog progressDialog) {
        progressDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Success");
        builder.setMessage("You have checked in today! Received 10 coins and 2 spins.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showAlreadyCheckedDialog(ProgressDialog progressDialog) {
        progressDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Already Checked");
        builder.setMessage("You've already checked in today");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showErrorDialog(ProgressDialog progressDialog, String message) {
        progressDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}