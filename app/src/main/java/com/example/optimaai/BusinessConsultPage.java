package com.example.optimaai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BusinessConsultPage extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText promptEditText;
    private MaterialButton sendPromptButton;
    private LottieAnimationView loadingAnimationView; // Menggunakan Lottie
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private ChatFutures chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_consult_page);

        // Mengatasi masalah keyboard menutupi input
        View rootView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            View inputLayout = findViewById(R.id.inputLayout);
            inputLayout.setPadding(inputLayout.getPaddingLeft(), inputLayout.getPaddingTop(), inputLayout.getPaddingRight(), bottomInset);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        promptEditText = findViewById(R.id.promptEditText);
        sendPromptButton = findViewById(R.id.sendPromptButton);
        loadingAnimationView = findViewById(R.id.loadingAnimationView); // Inisialisasi Lottie

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Inisialisasi model Gemini untuk mode chat
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        chat = model.startChat();

        sendPromptButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String prompt = promptEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            return;
        }

        addMessage(prompt, true);
        promptEditText.setText("");
        loadingAnimationView.setVisibility(View.VISIBLE); // Tampilkan animasi

        Content userContent = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userContent);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponse = result.getText();
                runOnUiThread(() -> {
                    addMessage(aiResponse, false);
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    addMessage("Maaf, terjadi kesalahan. Coba lagi.", false);
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void addMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}