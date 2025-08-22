package com.example.optimaai.ui.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.optimaai.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgotPassword_Page extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private Button sendLinkButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        Toolbar toolbar = findViewById(R.id.toolbar_forgot_password);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        emailEditText = findViewById(R.id.emailEditText);
        sendLinkButton = findViewById(R.id.sendLinkButton);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();

        sendLinkButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email cannot be empty");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Fill the valid email!");
            emailEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        sendLinkButton.setEnabled(false);

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ForgotPassword_Page.this, "Succeed sending reset password link!",
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(ForgotPassword_Page.this, "Failed to send link. Try again!" + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
            }
            progressBar.setVisibility(View.GONE);
            sendLinkButton.setEnabled(true);
        });
    }
}