package com.example.optimaai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout BusinessConsultContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        responseText = findViewById(R.id.responseText);
//        progressBar = findViewById(R.id.progressBar);
//        promptEditText = findViewById(R.id.promptEditText);
//        sendPromptButton = findViewById(R.id.sendPromptButton);

        BusinessConsultContainer = findViewById(R.id.businessConsultContainer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.API_KEY);
//        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
//
//        Executor executor = Executors.newSingleThreadExecutor();
//        sendPromptButton.setOnClickListener(v -> {
//            String prompt = promptEditText.getText().toString().trim();
//            if (prompt.isEmpty()) {
//                Toast.makeText(getApplicationContext(), "Please enter a prompt.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            progressBar.setVisibility(View.VISIBLE);
//            sendPromptButton.setEnabled(false);
//            responseText.setText("");
//
//            Content content = new Content.Builder().addText(prompt).build();
//
//            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
//            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
//                @Override
//                public void onSuccess(GenerateContentResponse result) {
//                    String resultText = result.getText();
//                    runOnUiThread(() -> {
//                        responseText.setText(resultText);
//                        progressBar.setVisibility(View.GONE);
//                        sendPromptButton.setEnabled(true);
//                    });
//                }
//
//                @Override
//                public void onFailure(Throwable t) {
//                    t.printStackTrace();
//                    runOnUiThread(() -> {
//                        Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
//                        progressBar.setVisibility(View.GONE);
//                        sendPromptButton.setEnabled(true);
//                    });
//                }
//            }, executor);
//        });
        BusinessConsultContainer.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == BusinessConsultContainer){
            Intent intent = new Intent(MainActivity.this, BusinessConsultPage.class);
            startActivity(intent);
        }
    }
}