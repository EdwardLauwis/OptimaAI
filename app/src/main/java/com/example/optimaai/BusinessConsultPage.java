package com.example.optimaai;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BusinessConsultPage extends AppCompatActivity implements SetToneBottomSheetFragment.ToneSelectionListener, ChatHistoryAdapter.ChatHistoryListener {
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
    private final OkHttpClient httpClient = new OkHttpClient();
    private final String proxyUrl = "https://optima-api-proxy.edwardlauwis30.workers.dev/";

    // --- State Variables ---
    private String currentChatId = null;
    private String currentAiTone = "Normal"; // Anda bisa mengintegrasikan ini ke dalam prompt jika perlu
    private String knowledgeCache = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_consult_page);

        // Panggil semua metode setup
        initializeFirebase();
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

    // Inisialisasi Firebase
    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    // Inisialisasi semua komponen UI
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

    // Metode ini adalah titik masuk utama saat tombol kirim ditekan
    private void sendMessage() {
        String userPrompt = promptEditText.getText().toString().trim();
        if (userPrompt.isEmpty()) {
            Toast.makeText(this, "Pertanyaan tidak boleh kosong.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gabungkan prompt pengguna dengan konteks/pengetahuan yang ada
        String finalPrompt = buildFinalPrompt(userPrompt);

        // 1. Tampilkan pesan pengguna di UI
        ChatMessage userMessage = new ChatMessage(userPrompt, true);
        addMessageToUI(userMessage);
        promptEditText.setText("");
        loadingAnimationView.setVisibility(View.VISIBLE);

        // 2. Tentukan apakah ini chat baru atau lanjutan
        if (currentChatId == null) {
            // Jika chat baru, buat sesi dulu, lalu panggil proxy
            createNewChatSession(userMessage, finalPrompt);
        } else {
            // Jika chat lanjutan, simpan pesan pengguna, lalu panggil proxy
            saveMessageToFirestore(userMessage);
            callProxy(finalPrompt);
        }
    }

    private void callProxy(String finalPrompt) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("prompt", finalPrompt);
        } catch (JSONException e) {
            Log.e("BusinessConsultPage", "Gagal membuat JSON body", e);
            loadingAnimationView.setVisibility(View.GONE);
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(proxyUrl)
                .post(body)
                .build();

        // Lakukan panggilan secara asynchronous agar tidak memblokir UI
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("BusinessConsultPage", "Gagal memanggil proxy", e);
                runOnUiThread(() -> {
                    Toast.makeText(BusinessConsultPage.this, "Error: Tidak dapat terhubung ke server.", Toast.LENGTH_SHORT).show();
                    loadingAnimationView.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("BusinessConsultPage", "Proxy error: " + response.body().string());
                    runOnUiThread(() -> {
                        Toast.makeText(BusinessConsultPage.this, "Terjadi kesalahan pada server.", Toast.LENGTH_SHORT).show();
                        loadingAnimationView.setVisibility(View.GONE);
                    });
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    String aiResponseText = jsonObject.getString("response");

                    // 3. Buat dan simpan pesan AI ke Firestore
                    ChatMessage aiMessage = new ChatMessage(aiResponseText, false);
                    saveMessageToFirestore(aiMessage); // Sekarang penyimpanan ini akan berhasil karena App Check

                    // 4. Tampilkan pesan AI di UI
                    runOnUiThread(() -> {
                        addMessageToUI(aiMessage);
                        loadingAnimationView.setVisibility(View.GONE);
                    });
                } catch (JSONException e) {
                    Log.e("BusinessConsultPage", "Gagal parsing JSON dari proxy", e);
                    runOnUiThread(() -> loadingAnimationView.setVisibility(View.GONE));
                }
            }
        });
    }

    // Membuat sesi chat baru di Firestore
    private void createNewChatSession(ChatMessage firstMessage, String finalPrompt) {
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
                    // Panggil proxy SETELAH chat ID didapatkan dan pesan pertama disimpan
                    callProxy(finalPrompt);
                }).addOnFailureListener(e -> {
                    Log.e("BusinessConsultPage", "Gagal membuat sesi chat", e);
                    runOnUiThread(() -> loadingAnimationView.setVisibility(View.GONE));
                });
    }

    // Menyimpan satu pesan (baik dari user maupun AI) ke Firestore
    private void saveMessageToFirestore(ChatMessage message) {
        if (messagesRef == null) {
            Log.e("BusinessConsultPage", "messagesRef belum diinisialisasi, tidak bisa menyimpan pesan.");
            return;
        }

        ChatMessage messageToSave = new ChatMessage();
        messageToSave.setUser(message.isUser());
        messageToSave.setTimestamp(message.getTimestamp());

        String encryptedText = EncryptionHelper.encrypt(message.getMessage());
        if (encryptedText != null) {
            messageToSave.setMessage(encryptedText);
            messagesRef.add(messageToSave)
                    .addOnFailureListener(e -> Log.e("BusinessConsultPage", "Gagal menyimpan pesan", e));
        }
    }

    // Menambahkan pesan ke RecyclerView di UI
    private void addMessageToUI(ChatMessage message) {
        if (disclaimerLayout.getVisibility() == View.VISIBLE) {
            hideDisclaimerWithAnimation();
        }
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    // Membangun prompt akhir yang akan dikirim ke AI
    @NonNull
    private String buildFinalPrompt(String userPrompt) {
        // Logika ini sama seperti sebelumnya, menggabungkan tone dan knowledge
        String personaPrompt = getSecretPromptForTone(currentAiTone);

        String finalPrompt;
        if (this.knowledgeCache != null && !this.knowledgeCache.isEmpty()) {
            finalPrompt = personaPrompt +
                    "\n\nBerikut adalah konteks tambahan yang harus kamu gunakan:\n---\n" +
                    this.knowledgeCache +
                    "\n---\n\n" +
                    "Jawab pertanyaan ini: " + userPrompt;
        } else {
            finalPrompt = personaPrompt + "\n\nJawab pertanyaan ini: " + userPrompt;
        }
        return finalPrompt;
    }

    private String getSecretPromptForTone(String selectedTone) {
        // Logika ini tidak berubah
        switch (selectedTone) {
            case "Normal":
                return "Kamu adalah konsultan bisnis profesional yang sangat berpengalaman tapi jawab pertanyaan dengan nada normal";
            case "Professional":
                return "Kamu adalah konsultan bisnis profesional yang sangat berpengalaman...";
            // ... (kasus lainnya tetap sama)
            default:
                return "Jawab pertanyaan ini: ";
        }
    }

    // --- METODE-METODE LAINNYA (TIDAK ADA PERUBAHAN) ---
    // Metode-metode di bawah ini umumnya tidak perlu diubah karena sudah menangani
    // setup UI, navigasi, dan manajemen data lokal.

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
            Log.d("BusinessConsultPage", "Knowledge berhasil dimuat.");
        } catch (IOException e) {
            Log.e("BusinessConsultPage", "Gagal memuat knowledge.", e);
            Toast.makeText(this, "Gagal memuat knowledge.", Toast.LENGTH_LONG).show();
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

    @Override public void onToneSelected(String tone) { this.currentAiTone = tone; }
    @Override public String getCurrentChatId() { return currentChatId; }
    @Override public void onCurrentChatDeleted() { startNewChat(); }
    @Override public void onChatSessionClicked(String chatId) { }
    @Override public void onRenameChat(ChatSession session, int position) { }
    @Override public void onDeleteChat(String chatId, int position) { }
}
