package com.example.newearningapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private Button registerBtn;
    private EditText nameEdit, emailEdit, passwordEdit, confirmPassEdit;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private TextView loginTV;
    private String deviceID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();


        init();
        clickListener();
    }

    private void init() {
        registerBtn = findViewById(R.id.registerBtn);
        nameEdit = findViewById(R.id.nameET);
        emailEdit = findViewById(R.id.emailET);
        passwordEdit = findViewById(R.id.passwordET);
        confirmPassEdit = findViewById(R.id.confirmPass);
        progressBar = findViewById(R.id.progressBar);
        loginTV = findViewById(R.id.login_TV);
    }

    private void clickListener() {

        loginTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        registerBtn.setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();
            String confirmPass = confirmPassEdit.getText().toString().trim();

            if (name.isEmpty()) {
                nameEdit.setError("Name is required");
                nameEdit.requestFocus();
                return;
            }
            if (email.isEmpty()) {
                emailEdit.setError("Email is required");
                emailEdit.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                passwordEdit.setError("Password is required");
                passwordEdit.requestFocus();
                return;
            }
            if (password.length() < 6) {
                passwordEdit.setError("Password should be at least 6 characters");
                passwordEdit.requestFocus();
                return;
            }
            if (confirmPass.isEmpty() || !password.equals(confirmPass)) {
                confirmPassEdit.setError("Passwords don't match");
                confirmPassEdit.requestFocus();
                return;
            }

            // Directly create account without device check
            createAccount(email, password);
        });
    }

    private void createAccount(final String email, final String password) {
        progressBar.setVisibility(View.VISIBLE);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        assert user != null;
                        auth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {

//                                            queryAccountExistence(user, email);

                                            updateUI(user, email);

                                        } else {
                                            Toast.makeText(RegisterActivity.this,
                                                    "Error: " + task.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void queryAccountExistence(String email, String password) {
//        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
//
//        Query query = reference.orderByChild("deviceID").equalTo(deviceID);
//        query.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    Toast.makeText(RegisterActivity.this, "Device already Registered with another email. PLease Login", Toast.LENGTH_SHORT).show();
//                } else {
////                    updateUI(user, email);
//
//                    createAccount(email, password);
//
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
        createAccount(email, password);
    }

    private void updateUI(FirebaseUser user, String email) {
        progressBar.setVisibility(View.VISIBLE);
        String refer = email.substring(0, email.lastIndexOf("@"));
        String referCode = refer.replace(".", "");
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", nameEdit.getText().toString().trim());
        userMap.put("email", email);
        userMap.put("uid", user.getUid());
        userMap.put("image", "");
        userMap.put("coins", 0);
        userMap.put("referCode", referCode);
        userMap.put("spins", 2);
        //userMap.put("DeviceID", deviceID);

        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        Date previousDate = calendar.getTime();
        String dateString = dateFormat.format(previousDate);

        // First save daily check data
        FirebaseDatabase.getInstance().getReference()
                .child("Daily Check")
                .child(user.getUid())
                .child("date")
                .setValue(dateString)
                .addOnCompleteListener(dailyCheckTask -> {
                    if (dailyCheckTask.isSuccessful()) {
                        // Then save user data
                        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users");
                        databaseRef.child(user.getUid())
                                .setValue(userMap)
                                .addOnCompleteListener(task -> {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Registration successful! Please verify your email.", Toast.LENGTH_SHORT).show();

                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this,
                                                "Database error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this,
                                "Failed to save daily check data: " + (dailyCheckTask.getException() != null ? dailyCheckTask.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}