package com.example.optimaai.data.models;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

public class IdeaOrbitLayout extends RelativeLayout {
    private Paint linePaint = new Paint();
    private List<View> ideaBubbles = new ArrayList<>();
    private View brainIcon;

    public IdeaOrbitLayout(Context context) {
        super(context);
        init();
    }

    public IdeaOrbitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IdeaOrbitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        setWillNotDraw(false); // Allow drawing
    }

    public void setBrainIcon(View brainIcon) {
        this.brainIcon = brainIcon;
    }

    public void addIdeaBubble(View bubble) {
        ideaBubbles.add(bubble);
        invalidate(); // Trigger redraw
    }

    public void removeIdeaBubble(View bubble) {
        ideaBubbles.remove(bubble);
        invalidate(); // Trigger redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (brainIcon != null && !ideaBubbles.isEmpty()) {
            int brainCenterX = brainIcon.getLeft() + brainIcon.getWidth() / 2;
            int brainCenterY = brainIcon.getTop() + brainIcon.getHeight() / 2;

            for (View bubble : ideaBubbles) {
                float bubbleX = bubble.getX() + bubble.getWidth() / 2;
                float bubbleY = bubble.getY() + bubble.getHeight() / 2;
                canvas.drawLine(brainCenterX, brainCenterY, bubbleX, bubbleY, linePaint);
            }
        }
    }
}