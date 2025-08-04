package com.example.optimaai;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BusinessConsultPage extends AppCompatActivity implements SetToneBottomSheetFragment.ToneSelectionListener {

    private DrawerLayout drawerLayout;
    private RecyclerView chatRecyclerView;
    private EditText promptEditText;
    private LottieAnimationView loadingAnimationView;
    private ChatAdapter chatAdapter;
    private ChatHistoryAdapter historyAdapter;
    private List<ChatMessage> chatMessages;
    private List<ChatSession> chatSessionList;
    private ChatFutures chat;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentChatId = null;
    private CollectionReference messagesRef;
    private String currentAiTone = "Professional";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_consult_page);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.consult);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    BusinessConsultPage.super.onBackPressed();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        promptEditText = findViewById(R.id.promptEditText);
        MaterialButton sendPromptButton = findViewById(R.id.sendPromptButton);
        loadingAnimationView = findViewById(R.id.loadingAnimationView);
        RecyclerView drawerHistoryRecyclerView = findViewById(R.id.drawerHistoryRecyclerView);
        MaterialButton drawerNewChatButton = findViewById(R.id.drawerNewChatButton);
        ImageButton optionsMenuButton = findViewById(R.id.optionsMenuButton);


        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatSessionList = new ArrayList<>();
        historyAdapter = new ChatHistoryAdapter(chatSessionList);
        drawerHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        drawerHistoryRecyclerView.setAdapter(historyAdapter);

        initializeGenerativeModel();

        optionsMenuButton.setOnClickListener(this::showOptionsMenu);
        sendPromptButton.setOnClickListener(v -> sendMessage());
        drawerNewChatButton.setOnClickListener(v -> startNewChat());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CHAT_ID")) {
            currentChatId = intent.getStringExtra("CHAT_ID");
            setupChatCollectionRef();
            loadChatMessages();
        }

        loadChatHistoryForDrawer();
    }

    // Kode yang sudah diperbaiki
    // Kode yang sudah diperbaiki
    private void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.chat_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_set_tone) {
                SetToneBottomSheetFragment bottomSheet = SetToneBottomSheetFragment.newInstance(currentAiTone);
                // ▼▼▼ PERUBAHAN DI SINI ▼▼▼
                bottomSheet.setToneSelectionListener(BusinessConsultPage.this);
                // ▲▲▲ AKHIR PERUBAHAN ▲▲▲
                bottomSheet.show(getSupportFragmentManager(), "SetToneBottomSheet");
                return true;
            } else if (itemId == R.id.option_ai_features) {
                Toast.makeText(this, "Fitur AI diklik", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void initializeGenerativeModel() {
        String apiKey;
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            apiKey = appInfo.metaData.getString("com.google.ai.client.generativeai.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("BusinessConsultPage", "Failed to load meta-data", e);
            Toast.makeText(this, "Error: API Key not found in manifest.", Toast.LENGTH_LONG).show();
            return;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e("BusinessConsultPage", "API Key is null or empty.");
            Toast.makeText(this, "Error: API Key is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", apiKey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        chat = model.startChat();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.consult_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_history_menu) {
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startNewChat() {
        drawerLayout.closeDrawer(GravityCompat.END);
        Intent intent = new Intent(this, BusinessConsultPage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadChatHistoryForDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("chats")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("BusinessConsultPage", "Listen failed.", error);
                        return;
                    }

                    if (value == null) return;

                    chatSessionList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        ChatSession session = doc.toObject(ChatSession.class);
                        session.setId(doc.getId());
                        chatSessionList.add(session);
                    }
                    historyAdapter.notifyDataSetChanged();
                });
    }

    private void setupChatCollectionRef() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && currentChatId != null) {
            messagesRef = db.collection("users").document(user.getUid())
                    .collection("chats").document(currentChatId)
                    .collection("messages");
        }
    }

    private void loadChatMessages() {
        if (messagesRef == null) return;

        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    chatMessages.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        chatMessages.add(msg);
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!chatMessages.isEmpty()) {
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                    }
                }).addOnFailureListener(e -> Log.e("BusinessConsultPage", "Error loading messages", e));
    }

    private void sendMessage() {
        String prompt = promptEditText.getText().toString().trim();
        if (prompt.isEmpty() || chat == null) {
            if(chat == null) Toast.makeText(this, "AI Model is not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage userMessage = new ChatMessage(prompt, true);
        addMessageToUI(userMessage);
        promptEditText.setText("");
        loadingAnimationView.setVisibility(View.VISIBLE);

        if (currentChatId == null) {
            createNewChatSession(userMessage);
        } else {
            saveMessageToFirestore(userMessage);
        }

        Content userContent = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userContent);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponseText = result.getText();
                ChatMessage aiMessage = new ChatMessage(aiResponseText, false);
                saveMessageToFirestore(aiMessage);
                runOnUiThread(() -> {
                    addMessageToUI(aiMessage);
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("BusinessConsultPage", "Error sending message to AI", t);
                ChatMessage errorMessage = new ChatMessage("Sorry, there was an error. Please try again!", false);
                runOnUiThread(() -> {
                    addMessageToUI(errorMessage);
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void createNewChatSession(ChatMessage firstMessage) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String title = firstMessage.getMessage();
        if (title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }

        ChatSession newSession = new ChatSession(title);
        db.collection("users").document(user.getUid()).collection("chats")
                .add(newSession)
                .addOnSuccessListener(documentReference -> {
                    currentChatId = documentReference.getId();
                    setupChatCollectionRef();
                    saveMessageToFirestore(firstMessage);
                }).addOnFailureListener(e -> Log.e("BusinessConsultPage", "Error creating new chat session", e));
    }

    private void saveMessageToFirestore(ChatMessage message) {
        if (messagesRef != null) {
            messagesRef.add(message).addOnFailureListener(e -> Log.e("BusinessConsultPage", "Error saving message to Firestore", e));
        }
    }

    private void addMessageToUI(ChatMessage message) {
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    @Override
    public void onToneSelected(String tone) {
        this.currentAiTone = tone;
    }
}