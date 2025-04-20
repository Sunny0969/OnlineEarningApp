package com.example.newearningapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailET, passwordET;
    private ProgressBar progressBar;
    private Button loginBtn;
    private TextView signupTV;
    private String deviceID;
    private FirebaseAuth auth;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
        auth = FirebaseAuth.getInstance();
        deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        clickListener();
    }

    private void init() {
        emailET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        progressBar = findViewById(R.id.progressBar);
        loginBtn = findViewById(R.id.loginBtn);
        signupTV = findViewById(R.id.signupTV);
    }

    private void clickListener() {
        signupTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }


        });
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Input a valid Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    passwordET.setError("Password is required");
                    Toast.makeText(LoginActivity.this, "Input a valid Password", Toast.LENGTH_SHORT).show();
                    return;
                }
                signIn(email, password);


            }
        });

    }

    private void signIn(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
//login success

                    FirebaseUser user = auth.getCurrentUser();
                    assert user != null;
                    if (user.isEmailVerified()) {
                        progressBar.setVisibility(View.GONE);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Please verify your email", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

}