package com.example.optimaai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Profile_Page extends AppCompatActivity {

    private TextView profileNameTextView, profileEmailTextView, logoutTextView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        profileNameTextView = findViewById(R.id.profileNameTextView);
        profileEmailTextView = findViewById(R.id.profileEmailTextView);
        logoutTextView = findViewById(R.id.logoutTextView);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadUserData();

        logoutTextView.setOnClickListener(v -> logoutUser());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });
    }
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            if (name == null || name.isEmpty()) {
                profileNameTextView.setText(email);
            } else {
                profileNameTextView.setText(name);
            }

            profileEmailTextView.setText(email);

        } else {
            Toast.makeText(this, "Sesi tidak valid, silakan login kembali.", Toast.LENGTH_SHORT).show();
            goToLoginPage();
        }
    }


    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logout berhasil.", Toast.LENGTH_SHORT).show();
        goToLoginPage();
    }

    private void goToLoginPage() {
        Intent intent = new Intent(Profile_Page.this, Login_Page.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}