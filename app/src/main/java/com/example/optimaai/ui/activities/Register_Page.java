package com.example.optimaai.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.optimaai.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class Register_Page extends AppCompatActivity implements View.OnClickListener{

    TextInputEditText UsernameEditText, EmailEditText, PasswordEditText, ConfPasswordEditText;
    TextView AlreadyHaveAnAccount;
    Button RegisterButton;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;
    CheckBox termsCheckBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        UsernameEditText = findViewById(R.id.UsernameEditText);
        EmailEditText = findViewById(R.id.EmailEditText);
        PasswordEditText = findViewById(R.id.PasswordEditText);
        AlreadyHaveAnAccount = findViewById(R.id.AlreadyHaveAnAccount);
        ConfPasswordEditText = findViewById(R.id.ConfPasswordEditText);
        RegisterButton = findViewById(R.id.RegisterButton);
        progressBar = findViewById(R.id.progressBar);
        termsCheckBox = findViewById(R.id.termsCheckBox);

        mAuth = FirebaseAuth.getInstance();

        makeTermsClickable();

        RegisterButton.setEnabled(false);
        RegisterButton.setAlpha(0.5f);

        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            RegisterButton.setEnabled(isChecked);
            RegisterButton.setAlpha(isChecked ? 1.0f : 0.5f);
        });

        RegisterButton.setOnClickListener(this);
        AlreadyHaveAnAccount.setOnClickListener(this);

        RegisterButton.setOnClickListener(this);
        AlreadyHaveAnAccount.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == RegisterButton) {
            String Username = Objects.requireNonNull(UsernameEditText.getText()).toString();
            String Email = Objects.requireNonNull(EmailEditText.getText()).toString();
            String Password = Objects.requireNonNull(PasswordEditText.getText()).toString();
            String Confirm_Password = Objects.requireNonNull(ConfPasswordEditText.getText()).toString();

            if (Username.isEmpty()){
                UsernameEditText.setError("Cannot be empty!");
                return;
            }
            if (Email.isEmpty()){
                EmailEditText.setError("Cannot be empty!");
                return;
            }
            if (Password.isEmpty()){
                PasswordEditText.setError("Cannot be empty!");
                return;
            }
            if (Confirm_Password.isEmpty()){
                ConfPasswordEditText.setError("Cannot be empty!");
                return;
            }

            if (!Password.equals(Confirm_Password)){
                ConfPasswordEditText.setError("Passwords do not match!");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            RegisterButton.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(Email, Password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        RegisterButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) updateUserProfile(user, Username);
                        } else {
                            Toast.makeText(Register_Page.this,
                                    "Registration failed " + Objects.requireNonNull(task.getException()).getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

        } else if (view == AlreadyHaveAnAccount){
            Intent intent = new Intent(Register_Page.this, Login_Page.class);
            startActivity(intent);
            finish();
        }
    }
    private void updateUserProfile(FirebaseUser user, String username) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registration succeed!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Register_Page.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void makeTermsClickable() {
        String fullText = "I agree to the Terms & Conditions";
        String clickableText = "Terms & Conditions";

        SpannableString spannableString = new SpannableString(fullText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(Register_Page.this, TermsAndConditions_Page.class));
            }
        };

        int startIndex = fullText.indexOf(clickableText);
        int endIndex = startIndex + clickableText.length();

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsCheckBox.setText(spannableString);
        termsCheckBox.setMovementMethod(LinkMovementMethod.getInstance());
    }
}