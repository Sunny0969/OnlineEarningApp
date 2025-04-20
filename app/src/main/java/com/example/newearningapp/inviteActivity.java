package com.example.newearningapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;  // Using android.widget.Toolbar

import com.example.newearningapp.model.ProfileModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class inviteActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private String oppositeUID;
    private TextView referCodeTV;
    private ImageView backBtn;

    private Button shareBtn, redeemBtn;
    DatabaseReference reference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);
        init();
        reference = FirebaseDatabase.getInstance().getReference().child("Users");
        loadData();
        redeemAvailability();
        clickListener();

    }

    private void init() {
        // Using setActionBar for android.widget.Toolbar instead of setSupportActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);


        referCodeTV = findViewById(R.id.referCodeTV);
        shareBtn = findViewById(R.id.shareBtn);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        redeemBtn = findViewById(R.id.redeemBtn);
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> onBackPressed());

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

        finish();
    }

    private void loadData() {
        if (user != null) {
            reference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("referCode")) {
                        String referCode = snapshot.child("referCode").getValue(String.class);
                        if (referCode != null) {
                            referCodeTV.setText(referCode);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(inviteActivity.this, "Something went wrong: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private void clickListener() {
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String referCode = referCodeTV.getText().toString().trim();
                String shareBody = "Hey, I am using online earning app with this code: " + "coins. " + "My invitation code is " + referCode + "\n" + "Download from Play Store \n" + "https://play.google.com/store/apps/details?id=" + getPackageName();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(intent);
            }
        });

        redeemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = new EditText(inviteActivity.this);
                editText.setHint("Enter coins");
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                editText.setLayoutParams(layoutParams);

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(inviteActivity.this);
                alertDialog.setTitle("Claim Code");
                alertDialog.setView(editText);
                alertDialog.setPositiveButton("Redeem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String inputCode = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(inputCode)) {
                            Toast.makeText(inviteActivity.this, "Please enter valid code", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (inputCode.equals(referCodeTV.getText().toString())) {
                            Toast.makeText(inviteActivity.this, "You can't redeem your own code", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        redeemQuery(inputCode, dialog);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });
    }

    private void redeemQuery(String inputCode, final DialogInterface dialog) {
        Query query = reference.orderByChild("referCode").equalTo(inputCode);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    oppositeUID = dataSnapshot.getKey();

                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                ProfileModel model = snapshot.child(oppositeUID).getValue(ProfileModel.class);
                                ProfileModel myModel = snapshot.child(user.getUid()).getValue(ProfileModel.class);

                                if (model == null || myModel == null) {
                                    Toast.makeText(inviteActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    return;
                                }

                                int coins = model.getCoins();
                                int updateCoins = coins + 100;

                                int myCoins = myModel.getCoins();
                                int myUpdate = myCoins - 100;

                                HashMap<String, Object> map = new HashMap<>();
                                map.put("coins", updateCoins);
                                map.put("Claimed", true);

                                HashMap<String, Object> myMap = new HashMap<>();
                                myMap.put("coins", myUpdate);

                                reference.child(oppositeUID).updateChildren(map);
                                reference.child(user.getUid()).updateChildren(myMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        dialog.dismiss();
                                        Toast.makeText(inviteActivity.this, "Coins redeemed successfully", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                Toast.makeText(inviteActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(inviteActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });

                    break; // Process only the first match
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(inviteActivity.this, "Something went wrong: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    private void redeemAvailability() {
        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("Claimed")) {
                    boolean isAvailable = snapshot.child("").getValue(Boolean.class);
                    if (isAvailable) {
                        redeemBtn.setVisibility(View.GONE);
                        redeemBtn.setEnabled(false);
                    } else {
                        redeemBtn.setEnabled(true);
                        redeemBtn.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}