package com.example.optimaai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout BusinessConsultContainer, IdeaGeneratorContainer, CopyWriterGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BusinessConsultContainer = findViewById(R.id.businessConsultContainer);
        IdeaGeneratorContainer = findViewById(R.id.ideaGeneratorContainer);
        CopyWriterGenerator = findViewById(R.id.copyWriterGenerator);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BusinessConsultContainer.setOnClickListener(this);
        IdeaGeneratorContainer.setOnClickListener(this);
        CopyWriterGenerator.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == BusinessConsultContainer){
            Intent intent = new Intent(MainActivity.this, BusinessConsultPage.class);
            startActivity(intent);
        } else if (view == IdeaGeneratorContainer){
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (view == CopyWriterGenerator){
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
        }
    }
}