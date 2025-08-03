package com.example.optimaai;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private final List<ChatSession> chatSessions;

    ChatHistoryAdapter(List<ChatSession> chatSessions) {
        this.chatSessions = chatSessions;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BusinessConsultPage.class);
            intent.putExtra("CHAT_ID", session.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            v.getContext().startActivity(intent);
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
