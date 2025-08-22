package com.example.optimaai.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
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

import com.example.optimaai.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class Profile_Page extends AppCompatActivity implements View.OnClickListener{
    private TextView profileNameTextView;
    private TextView profileEmailTextView;
    private FirebaseAuth mAuth;
    private MaterialCardView businessInfoNotificationCard;
    private Button logoutButton;
    private MaterialButton skipButton, fillNowButton;
    private Toolbar toolbar;
    private NestedScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_page);

        initializeFirebase();
        initializeViews();
        loadUserData();
        checkBusinessInfoStatusAsync();
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("User Profile");
        }

        skipButton.setOnClickListener(this);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        logoutButton.setOnClickListener(v -> logoutUser());
        fillNowButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == skipButton) {
            businessInfoNotificationCard.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> businessInfoNotificationCard.setVisibility(View.GONE))
                    .start();
        } else if (v == fillNowButton){
            Intent intent = new Intent(Profile_Page.this, BusinessActivity_Page.class);
            startActivity(intent);
        }
    }

    private void initializeFirebase(){
        mAuth = FirebaseAuth.getInstance();
    }
    private void initializeViews() {
        profileNameTextView = findViewById(R.id.profileNameTextView);
        profileEmailTextView = findViewById(R.id.profileEmailTextView);
        logoutButton = findViewById(R.id.logoutButton);
        toolbar = findViewById(R.id.toolbar);
        scrollView = findViewById(R.id.profile_scroll_view);

        businessInfoNotificationCard = findViewById(R.id.businessInfoNotificationCard);
        skipButton = findViewById(R.id.skipButton);
        fillNowButton = findViewById(R.id.fillNowButton);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            profileNameTextView.setText(name != null && !name.isEmpty() ? name : "Name Not Set");
            profileEmailTextView.setText(email != null ? email : "Email not available");


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

//                            businessNameEditText.setText(businessName);
//                            businessIndustryEditText.setText(businessIndustry);
//                            businessTargetEditText.setText(businessTarget);
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