package com.example.optimaai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Profile_Page extends AppCompatActivity {

    // Declaration elements
    private TextView profileNameTextView;
    private TextView profileEmailTextView;
    private FirebaseAuth mAuth;
    private MaterialCardView businessInfoNotificationCard;
    private TextInputEditText businessNameEditText, businessTargetEditText, businessIndustryEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_page);

        mAuth = FirebaseAuth.getInstance();

        profileNameTextView = findViewById(R.id.profileNameTextView);
        profileEmailTextView = findViewById(R.id.profileEmailTextView);
        TextView logoutTextView = findViewById(R.id.logoutTextView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        NestedScrollView scrollView = findViewById(R.id.profile_scroll_view);

        businessInfoNotificationCard = findViewById(R.id.businessInfoNotificationCard);
        MaterialButton skipButton = findViewById(R.id.skipButton);
        MaterialButton fillNowButton = findViewById(R.id.fillNowButton);

        businessNameEditText = findViewById(R.id.businessNameEditText);
        businessTargetEditText = findViewById(R.id.targetMarketEditText);
        businessIndustryEditText = findViewById(R.id.industryEditText);
        MaterialButton saveBusinessInfoButton = findViewById(R.id.saveBusinessProfileButton);

        businessNameEditText = findViewById(R.id.businessNameEditText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("User Profile");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadUserData();

        logoutTextView.setOnClickListener(v -> logoutUser());

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        checkBusinessInfoStatusAsync();

        skipButton.setOnClickListener(v -> {
            businessInfoNotificationCard.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> businessInfoNotificationCard.setVisibility(View.GONE))
                    .start();
        });

        fillNowButton.setOnClickListener(v -> businessNameEditText.requestFocus());

        saveBusinessInfoButton.setOnClickListener(v -> saveBusinessInfo());
    }

    private void saveBusinessInfo() {
        String businessName = Objects.requireNonNull(businessNameEditText.getText()).toString().trim();
        String businessIndustry = Objects.requireNonNull(businessIndustryEditText.getText()).toString().trim();
        String businessTarget = Objects.requireNonNull(businessTargetEditText.getText()).toString().trim();

        if (businessName.isEmpty() || businessIndustry.isEmpty() || businessTarget.isEmpty()) {
            Toast.makeText(this, "Please fill in all business information", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        Map<String, Object> businessData = new HashMap<>();
        businessData.put("businessName", businessName);
        businessData.put("businessIndustry", businessIndustry);
        businessData.put("businessTarget", businessTarget);

        userRef.child("businessProfile").setValue(businessData)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferences.Editor editor = getSharedPreferences("BusinessProfile", MODE_PRIVATE).edit();
                    editor.putBoolean("isFilled", true);
                    editor.apply();

                    businessInfoNotificationCard.setVisibility(View.GONE);
                    Toast.makeText(Profile_Page.this, "Business information saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Profile_Page.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            // 2. Tampilkan di UI
            profileNameTextView.setText(name != null && !name.isEmpty() ? name : "Nama Belum Diatur");
            profileEmailTextView.setText(email != null ? email : "Email tidak tersedia");


            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);

                        profileNameTextView.setText(name != null ? name : "Name not available");
                        profileEmailTextView.setText(email != null ? email : "Email not available");

                        if (snapshot.hasChild("businessProfile")) {
                            String businessName = snapshot.child("businessProfile/businessName").getValue(String.class);
                            String businessIndustry =
                                    snapshot.child("businessProfile/businessIndustry").getValue(String.class);
                            String businessTarget =
                                    snapshot.child("businessProfile/businessTarget").getValue(String.class);

                            businessNameEditText.setText(businessName);
                            businessIndustryEditText.setText(businessIndustry);
                            businessTargetEditText.setText(businessTarget);
                        }
                    } else {
                        Toast.makeText(Profile_Page.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(Profile_Page.this, "Failed to load data:" + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(Profile_Page.this, Login_Page.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isBusinessInfoFilled() {
        SharedPreferences prefs = getSharedPreferences("BusinessProfile", MODE_PRIVATE);
        return prefs.getBoolean("isFilled", false);
    }

    private void checkBusinessInfoStatusAsync() {
        new Thread(() -> {
            final boolean isFilled = isBusinessInfoFilled();
            runOnUiThread(() -> {
                if (isFilled) {
                    businessInfoNotificationCard.setVisibility(View.GONE);
                } else {
                    businessInfoNotificationCard.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
}
