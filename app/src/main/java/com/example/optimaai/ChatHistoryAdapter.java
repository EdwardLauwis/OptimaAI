package com.example.optimaai;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private final List<ChatSession> chatSessions;
    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;

    public ChatHistoryAdapter(List<ChatSession> chatSessions, Context context) {
        this.chatSessions = chatSessions;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
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
        String encryptedTitle = session.getTitle();
        Log.d("ChatHistoryAdapter", "Encrypted Title: " + encryptedTitle);

        String decryptedTitle = EncryptionHelper.decrypt(encryptedTitle);
        Log.d("ChatHistoryAdapter", "Decrypted Title: " + decryptedTitle);

        if (decryptedTitle != null && !decryptedTitle.isEmpty()) {
            holder.title.setText(decryptedTitle);
        } else {
            Log.w("ChatHistoryAdapter", "Decryption failed, using fallback title");
            holder.title.setText("Encrypted Title");
        }

        if (session.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault());
            holder.date.setText(sdf.format(session.getCreatedAt()));
        } else {
            holder.date.setText("Recent");
        }

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, session, position);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BusinessConsultPage.class);
            intent.putExtra("CHAT_ID", session.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            v.getContext().startActivity(intent);
        });
    }

    private void showPopupMenu(View v, ChatSession session, int position) {
        PopupMenu popup = new PopupMenu(context, v);
        popup.getMenuInflater().inflate(R.menu.chat_history_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_rename) {
                    showRenameDialog(session, position);
                    return true;
                } else if (item.getItemId() == R.id.action_delete) {
                    showDeleteConfirmationDialog(session.getId(), position);
                    return true;
                }
                return false;
            }
        });

        popup.show();
    }

    private void showRenameDialog(ChatSession session, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rename Chat");

        final EditText input = new EditText(context);
        input.setText(EncryptionHelper.decrypt(session.getTitle()));
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newTitle = input.getText().toString().trim();
                if (!newTitle.isEmpty()) {
                    String encryptedTitle = EncryptionHelper.encrypt(newTitle);
                    if (encryptedTitle != null) {
                        updateChatTitle(session.getId(), encryptedTitle);
                        chatSessions.get(position).setTitle(encryptedTitle);
                        notifyItemChanged(position);
                    } else {
                        Toast.makeText(context, "Failed to encrypt new title", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(String chatId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Chat");
        builder.setMessage("Are you sure you want to delete this chat?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteChat(chatId, position);
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void updateChatTitle(String chatId, String newEncryptedTitle) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .collection("chats").document(chatId)
                    .update("title", newEncryptedTitle)
                    .addOnSuccessListener(aVoid -> Log.d("ChatHistoryAdapter", "Title updated successfully"))
                    .addOnFailureListener(e -> Log.e("ChatHistoryAdapter", "Error updating title", e));
        }
    }

    // ChatHistoryAdapter.java

    private void deleteChat(String chatId, int position) {
        if (currentUser == null) return;

        CollectionReference messagesRef = db.collection("users").document(currentUser.getUid())
                .collection("chats").document(chatId)
                .collection("messages");

        messagesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    messagesRef.document(document.getId()).delete();
                }

                db.collection("users").document(currentUser.getUid())
                        .collection("chats").document(chatId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ChatHistoryAdapter", "Chat and its messages deleted successfully.");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ChatHistoryAdapter", "Error deleting chat document", e);
                            Toast.makeText(context, "Failed to delete chat.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Log.e("ChatHistoryAdapter", "Error getting messages for deletion.", task.getException());
                Toast.makeText(context, "Failed to delete chat messages.", Toast.LENGTH_SHORT).show();
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