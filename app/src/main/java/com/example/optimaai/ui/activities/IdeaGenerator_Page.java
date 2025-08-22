package com.example.optimaai.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.optimaai.R;
import com.google.android.material.button.MaterialButton;

public class IdeaGenerator_Page extends AppCompatActivity {

    // 1. VARIABLE DECLARATIONS
    // Variables to store user selections
    private String selectedInspiration = null;
    private String selectedAudience = null;
    private String selectedTwist = null;

    // Variables for UI components
    private TextView tvSelectedInspiration, tvSelectedAudience, tvSelectedTwist;
    private MaterialButton btnBrewIdea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idea_generator_page);

        // 2. INITIALIZATION & LISTENER SETUP
        // Connecting Java variables with components in the XML
        tvSelectedInspiration = findViewById(R.id.tv_selected_inspiration);
        tvSelectedAudience = findViewById(R.id.tv_selected_audience);
        tvSelectedTwist = findViewById(R.id.tv_selected_twist);
        btnBrewIdea = findViewById(R.id.btn_racik_ide); // Make sure ID in XML is btn_racik_ide

        ImageButton btnSelectInspiration = findViewById(R.id.btn_select_inspiration);
        ImageButton btnSelectAudience = findViewById(R.id.btn_select_audience);
        ImageButton btnSelectTwist = findViewById(R.id.btn_select_twist);

        // Setting click listeners for each "ingredient" button
        btnSelectInspiration.setOnClickListener(v -> showPickerDialog("Select Inspiration", R.array.idea_inspirations));
        btnSelectAudience.setOnClickListener(v -> showPickerDialog("Select Audience", R.array.idea_audiences));
        btnSelectTwist.setOnClickListener(v -> showPickerDialog("Select Secret Spice", R.array.idea_twists));

        // Listener for the main "Brew Idea" button
        btnBrewIdea.setOnClickListener(v -> generateIdea());
    }

    // 3. LOGIC TO DISPLAY THE SELECTION DIALOG
    private void showPickerDialog(String title, int arrayResourceId) {
        final String[] items = getResources().getStringArray(arrayResourceId);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    String selectedItem = items[which];

                    // Update the variable and UI text based on the selection
                    if (arrayResourceId == R.array.idea_inspirations) {
                        selectedInspiration = selectedItem;
                        tvSelectedInspiration.setText("Inspiration: " + selectedInspiration);
                    } else if (arrayResourceId == R.array.idea_audiences) {
                        selectedAudience = selectedItem;
                        tvSelectedAudience.setText("Audience: " + selectedAudience);
                    } else if (arrayResourceId == R.array.idea_twists) {
                        selectedTwist = selectedItem;
                        tvSelectedTwist.setText("Secret Spice: " + selectedTwist);
                    }
                    checkIfReadyToBrew();
                })
                .show();
    }

    // 4. CHECK IF ALL INGREDIENTS HAVE BEEN SELECTED
    private void checkIfReadyToBrew() {
        if (selectedInspiration != null && selectedAudience != null && selectedTwist != null) {
            btnBrewIdea.setVisibility(View.VISIBLE);
        }
    }

    // 5. LOGIC TO CREATE THE PROMPT AND SEND TO THE AI
    private void generateIdea() {
        // Assembling the creative prompt for the AI
        String finalPrompt = "You are a visionary business innovator. Create a unique business concept " +
                "by combining the following three 'ingredients': a trend/problem which is '" + selectedInspiration +
                "', a target audience which is '" + selectedAudience +
                "', and a 'secret spice' which is '" + selectedTwist +
                "'. Provide a catchy name for the idea, explain the concept in one paragraph, " +
                "and give a single, concrete first step to start.";

        // For now, we'll display the prompt in a Toast
        // Later, this is where you will call your callProxy() method like in BusinessConsult_Page
        Toast.makeText(this, "Prompt Ready to Send:\n" + finalPrompt, Toast.LENGTH_LONG).show();

        // TODO: Call your Gemini API here with the finalPrompt
    }
}