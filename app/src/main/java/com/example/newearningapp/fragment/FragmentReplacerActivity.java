package com.example.newearningapp.fragment;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.newearningapp.R;

public class FragmentReplacerActivity extends AppCompatActivity {

    private FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_replacer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        frameLayout = findViewById(R.id.frameLayout);
        int position = getIntent().getIntExtra("position", 0);
        if (position == 1) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle("Amazon Gift Card");
            fragmentReplacer(new AmazonFragment());
        }

        if (position == 2) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle("Lucky Spin");
            fragmentReplacer(new LuckySpin());
        }
        if (position == 3) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle("About");
            fragmentReplacer(new About());
        }
        if (position == 4) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle("Mining");
            fragmentReplacer(new mining());
        }
    }

    public void fragmentReplacer(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        fragmentTransaction.replace(frameLayout.getId(), fragment);
        fragmentTransaction.commit();


    }
}