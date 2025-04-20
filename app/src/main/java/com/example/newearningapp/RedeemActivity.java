package com.example.newearningapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.newearningapp.fragment.FragmentReplacerActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RedeemActivity extends AppCompatActivity {

    private static final double COIN_TO_DOLLAR_RATE = 0.01; // 10 coins = $0.1
    private static final double WITHDRAWAL_FEE = 0.01; // $0.01 fee
    private static final int MINIMUM_COINS = 500; // $5 equivalent

    // UI Components
    private CardView amazonCard;
    private ImageView backBtn;

    private RadioGroup networkTypeGroup;
    private EditText binanceAddressInput, coinsInput;
    private Button withdrawBtn;
    private TextView coinAmount, dollarAmount, feeAmount, totalAmount, userCoinsDisplay;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // User Data
    private int userCoins = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupListeners();
        loadUserCoins();
    }
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        amazonCard = findViewById(R.id.amazonGiftCard);
        networkTypeGroup = findViewById(R.id.networkTypeGroup);
        binanceAddressInput = findViewById(R.id.binanceAddressInput);
        coinsInput = findViewById(R.id.coinsInput);
        withdrawBtn = findViewById(R.id.withdrawBtn);
        coinAmount = findViewById(R.id.coinAmount);
        dollarAmount = findViewById(R.id.dollarAmount);
        feeAmount = findViewById(R.id.feeAmount);
        totalAmount = findViewById(R.id.totalAmount);
        userCoinsDisplay = findViewById(R.id.userCoinsDisplay);


        // Set default fee
        feeAmount.setText("$" + new DecimalFormat("0.00").format(WITHDRAWAL_FEE));
    }

    private void setupListeners() {

        // Amazon gift card click listener
        amazonCard.setOnClickListener(v -> {
            Intent intent = new Intent(RedeemActivity.this, FragmentReplacerActivity.class);
            intent.putExtra("position", 1);
            startActivity(intent);
        });

        // Coins input text change listener
        coinsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateWithdrawalSummary();
            }
        });

        // Withdraw button click listener
        withdrawBtn.setOnClickListener(v -> processWithdrawal());
    }

    private void loadUserCoins() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mDatabase.child("Users").child(currentUser.getUid()).child("coins")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                userCoins = snapshot.getValue(Integer.class);
                                userCoinsDisplay.setText("Your balance: " + userCoins + " coins");
                            } else {
                                userCoins = 0;
                                userCoinsDisplay.setText("Your balance: 0 coins");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(RedeemActivity.this,
                                    "Failed to load your coin balance", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "You need to be logged in to withdraw coins", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateWithdrawalSummary() {
        try {
            int coins = coinsInput.getText().toString().isEmpty() ? 0 :
                    Integer.parseInt(coinsInput.getText().toString());

            coinAmount.setText(String.valueOf(coins));
            double dollars = coins * COIN_TO_DOLLAR_RATE / 10;
            dollarAmount.setText("$" + new DecimalFormat("0.00").format(dollars));

            double total = Math.max(0, dollars - WITHDRAWAL_FEE);
            totalAmount.setText("$" + new DecimalFormat("0.00").format(total));

            // Validate coins
            if (coins > userCoins) {
                withdrawBtn.setEnabled(false);
                withdrawBtn.setAlpha(0.5f);
                coinsInput.setError("Insufficient coins");
            } else if (coins < MINIMUM_COINS && coins > 0) {
                withdrawBtn.setEnabled(false);
                withdrawBtn.setAlpha(0.5f);
                coinsInput.setError("Minimum " + MINIMUM_COINS + " coins");
            } else {
                withdrawBtn.setEnabled(coins > 0);
                withdrawBtn.setAlpha(coins > 0 ? 1.0f : 0.5f);
                coinsInput.setError(null);
            }

        } catch (NumberFormatException e) {
            coinAmount.setText("0");
            dollarAmount.setText("$0.00");
            totalAmount.setText("$0.00");
            withdrawBtn.setEnabled(false);
            withdrawBtn.setAlpha(0.5f);
        }
    }

    private void processWithdrawal() {
        String binanceAddress = binanceAddressInput.getText().toString().trim();
        if (binanceAddress.isEmpty()) {
            binanceAddressInput.setError("Please enter your Binance address");
            return;
        }

        int selectedId = networkTypeGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select network type", Toast.LENGTH_SHORT).show();
            return;
        }

        String coinsStr = coinsInput.getText().toString().trim();
        if (coinsStr.isEmpty()) {
            coinsInput.setError("Please enter coin amount");
            return;
        }

        int coins = Integer.parseInt(coinsStr);
        if (coins < MINIMUM_COINS) {
            coinsInput.setError("Minimum withdrawal is " + MINIMUM_COINS + " coins ($5)");
            return;
        }

        if (coins > userCoins) {
            new AlertDialog.Builder(this)
                    .setTitle("Insufficient Coins")
                    .setMessage("You don't have enough coins for this withdrawal")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        RadioButton radioButton = findViewById(selectedId);
        String networkType = radioButton.getText().toString();
        double dollarValue = coins * COIN_TO_DOLLAR_RATE / 10;
        double finalAmount = dollarValue - WITHDRAWAL_FEE;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Withdrawal")
                .setMessage("Withdraw " + coins + " coins ($" +
                        new DecimalFormat("0.00").format(finalAmount) +
                        ") to:\n" + binanceAddress + "\nvia " + networkType + " network?")
                .setPositiveButton("Confirm", (dialog, which) ->
                        submitWithdrawal(coins, dollarValue, finalAmount, binanceAddress, networkType))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitWithdrawal(int coins, double dollarValue, double finalAmount,
                                  String address, String network) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to withdraw", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare withdrawal data
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String withdrawalId = mDatabase.child("withdrawals").push().getKey();

        Map<String, Object> withdrawalData = new HashMap<>();
        withdrawalData.put("coins", coins);
        withdrawalData.put("dollarAmount", dollarValue);
        withdrawalData.put("fee", WITHDRAWAL_FEE);
        withdrawalData.put("finalAmount", finalAmount);
        withdrawalData.put("address", address);
        withdrawalData.put("network", network);
        withdrawalData.put("status", "pending");
        withdrawalData.put("timestamp", System.currentTimeMillis());
        withdrawalData.put("date", currentTime);
        withdrawalData.put("referenceId", generateReferenceId());
        withdrawalData.put("userId", currentUser.getUid());

        // Transaction-like operation
        mDatabase.child("Users").child(currentUser.getUid()).child("coins")
                .setValue(userCoins - coins)
                .addOnCompleteListener(deductTask -> {
                    if (deductTask.isSuccessful()) {
                        // Save withdrawal record
                        mDatabase.child("withdrawals").child(withdrawalId)
                                .setValue(withdrawalData)
                                .addOnCompleteListener(withdrawalTask -> {
                                    if (withdrawalTask.isSuccessful()) {
                                        // Also save to user's withdrawal history
                                        mDatabase.child("Users").child(currentUser.getUid())
                                                .child("withdrawals").child(withdrawalId)
                                                .setValue(withdrawalData)
                                                .addOnCompleteListener(historyTask -> {
                                                    if (historyTask.isSuccessful()) {
                                                        // Success - update UI
                                                        userCoins -= coins;
                                                        userCoinsDisplay.setText("Your balance: " + userCoins + " coins");
                                                        coinsInput.setText("");
                                                        binanceAddressInput.setText("");
                                                        updateWithdrawalSummary();

                                                        showSuccessDialog(finalAmount, withdrawalId);
                                                    } else {
                                                        refundCoins(currentUser.getUid(), coins);
                                                    }
                                                });
                                    } else {
                                        refundCoins(currentUser.getUid(), coins);
                                    }
                                });
                    } else {
                        Toast.makeText(RedeemActivity.this,
                                "Failed to process withdrawal", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void refundCoins(String userId, int coins) {
        mDatabase.child("Users").child(userId).child("coins")
                .setValue(userCoins)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(RedeemActivity.this,
                                "Error refunding coins. Please contact support", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String generateReferenceId() {
        return "TX" + System.currentTimeMillis() % 1000000;
    }

    private void showSuccessDialog(double amount, String withdrawalId) {
        new AlertDialog.Builder(this)
                .setTitle("Withdrawal Successful")
                .setMessage(String.format(Locale.getDefault(),
                        "You will receive $%.2f\n\nReference: %s\n\nProcessing time: 24-48 hours",
                        amount, withdrawalId))
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}