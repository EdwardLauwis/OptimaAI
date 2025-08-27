package com.example.optimaai.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.optimaai.R;

public class IdeaResult_Page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idea_result_page);

        String fullResponse = getIntent().getStringExtra("AI_RESPONSE");

        TextView resultConceptTextView = findViewById(R.id.resultConceptTextView);
        Button btnSaveIdea = findViewById(R.id.btnSaveIdea);
        Button btnCopyIdea = findViewById(R.id.btnCopyIdea);

        resultConceptTextView.setText(fullResponse);

        btnCopyIdea.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Idea", fullResponse);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Idea copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        btnSaveIdea.setOnClickListener(v -> {
            // TODO: Add logic to save the idea to Firestore or local database
            Toast.makeText(this, "Save feature is coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
}