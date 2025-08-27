package com.example.optimaai.ui.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.optimaai.R;
import com.example.optimaai.data.models.IdeaOrbitLayout;
import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class IdeaGenerator_Page extends AppCompatActivity {

    private IdeaOrbitLayout ideaOrbitContainer;
    private MaterialButton btnAddIdea;
    private LinearLayout inputContainer;
    private EditText ideaInput;
    private MaterialButton btnSubmitIdea;
    private MaterialButton btnCancelIdea;
    private Random random = new Random();
    private static final int MAX_IDEAS = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idea_generator_page);

        // Initialize components
        ideaOrbitContainer = findViewById(R.id.idea_orbit_container);
        btnAddIdea = findViewById(R.id.btnAddIdea);
        inputContainer = findViewById(R.id.input_container);
        ideaInput = findViewById(R.id.idea_input);
        btnSubmitIdea = findViewById(R.id.btn_submit_idea);
        btnCancelIdea = findViewById(R.id.btn_cancel_idea);

        // Set brain icon reference
        ideaOrbitContainer.setBrainIcon(findViewById(R.id.brain_icon));

        // Hide input container by default
        inputContainer.setVisibility(View.GONE);

        // Set up listeners
        btnAddIdea.setOnClickListener(v -> showIdeaInputDialog());
        btnSubmitIdea.setOnClickListener(v -> addNewIdea());
        btnCancelIdea.setOnClickListener(v -> hideIdeaInputDialog());

        ideaInput.setOnEditorActionListener((v, actionId, event) -> {
            addNewIdea();
            return true;
        });
    }

    private void showIdeaInputDialog() {
        inputContainer.setVisibility(View.VISIBLE);
        btnAddIdea.setVisibility(View.GONE);
        ideaInput.requestFocus();
    }

    private void hideIdeaInputDialog() {
        inputContainer.setVisibility(View.GONE);
        btnAddIdea.setVisibility(View.VISIBLE);
        ideaInput.setText("");
    }

    private void addNewIdea() {
        String ideaText = ideaInput.getText().toString().trim();
        if (!ideaText.isEmpty()) {
            addIdeaAsNode(ideaText);
            hideIdeaInputDialog();
        } else {
            Toast.makeText(this, "Idea cannot be empty.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addIdeaAsNode(String ideaText) {
        if (ideaOrbitContainer.getChildCount() >= MAX_IDEAS) {
            ideaOrbitContainer.removeViewAt(0);
        }

        TextView ideaNode = new TextView(this);
        ideaNode.setText(ideaText);
        ideaNode.setBackgroundResource(R.drawable.rounded_background_stroke);
        ideaNode.setTextColor(Color.WHITE);
        ideaNode.setGravity(Gravity.CENTER);
        ideaNode.setPadding(32, 16, 32, 16);
        ideaNode.setElevation(8f);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        ideaOrbitContainer.addView(ideaNode, params);

        ideaNode.post(() -> {
            int centerX = ideaOrbitContainer.getWidth() / 2;
            int centerY = ideaOrbitContainer.getHeight() / 2;

            // Initial position at center
            ideaNode.setX(centerX - ideaNode.getWidth() / 2);
            ideaNode.setY(centerY - ideaNode.getHeight() / 2);

            // Random target position
            double angle = random.nextDouble() * 2 * Math.PI;
            int minRadius = (int) (ideaOrbitContainer.getWidth() * 0.25);
            int maxRadius = (int) (ideaOrbitContainer.getWidth() * 0.45);
            int radius = random.nextInt(maxRadius - minRadius) + minRadius;

            float targetX = (float) (centerX + radius * Math.cos(angle));
            float targetY = (float) (centerY + radius * Math.sin(angle));

            // Animate to new position
            ObjectAnimator xAnimator = ObjectAnimator.ofFloat(ideaNode, "x", ideaNode.getX(), targetX - ideaNode.getWidth() / 2);
            ObjectAnimator yAnimator = ObjectAnimator.ofFloat(ideaNode, "y", ideaNode.getY(), targetY - ideaNode.getHeight() / 2);

            xAnimator.setDuration(1000);
            yAnimator.setDuration(1000);
            xAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            yAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(xAnimator, yAnimator);
            animatorSet.start();

            // Add bubble to custom layout for drawing lines
            ideaOrbitContainer.addIdeaBubble(ideaNode);
        });
    }
}