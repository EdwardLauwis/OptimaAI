package com.example.optimaai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Login_Page extends AppCompatActivity implements View.OnClickListener {

    TextInputEditText EmailEditText, PasswordEditText;
    TextView ToRegisterPage;
    Button LoginButton;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        EmailEditText = findViewById(R.id.EmailEditText);
        PasswordEditText = findViewById(R.id.PasswordEditText);
        ToRegisterPage = findViewById(R.id.DontHaveAnAccountRegister);
        LoginButton = findViewById(R.id.LoginButton);
        progressBar = findViewById(R.id.progressBar);

        LoginButton.setOnClickListener(this);
        ToRegisterPage.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Periksa apakah pengguna sudah login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Toast.makeText(getApplicationContext(), "Already logged in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Login_Page.this, MainActivity.class);
            startActivity(intent);
            finish(); // Tutup Login_Page agar tidak kembali ke sini
        }
    }

    @Override
    public void onClick(View view) {
        if (view == LoginButton) {
            progressBar.setVisibility(View.VISIBLE);
            LoginButton.setEnabled(false);
            String Email = Objects.requireNonNull(EmailEditText.getText()).toString();
            String Password = Objects.requireNonNull(PasswordEditText.getText()).toString();

            if (Email.isEmpty()) {
                EmailEditText.setError("Cannot be empty!");
                progressBar.setVisibility(View.GONE);
                LoginButton.setEnabled(true);
                return;
            }

            if (Password.isEmpty()) {
                PasswordEditText.setError("Cannot be empty!");
                progressBar.setVisibility(View.GONE);
                LoginButton.setEnabled(true);
                return;
            }

            mAuth.signInWithEmailAndPassword(Email, Password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        LoginButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Login Succeed.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Login_Page.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(Login_Page.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else if (view == ToRegisterPage) {
            Intent intent = new Intent(Login_Page.this, Register_Page.class);
            startActivity(intent);
            finish();
        }
    }
}