package com.example.newearningapp.fragment;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import com.example.newearningapp.R;
import com.example.newearningapp.model.AmazonModel;
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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class AmazonFragment extends Fragment {

    private RadioGroup radioGroup;
    private Button withdrawBtn;
    private TextView coinsTV;
    DatabaseReference reference;
    FirebaseUser user;
    String name = "", email = "";
    private Dialog dialog;

    public AmazonFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_amazon, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("users");
        loadData();
        clickListener();
    }
    private void init(View view) {
        radioGroup = view.findViewById(R.id.radioGroup);
        withdrawBtn = view.findViewById(R.id.submitBtn);
        coinsTV = view.findViewById(R.id.coinsTV);

        dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.loading_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);
    }
    private void loadData() {
        if (user == null || reference == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "User data not found in database", Toast.LENGTH_SHORT).show();
                    return;
                }
                ProfileModel model = snapshot.getValue(ProfileModel.class);
                if (model != null) {
                    coinsTV.setText(String.valueOf(model.getCoins()));
                    name = model.getName();
                    email = model.getEmail();
                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
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
    private void clickListener() {
        withdrawBtn.setOnClickListener((v) -> {
            Dexter.withContext(getActivity())
                    .withPermissions(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                String filePath = Environment.getExternalStorageDirectory() + "/Earning App/Amazon Gift Card";
                                File file = new File(filePath);
                                file.mkdirs();

                                int currentCoins = 0;
                                try {
                                    currentCoins = Integer.parseInt(coinsTV.getText().toString());
                                } catch (NumberFormatException e) {
                                    Toast.makeText(getContext(), "Invalid coin value", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                int checkedId = radioGroup.getCheckedRadioButtonId();

                                if (checkedId == -1) {
                                    Toast.makeText(getContext(), "Please select a gift card option", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (checkedId == R.id.amazon25) {
                                    AmazonCard(25, currentCoins);
                                } else if (checkedId == R.id.amazon50) {
                                    AmazonCard(50, currentCoins);
                                }
                            } else {
                                Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    }).check();
        });
    }

    private void AmazonCard(int amazonCard, int currentCoins) {
        if (amazonCard == 25) {
            if (currentCoins >= 100) {
                sendGiftCard(1);
            } else {
                Toast.makeText(getContext(), "Insufficient Coins", Toast.LENGTH_SHORT).show();
            }
        } else if (amazonCard == 50) {
            if (currentCoins >= 200) {
                sendGiftCard(2);
            } else {
                Toast.makeText(getContext(), "Insufficient Coins", Toast.LENGTH_SHORT).show();
            }
        }
    }

    DatabaseReference amazonRef;
    Query query;

    private void sendGiftCard(int cardAmount) {
        dialog.show();
        amazonRef = FirebaseDatabase.getInstance().getReference().child("Gift Cards").child("Amazon");

        if (cardAmount == 1) {
            query = amazonRef.orderByChild("amzaon").equalTo(25);
        } else if (cardAmount == 2) {
            query = amazonRef.orderByChild("amzaon").equalTo(50);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    Toast.makeText(getContext(), "No gift cards available currently", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                Random random = new Random();
                int childCount = (int) snapshot.getChildrenCount();
                int rand = random.nextInt(childCount);
                Iterator<DataSnapshot> iterator = snapshot.getChildren().iterator();

                for (int i = 0; i < rand; i++) {
                    if (iterator.hasNext()) {
                        iterator.next();
                    }
                }

                if (iterator.hasNext()) {
                    DataSnapshot childSnap = iterator.next();
                    AmazonModel model = childSnap.getValue(AmazonModel.class);

                    if (model != null) {
                        String id = model.getId();
                        String giftCode = model.getAmazonCode();
                        printAmazonCode(id, giftCode, cardAmount);
                    } else {
                        Toast.makeText(getContext(), "Error retrieving gift card data", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                } else {
                    Toast.makeText(getContext(), "Error retrieving gift card", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    private void printAmazonCode(String id, String amazonCode, int cardAmount) {
        try {
            updateDate(cardAmount, id);
            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            String currentTime = dateFormat.format(date);
            String text = "Date: " + currentTime + "\n" +
                    "Name: " + name + "\n" +
                    "Email: " + email + "\n" +
                    "Redeem ID: " + id + "\n\n" +
                    "Gift Card Code: " + amazonCode;

            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(800, 800, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(20);

            String[] lines = text.split("\n");
            float y = 50;
            for (String line : lines) {
                page.getCanvas().drawText(line, 50, y, paint);
                y += 40;
            }

            pdfDocument.finishPage(page);

            String filePath = Environment.getExternalStorageDirectory() + "/Earning App/Amazon Gift Card/" +
                    System.currentTimeMillis() + "_" + user.getUid() + "_amazonCode.pdf";
            File file = new File(filePath);

            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();

            viewPdfFile(file);

        } catch (FileNotFoundException e) {
            Toast.makeText(getContext(), "File not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    private void viewPdfFile(File file) {
        if (getContext() == null) {
            dialog.dismiss();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            uri = androidx.core.content.FileProvider.getUriForFile(
                    getContext(),
                    getContext().getApplicationContext().getPackageName() + ".provider",
                    file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }

        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        try {
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Please install a PDF reader app", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDate(int cardAmount, String id) {
        HashMap<String, Object> map = new HashMap<>();
        int currentCoins = 0;

        try {
            currentCoins = Integer.parseInt(coinsTV.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid coin value", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        int updatedCoins = 0;
        if (cardAmount == 1) {
            updatedCoins = currentCoins - 100;
            map.put("coins", updatedCoins);
        } else if (cardAmount == 2) {
            updatedCoins = currentCoins - 200;
            map.put("coins", updatedCoins);
        }

        reference.child(user.getUid()).updateChildren(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Gift Card Sent", Toast.LENGTH_SHORT).show();

                // Remove the gift card from database
                FirebaseDatabase.getInstance().getReference()
                        .child("Gift Cards")
                        .child("Amazon")
                        .child(id)
                        .removeValue()
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Warning: Card may not be removed from database",
                                        Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(getContext(), "Failed to update coins", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
    }
}