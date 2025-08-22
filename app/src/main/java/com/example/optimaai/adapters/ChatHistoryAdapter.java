package com.example.optimaai.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.optimaai.data.models.ChatSession;
import com.example.optimaai.R;
import com.example.optimaai.ui.activities.BusinessConsult_Page;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {

    private final List<ChatSession> chatSessions;
    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;
    private final ChatHistoryListener listener;

    public interface ChatHistoryListener {
        void onChatSessionClicked(String chatId);
        void onRenameChat(ChatSession session, int position);
        void onDeleteChat(String chatId, int position);

        String getCurrentChatId();
        void onCurrentChatDeleted();
    }

    public ChatHistoryAdapter(List<ChatSession> chatSessions, Context context) {
        this.chatSessions = chatSessions;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (context instanceof ChatHistoryListener) {
            this.listener = (ChatHistoryListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ChatHistoryListener");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = chatSessions.get(position);

        holder.title.setText(session.getTitle());

        if (session.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault());
            holder.date.setText(sdf.format(session.getCreatedAt()));
        } else {
            holder.date.setText("Recent");
        }

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, session);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BusinessConsult_Page.class);
            intent.putExtra("CHAT_ID", session.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            v.getContext().startActivity(intent);
        });
    }

    private void showPopupMenu(View v, ChatSession session) {
        PopupMenu popup = new PopupMenu(context, v);
        popup.getMenuInflater().inflate(R.menu.chat_history_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                showDeleteConfirmationDialog(session.getId());
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteConfirmationDialog(String chatId) {
        new AlertDialog.Builder(context)
                .setTitle("Hapus Chat")
                .setMessage("Apakah Anda yakin ingin menghapus chat ini secara permanen?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteChat(chatId))
                .setNegativeButton("Batal", null)
                .show();
    }

    // 4. Logika hapus yang sudah disempurnakan
    private void deleteChat(String chatId) {
        if (currentUser == null) return;

        CollectionReference messagesRef = db.collection("users").document(currentUser.getUid())
                .collection("chats").document(chatId)
                .collection("messages");

        messagesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }

                batch.commit().addOnCompleteListener(batchTask -> {
                    if (batchTask.isSuccessful()) {
                        db.collection("users").document(currentUser.getUid())
                                .collection("chats").document(chatId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Chat dihapus", Toast.LENGTH_SHORT).show();

                                    // Ini bagian penting untuk pindah halaman
                                    if (chatId.equals(listener.getCurrentChatId())) {
                                        listener.onCurrentChatDeleted();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Gagal menghapus chat.", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(context, "Gagal menghapus pesan.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(context, "Error menemukan pesan untuk dihapus.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatSessions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.chatTitleTextView);
            date = itemView.findViewById(R.id.chatDateTextView);
        }
    }
}
