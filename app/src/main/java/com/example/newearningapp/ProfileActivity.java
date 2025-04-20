package com.example.newearningapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newearningapp.model.ProfileModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private CircleImageView profileImage;
    private TextView nameTV, emailTV, shareTV, redeemHistoryTV, logoutTV, coinsTV;
    private ImageButton imageButton;
    private Button updateBtn;
    private FirebaseUser user;
    private FirebaseAuth auth;
    private DatabaseReference reference;
    private static final int IMAGE_PICKER = 1;
    private Uri photoUri;
    private String imageUrl;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        init();
        if (user != null) {
            loadDataFromDatabase();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
        clickListener();
    }

    private void init() {
        profileImage = findViewById(R.id.profileImage);
        nameTV = findViewById(R.id.nameTV);
        emailTV = findViewById(R.id.emailTV);
        shareTV = findViewById(R.id.shareTV);
        redeemHistoryTV = findViewById(R.id.redeemHistory);
        logoutTV = findViewById(R.id.logoutTv);
        coinsTV = findViewById(R.id.coinsTV);
        imageButton = findViewById(R.id.editImage);
        updateBtn = findViewById(R.id.updateBtn);
        updateBtn.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Get the correct reference to the database
        reference = FirebaseDatabase.getInstance().getReference().child("Users");

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCancelable(false);
    }

    private void loadDataFromDatabase() {
        reference.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProfileModel model = snapshot.getValue(ProfileModel.class);
                if (model != null) {
                    nameTV.setText(model.getName());
                    emailTV.setText(model.getEmail());
                    coinsTV.setText(String.valueOf(model.getCoins()));
                    if (model.getImage() != null && !model.getImage().isEmpty()) {
                        Glide.with(ProfileActivity.this).load(model.getImage()).timeout(6000).placeholder(R.drawable.profile).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clickListener() {
        logoutTV.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });

        shareTV.setOnClickListener(v -> {
            String shareBody = "Check out the best earning app. Download " + getString(R.string.app_name) + " from Playstore\nhttps://play.google.com/store/apps/details?id=" + getPackageName();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(intent, "Share via"));
        });

        imageButton.setOnClickListener(v -> checkPermissionsAndPickImage());

        updateBtn.setOnClickListener(v -> uploadImage());

        redeemHistoryTV.setOnClickListener(v -> {
            // Handle redeem history click
            // Add your redemption history activity intent here
        });
    }

    private void checkPermissionsAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, we don't need storage permissions for gallery access
            openImagePicker();
        } else {
            Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    if (report.areAllPermissionsGranted()) {
                        openImagePicker();
                    } else {
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Permissions are required to select image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();
        }
    }

    private void showSettingsDialog() {
        Toast.makeText(this, "Go to Settings > Apps > Your App > Permissions to grant permissions", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICKER && resultCode == RESULT_OK && data != null) {
            photoUri = data.getData();
            if (photoUri != null) {
                updateBtn.setVisibility(View.VISIBLE);
                profileImage.setImageURI(photoUri);
            }
        }
    }

    private void uploadImage() {
        if (photoUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        // Debug log
        Log.d(TAG, "Starting image upload...");

        // Create a timestamp-based filename to avoid caching issues
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = user.getUid() + "_" + timestamp + ".jpg";

        // Get storage reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference fileRef = storageRef.child("profile_images").child(filename);

        // Upload file to Firebase Storage
        fileRef.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "File uploaded successfully to storage. Getting download URL...");

                    // Get the download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Got the download URL
                        imageUrl = uri.toString();
                        Log.d(TAG, "Got download URL: " + imageUrl);

                        // Update database with new image URL
                        updateProfileInDatabase(imageUrl);

                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                        Toast.makeText(ProfileActivity.this,
                                "Failed to get download URL: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    Toast.makeText(ProfileActivity.this,
                            "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading: " + (int) progress + "%");
                });
    }

    private void updateProfileInDatabase(String imageUrl) {
        // Create map with image URL
        HashMap<String, Object> map = new HashMap<>();
        map.put("image", imageUrl);

        Log.d(TAG, "Updating database with image URL: " + imageUrl);
        Log.d(TAG, "User ID: " + user.getUid());

        // Update user profile in database
        reference.child(user.getUid())
                .updateChildren(map)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    updateBtn.setVisibility(View.GONE);
                    Log.d(TAG, "Database updated successfully");
                    Toast.makeText(ProfileActivity.this,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Database update failed: " + e.getMessage());
                    Toast.makeText(ProfileActivity.this,
                            "Database update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}