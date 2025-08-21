package com.example.optimaai;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
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
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BusinessConsult_Page extends AppCompatActivity implements SetToneBottomSheetFragment.ToneSelectionListener, ChatHistoryAdapter.ChatHistoryListener {
    // --- UI Elements ---
    private DrawerLayout drawerLayout;
    private RecyclerView chatRecyclerView;
    private EditText promptEditText;
    private LottieAnimationView loadingAnimationView;
    private LinearLayout disclaimerLayout;

    // --- Adapters and Data Lists ---
    private ChatAdapter chatAdapter;
    private ChatHistoryAdapter historyAdapter;
    private List<ChatMessage> chatMessages;
    private List<ChatSession> chatSessionList;

    // --- Firebase & Networking ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference messagesRef;
    private final String proxyUrl = "https://optima-api-proxy.edwardlauwis30.workers.dev/";

    // --- State Variables ---
    private String currentChatId = null;
    private String currentAiTone = "Normal";
    private String knowledgeCache = null;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_consult_page);

        initializeFirebase();
        ensureUserDocumentExists();
        initializeViews();
        setupToolbar();
        setupChat();
        setupDrawer();
        loadKnowledgeIntoCache();
        handleIntent();
        loadChatHistoryForDrawer();

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();

            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                chatRecyclerView.setPadding(0, 0, 0, keypadHeight);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            } else {
                chatRecyclerView.setPadding(0, 0, 0, 0);
            }
        });
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

    private void sendMessage() {
        String userPrompt = promptEditText.getText().toString().trim();
        if (userPrompt.isEmpty()) {
            Toast.makeText(this, "Question cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage userMsg = new ChatMessage(userPrompt, true);
        addMessageToUI(userMsg);
        promptEditText.setText("");

        loadingAnimationView.setVisibility(View.VISIBLE);
        disclaimerLayout.setVisibility(View.GONE);

        callProxy(userPrompt);
    }

    private void callProxy(String userPrompt) {
        Log.d("GeminiDebug", "callProxy is called with a prompt:" + userPrompt);

        String finalPrompt = buildFinalPrompt(userPrompt);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("prompt", finalPrompt);
        } catch (JSONException e) {
            Log.e("GeminiDebug", "Failed to create JSON body", e);
            runOnUiThread(() -> loadingAnimationView.setVisibility(View.GONE));
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(proxyUrl)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GeminiDebug", "Entry to  onFailure()", e);
                runOnUiThread(() -> {
                    Toast.makeText(BusinessConsult_Page.this, "Failed to connect to the server:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";
                Log.d("GeminiDebug", "Raw response: " + responseData);

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(BusinessConsult_Page.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                        loadingAnimationView.setVisibility(View.GONE);
                    });
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    String aiResponseText = "";

                    if (jsonObject.has("response")) {
                        aiResponseText = jsonObject.getString("response");
                    } else if (jsonObject.has("candidates")) {
                        JSONArray candidates = jsonObject.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                aiResponseText = parts.getJSONObject(0).getString("text");
                            }
                        }
                    }

                    if (aiResponseText.isEmpty()) {
                        throw new JSONException("AI response text is empty after parsing.");
                    }

                    final String finalAiResponseText = aiResponseText;
                    runOnUiThread(() -> {
                        handleSuccessfulResponse(userPrompt, finalAiResponseText);
                    });

                } catch (JSONException e) {
                    Log.e("GeminiDebug", "Gagal parsing JSON", e);
                    runOnUiThread(() -> {
                        Toast.makeText(BusinessConsult_Page.this, "Error parsing AI response.", Toast.LENGTH_SHORT).show();
                        loadingAnimationView.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void handleSuccessfulResponse(String userPrompt, String aiResponse) {
        ChatMessage userMessage = new ChatMessage(userPrompt, true);
        ChatMessage aiMessage = new ChatMessage(aiResponse, false);

        if (currentChatId == null) {

            createNewChatSessionAndSaveMessages(userMessage, aiMessage);
        } else {

            saveMessageToFirestore(userMessage);
            saveMessageToFirestore(aiMessage);
        }

        addMessageToUI(aiMessage);
        loadingAnimationView.setVisibility(View.GONE);
    }

    private void createNewChatSessionAndSaveMessages(ChatMessage userMessage, ChatMessage aiMessage) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("BusinessConsultPage", "User tidak login, tidak bisa membuat sesi chat.");
            loadingAnimationView.setVisibility(View.GONE); // Pastikan loading berhenti
            return;
        }

        DocumentReference chatDocRef = db.collection("users")
                .document(user.getUid())
                .collection("chats")
                .document();

        this.currentChatId = chatDocRef.getId();

        String title = userMessage.getMessage().length() > 40 ?
                userMessage.getMessage().substring(0, 40) + "…" :
                userMessage.getMessage();

        String encryptedTitle = EncryptionHelper.encrypt(title);

        if (encryptedTitle == null) {
            Log.e("EncryptionError", "FATAL: Failed to encrypt the title when saving. Using the original title as an emergency fallback.");
        }



        Map<String, Object> chatMetaData = new HashMap<>();
        chatMetaData.put("title", encryptedTitle);
        chatMetaData.put("createdAt", FieldValue.serverTimestamp());

        chatDocRef.set(chatMetaData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("BusinessConsultPage", "The new chat session was successfully created:" + currentChatId);
                    saveMessageToFirestore(userMessage);
                    saveMessageToFirestore(aiMessage);
                    loadChatHistoryForDrawer();
                })
                .addOnFailureListener(e -> {
                    Log.e("BusinessConsultPage", "Failed to create a chat session", e);
                    // Rollback state
                    this.currentChatId = null;
                    loadingAnimationView.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to start a new chat.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMessageToFirestore(ChatMessage message) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("BusinessConsultPage", "User has not login yet; failed to save conversation.");
            return;
        }
        if (currentChatId == null) {
            Log.e("BusinessConsultPage", "currentChatId null; calling ensureChatSession() old.");
            return;
        }

        String encryptedText = EncryptionHelper.encrypt(message.getMessage());
        if (encryptedText == null) {
            Log.e("BusinessConsultPage", "Encryption failed; save failed.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", encryptedText);
        payload.put("isUser", message.isUser());
        payload.put("timestamp", FieldValue.serverTimestamp());

        CollectionReference msgsRef = db.collection("users")
                .document(user.getUid())
                .collection("chats")
                .document(currentChatId)
                .collection("messages");

        Log.d("BusinessConsultPage",
                "Save to : users/" + user.getUid() + "/chats/" + currentChatId + "/messages");

        // checking ID and Path in Firestore
        Log.d("BusinessConsultPage", "UID login: " + user.getUid());
        Log.d("BusinessConsultPage", "Path Firestore: " + msgsRef.getPath());


        msgsRef.add(payload)
                .addOnSuccessListener(docRef -> Log.d("BusinessConsultPage",
                        "Conversation Saved: " + docRef.getId()))
                .addOnFailureListener(e -> Log.e("BusinessConsultPage", "Failed to save " +
                                "conversation!",
                        e));
    }

    private void ensureUserDocumentExists(){
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        DocumentReference userDoc = db.collection("users").document(user.getUid());

        userDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("email", user.getEmail());
                userData.put("createdAt", FieldValue.serverTimestamp());

                userDoc.set(userData)
                        .addOnSuccessListener(aVoid ->
                                Log.d("BusinessConsultPage",
                                        "User doc has created with UID: " + user.getUid()))
                        .addOnFailureListener(e ->
                                Log.e("BusinessConsultPage", "Failed to create user doc", e));
            } else {
                Log.d("BusinessConsultPage", "User doc already exist with UID: " + user.getUid());
            }
        });

    }

    private void addMessageToUI(ChatMessage message) {
        if (disclaimerLayout.getVisibility() == View.VISIBLE) {
            hideDisclaimerWithAnimation();
        }
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    @NonNull
    private String buildFinalPrompt(String userPrompt) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        String personaPrompt = getSecretPromptForTone(currentAiTone);
        String defaultLanguage = "English or Indonesia";
        String username = (firebaseUser != null && firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) ? firebaseUser.getDisplayName() : "User";


        String finalPrompt;
        if (this.knowledgeCache != null && !this.knowledgeCache.isEmpty()) {
            finalPrompt = "Hai, you are currently talking to user with username: "+ username + ", " + personaPrompt + " and the default language you should use is either " + defaultLanguage + "and" +
                    "\n\nHere are the additional contexts you should use:\n---\n" +
                    this.knowledgeCache +
                    "\n---\n\n" + " And also avoid to use '**text**' for bold, because the result output literally '**text**', just use a clear structure." +
                    "Answer this question:" + userPrompt;
        } else {
            finalPrompt = personaPrompt + "\n\nAnswer this question:" + userPrompt;
        }
        return finalPrompt;
    }

    private String getSecretPromptForTone(String selectedTone) {
        switch (selectedTone) {
            case "Normal":
                return "You are a very experienced professional business consultant but answer questions in a normal tone";

            case "Professional":
                return "You are a highly experienced professional business consultant. Use formal, structured, and to-the-point language.";

            case "Friendly & Casual":
                return "You are a supportive friend and an experienced entrepreneur. Give answers in a friendly, relaxed, and easy-to-understand style, as if you were chatting in a coffee shop.";

            case "Creative & Inspirational":
                return "You are a very creative and visionary business motivator. Give answers that are inspiring, use analogies, and encourage thinking outside the box.";

            case "Analytics & Data":
                return "You are a highly skilled data analyst and business strategist. Focus your answers on data, metrics, and objective analysis. Use bullet points and, where possible, make suggestions based on data-driven logic.";

            case "Decisive Business Mentor":
                return "You are an experienced, decisive and no-nonsense business mentor. Give advice that is straightforward, actionable, and results-focused. Give concrete steps to take.";

            case "Confidential Friend in arms":
                return "You are a very trustworthy friend in arms of fellow MSME (Micro, Small and Medium Enterprises) owners. Use an empathetic tone, provide support, and share insights from the perspective of someone who truly understands the day-to-day challenges of running a small business.";

            default:
                if (!selectedTone.isEmpty()) {
                    return selectedTone;
                }
                return "Answer this question clearly and helpfully.";
        }
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("knowledge.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            this.knowledgeCache = stringBuilder.toString();
            Log.d("BusinessConsultPage", "Knowledge successfully loaded.");
        } catch (IOException e) {
            Log.e("BusinessConsultPage", "Fails to contain knowledge.", e);
            Toast.makeText(this, "Fails to contain knowledge.", Toast.LENGTH_LONG).show();
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
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.chat_options_menu, popup.getMenu());
        popup.setGravity(Gravity.TOP);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.option_set_tone) {
                SetToneBottomSheetFragment bottomSheet = SetToneBottomSheetFragment.newInstance(currentAiTone);
                bottomSheet.setToneSelectionListener(BusinessConsult_Page.this);
                bottomSheet.show(getSupportFragmentManager(), "SetToneBottomSheet");
                return true;
            }
            return false;
        });
        popup.show();
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
        Intent intent = new Intent(this, BusinessConsult_Page.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadChatHistoryForDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Log.d("DEBUG_UID", currentUser.getUid());
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

                            String titleFromDb = session.getTitle();
                            if (titleFromDb != null) {
                                String decryptedTitle = EncryptionHelper.decrypt(titleFromDb);
                                session.setTitle(decryptedTitle != null ? decryptedTitle : "[Decrypt Failed!]");
                            } else {
                                session.setTitle("Title Empty");
                            }
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
                        String encryptedMessage = doc.getString("message");
                        Boolean isUser = doc.getBoolean("isUser");
                        java.util.Date timestamp = doc.getDate("timestamp");

                        String decryptedMessage = EncryptionHelper.decrypt(encryptedMessage);

                        if (decryptedMessage != null && isUser != null) {
                            ChatMessage message = new ChatMessage();
                            message.setMessage(decryptedMessage);
                            message.setUser(isUser);
                            message.setTimestamp(timestamp);

                            chatMessages.add(message);
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

    @Override public void onToneSelected(String tone) { this.currentAiTone = tone; }
    @Override public String getCurrentChatId() { return currentChatId; }
    @Override public void onCurrentChatDeleted() { startNewChat(); }
    @Override public void onChatSessionClicked(String chatId) { }
    @Override public void onRenameChat(ChatSession session, int position) { }
    @Override public void onDeleteChat(String chatId, int position) { }
}
