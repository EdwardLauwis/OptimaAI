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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BusinessConsultPage extends AppCompatActivity implements SetToneBottomSheetFragment.ToneSelectionListener, ChatHistoryAdapter.ChatHistoryListener {
    private DrawerLayout drawerLayout;
    private RecyclerView chatRecyclerView;
    private EditText promptEditText;
    private LottieAnimationView loadingAnimationView;
    private LinearLayout disclaimerLayout;

    private ChatAdapter chatAdapter;
    private ChatHistoryAdapter historyAdapter;
    private List<ChatMessage> chatMessages;
    private List<ChatSession> chatSessionList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ChatFutures chat;
    private CollectionReference messagesRef;


    private String currentChatId = null;
    private String currentAiTone = "Normal";
    private String knowledgeCache = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_consult_page);

        initializeFirebase();
        initializeViews();
        setupToolbar();
        setupChat();
        setupDrawer();

        initializeGenerativeModel();
        loadKnowledgeIntoCache();
        handleIntent();
        loadChatHistoryForDrawer();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        promptEditText = findViewById(R.id.promptEditText);
        loadingAnimationView = findViewById(R.id.loadingAnimationView);
        disclaimerLayout = findViewById(R.id.disclaimerLayout);

        MaterialButton sendPromptButton = findViewById(R.id.sendPromptButton);
        ImageButton optionsMenuButton = findViewById(R.id.optionsMenuButton);
        sendPromptButton.setOnClickListener(v -> sendMessage());
        optionsMenuButton.setOnClickListener(this::showOptionsMenu);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.consult);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupChat() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupDrawer() {
        RecyclerView drawerHistoryRecyclerView = findViewById(R.id.drawerHistoryRecyclerView);
        MaterialButton drawerNewChatButton = findViewById(R.id.drawerNewChatButton);
        drawerNewChatButton.setOnClickListener(v -> startNewChat());

        chatSessionList = new ArrayList<>();
        historyAdapter = new ChatHistoryAdapter(chatSessionList, this);
        drawerHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        drawerHistoryRecyclerView.setAdapter(historyAdapter);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    finish();
                }
            }
        });
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CHAT_ID")) {
            currentChatId = intent.getStringExtra("CHAT_ID");
            setupChatCollectionRef();
            loadChatMessages();
        } else {
            checkDisclaimerVisibility();
        }
    }

    private void loadKnowledgeIntoCache() {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(
                "knowledge" +
                ".txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            this.knowledgeCache = stringBuilder.toString();
            Log.d("BusinessConsultPage", "Knowledge success to loaded.");
        } catch (IOException e) {
            Log.e("BusinessConsultPage", "Failed to load knowledge.", e);
            Toast.makeText(this, "Failed to load knowledge.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendMessage() {
        String userPrompt = promptEditText.getText().toString().trim();
        if (userPrompt.isEmpty() || chat == null) {
            Toast.makeText(this, "Question cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalPrompt = getString(userPrompt);

        ChatMessage userMessage = new ChatMessage(userPrompt, true);
        addMessageToUI(userMessage);
        promptEditText.setText("");
        loadingAnimationView.setVisibility(View.VISIBLE);

        if (currentChatId == null) {
            createNewChatSession(userMessage);
        } else {
            saveMessageToFirestore(userMessage);
        }

        sendToGemini(finalPrompt);
    }

    @NonNull
    private String getString(String userPrompt) {
        String personaPrompt = getSecretPromptForTone(currentAiTone);

        String finalPrompt;
        if (this.knowledgeCache != null && !this.knowledgeCache.isEmpty()) {
            finalPrompt = personaPrompt +
                    "\n\nHere is an additional context you have to use:\n---\n" +
                    this.knowledgeCache +
                    "\n---\n\n" +
                    "Answer these question: " + userPrompt;
        } else {
            finalPrompt = personaPrompt + "\n\nAnswer these question: " + userPrompt;
        }
        return finalPrompt;
    }

    private void sendToGemini(String finalPrompt) {
        Content userContent = new Content.Builder().addText(finalPrompt).build();
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
                runOnUiThread(() -> {
                    Toast.makeText(BusinessConsultPage.this, "There is an error, try again",
                            Toast.LENGTH_SHORT).show();
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void addMessageToUI(ChatMessage message) {
        if (disclaimerLayout.getVisibility() == View.VISIBLE) {
            hideDisclaimerWithAnimation();
        }
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private String getSecretPromptForTone(String selectedTone) {
        switch (selectedTone) {
            case "Normal":
                return "You are a highly experienced professional business consultant but answer " +
                        "the question with normal tone";
            case "Professional":
                return "You are a highly experienced professional business consultant...";
            case "Friendly & Casual":
                return "You are a supportive friend who is also a seasoned entrepreneur...";
            case "Creative & Inspirational":
                return "You are a highly creative business motivator...";
            case "Analytics & Data":
                return "You are a highly skilled data analyst...";
            case "Decisive Business Mentor":
                return "You are an experienced business mentor...";
            case "Confidential Friend in arms":
                return "You are a trusted friend and fellow MSME (Micro, Small, and Medium Enterprise) owner...";
            default:
                return "Answer this question: ";
        }
    }

    private void checkDisclaimerVisibility() {
        if (chatMessages.isEmpty()) {
            disclaimerLayout.setVisibility(View.VISIBLE);
        } else {
            disclaimerLayout.setVisibility(View.GONE);
        }
    }

    private void hideDisclaimerWithAnimation() {
        disclaimerLayout.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> disclaimerLayout.setVisibility(View.GONE))
                .start();
    }

    private void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.chat_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.option_set_tone) {
                SetToneBottomSheetFragment bottomSheet = SetToneBottomSheetFragment.newInstance(currentAiTone);
                bottomSheet.setToneSelectionListener(BusinessConsultPage.this);
                bottomSheet.show(getSupportFragmentManager(), "SetToneBottomSheet");
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
            Toast.makeText(this, "Error: API Key not found.", Toast.LENGTH_LONG).show();
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

                    if (value != null) {
                        chatSessionList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ChatSession session = doc.toObject(ChatSession.class);
                            session.setId(doc.getId());
                            chatSessionList.add(session);
                        }
                        historyAdapter.notifyDataSetChanged();
                    }
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
        if (messagesRef == null) {
            checkDisclaimerVisibility();
            return;
        }

        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    chatMessages.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatMessage encryptedMsg = doc.toObject(ChatMessage.class);
                        String decryptedMessage = EncryptionHelper.decrypt(encryptedMsg.getMessage());
                        if (decryptedMessage != null) {
                            encryptedMsg.setMessage(decryptedMessage);
                            chatMessages.add(encryptedMsg);
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!chatMessages.isEmpty()) {
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                    }
                    checkDisclaimerVisibility();
                }).addOnFailureListener(e -> {
                    Log.e("BusinessConsultPage", "Error loading messages", e);
                    checkDisclaimerVisibility();
                });
    }

    private void createNewChatSession(ChatMessage firstMessage) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String originalTitle = firstMessage.getMessage();
        if (originalTitle.length() > 30) {
            originalTitle = originalTitle.substring(0, 30) + "...";
        }

        String encryptedTitle = EncryptionHelper.encrypt(originalTitle);
        if (encryptedTitle == null) {
            encryptedTitle = EncryptionHelper.encrypt("New Chat");
        }

        ChatSession newSession = new ChatSession(encryptedTitle);
        db.collection("users").document(user.getUid()).collection("chats")
                .add(newSession)
                .addOnSuccessListener(documentReference -> {
                    currentChatId = documentReference.getId();
                    setupChatCollectionRef();
                    saveMessageToFirestore(firstMessage);
                }).addOnFailureListener(e -> Log.e("BusinessConsultPage", "Error creating chat session", e));
    }

    private void saveMessageToFirestore(ChatMessage message) {
        if (messagesRef == null) return;

        ChatMessage messageToSave = new ChatMessage();
        messageToSave.setUser(message.isUser());
        messageToSave.setTimestamp(message.getTimestamp());

        String encryptedText = EncryptionHelper.encrypt(message.getMessage());
        if (encryptedText != null) {
            messageToSave.setMessage(encryptedText);
            messagesRef.add(messageToSave)
                    .addOnFailureListener(e -> Log.e("BusinessConsultPage", "Error saving message", e));
        }
    }

    @Override public void onToneSelected(String tone) { this.currentAiTone = tone; }
    @Override public String getCurrentChatId() { return currentChatId; }
    @Override public void onCurrentChatDeleted() { startNewChat(); }
    @Override public void onChatSessionClicked(String chatId) { }
    @Override public void onRenameChat(ChatSession session, int position) { }
    @Override public void onDeleteChat(String chatId, int position) { }
}