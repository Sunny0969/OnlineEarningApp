package com.example.newearningapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.newearningapp.MainActivity;
import com.example.newearningapp.R;
import com.example.newearningapp.model.ProfileModel;
import com.example.newearningapp.spin.SpinItem;
import com.example.newearningapp.spin.WheelView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class LuckySpin extends Fragment {

    private Button playBtn;
    private WheelView wheelView;
    List<SpinItem> spinItemList = new ArrayList<>();
    int currentSpins;
    private FirebaseUser user;
    DatabaseReference reference;
    private TextView coinsTv;
    private ImageView backBtn;

    public LuckySpin() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lucky_spin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
        spinList();
        clickListener();
        loadData();
    }

    private void init(View view) {
        playBtn = view.findViewById(R.id.playBtn);
        wheelView = view.findViewById(R.id.wheelView);
        coinsTv = view.findViewById(R.id.coinsTV);
        backBtn = view.findViewById(R.id.backBtn);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid());
    }

    private void spinList() {
        SpinItem item1 = new SpinItem();
        item1.Text = "0";
        item1.color = 0xffFFEB3B;
        spinItemList.add(item1);

        SpinItem item2 = new SpinItem();
        item2.Text = "1";
        item2.color = 0xffFFE0B2;
        spinItemList.add(item2);

        SpinItem item3 = new SpinItem();
        item3.Text = "2";
        item3.color = 0xffFFCC80;
        spinItemList.add(item3);

        SpinItem item4 = new SpinItem();
        item4.Text = "3";
        item4.color = 0xffFF9800;
        spinItemList.add(item4);

        SpinItem item5 = new SpinItem();
        item5.Text = "4";
        item5.color = 0xffFF5722;
        spinItemList.add(item5);

        SpinItem item6 = new SpinItem();
        item6.Text = "5";
        item6.color = 0xffF44336;
        spinItemList.add(item6);

        SpinItem item7 = new SpinItem();
        item7.Text = "6";
        item7.color = 0xff9C27B0;
        spinItemList.add(item7);

        SpinItem item8 = new SpinItem();
        item8.Text = "7";
        item8.color = 0xff673AB7;
        spinItemList.add(item8);

        SpinItem item9 = new SpinItem();
        item9.Text = "8";
        item9.color = 0xff3F51B5;
        spinItemList.add(item9);

        SpinItem item10 = new SpinItem();
        item10.Text = "9";
        item10.color = 0xff2196F3;
        spinItemList.add(item10);

        SpinItem item11 = new SpinItem();
        item11.Text = "10";
        item11.color = 0xff03A9F4;
        spinItemList.add(item11);

        SpinItem item12 = new SpinItem();
        item12.Text = "15";
        item12.color = 0xff00BCD4;
        spinItemList.add(item12);

        SpinItem item13 = new SpinItem();
        item13.Text = "20";
        item13.color = 0xff009688;
        spinItemList.add(item13);


        wheelView.setData(spinItemList);
        wheelView.setRound(getRandCircleRound());

        wheelView.setLuckyRoundItemSelectedListener(new WheelView.LuckyRoundItemSelectedListener() {
            @Override
            public void LuckyRoundItemSelected(int index) {
                playBtn.setEnabled(true);
                playBtn.setAlpha(1f);
                String value = spinItemList.get(index - 1).Text;
                updateDataFirebase(Integer.parseInt(value));
            }
        });
    }

    private void clickListener() {
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMainActivity();
            }
        });
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = getRandomIndex();
                if (currentSpins >= 1 && currentSpins < 3) {
                    wheelView.startWheelWithTargetIndex(index);
                    Toast.makeText(getContext(), "Watch videos to get more Spins", Toast.LENGTH_SHORT).show();

                }
                if (currentSpins < 1) {
                    playBtn.setEnabled(false);
                    playBtn.setAlpha(0.4f);
                    Toast.makeText(getContext(), "Watch videos to get more Spins", Toast.LENGTH_SHORT).show();
                } else {
                    playBtn.setEnabled(false);
                    playBtn.setAlpha(0.4f);
                    wheelView.startWheelWithTargetIndex(index);


                }
            }
        });
    }
    private void navigateToMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
    private int getRandomIndex() {
        int[] index = new int[]{1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8, 9, 10, 11};
        int random = new Random().nextInt(index.length);
        return index[random];
    }

    private int getRandCircleRound() {
        Random random = new Random();
        return random.nextInt(10) + 15;
    }

    private void loadData() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProfileModel model = snapshot.getValue(ProfileModel.class);
                if (model != null) {
                    coinsTv.setText(String.valueOf(model.getCoins()));
                    currentSpins = model.getSpins();
                    String currentSpin = "Spin " + currentSpins;
                    playBtn.setText(currentSpin);

                    // Update button state based on available spins
                    playBtn.setEnabled(currentSpins > 0);
                    playBtn.setAlpha(currentSpins > 0 ? 1f : 0.4f);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
    }
    private void updateDataFirebase(int reward) {
        try {
            int currentCoins = Integer.parseInt(coinsTv.getText().toString());
            int updatedCoins = currentCoins + reward;
            int updatedSpins = currentSpins - 1;

            HashMap<String, Object> map = new HashMap<>();
            map.put("coins", updatedCoins);
            map.put("spins", updatedSpins);

            reference.updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), reward + " Coins added!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Error parsing coins value", Toast.LENGTH_SHORT).show();
        }
    }
}