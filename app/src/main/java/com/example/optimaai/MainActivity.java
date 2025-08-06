package com.example.optimaai;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    private LinearLayout businessConsultContainer, ideaGeneratorContainer, copyWriterGenerator;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        businessConsultContainer = findViewById(R.id.businessConsultContainer);
        ideaGeneratorContainer = findViewById(R.id.ideaGeneratorContainer);
        copyWriterGenerator = findViewById(R.id.copyWriterGenerator);

        businessConsultContainer.setOnClickListener(this);
        ideaGeneratorContainer.setOnClickListener(this);
        copyWriterGenerator.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);

        updateNavHeader();

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0); // Hanya padding kiri & kanan
            toolbar.setPadding(0, systemBars.top, 0, 0);
            return WindowInsetsCompat.CONSUMED; // Tandai bahwa kita sudah menangani insets
        });

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

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderName = headerView.findViewById(R.id.nav_header_name);
        TextView navHeaderEmail = headerView.findViewById(R.id.nav_header_email);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                navHeaderName.setText(currentUser.getDisplayName());
            } else {
                navHeaderName.setText("User");
            }
            navHeaderEmail.setText(currentUser.getEmail());
        }
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.businessConsultContainer) {
            startActivity(new Intent(MainActivity.this, BusinessConsultPage.class));
        } else if (viewId == R.id.ideaGeneratorContainer || viewId == R.id.copyWriterGenerator) {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_open_drawer) {
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_profile) {
            Toast.makeText(this, "Menu Profil diklik", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_settings) {
            Toast.makeText(this, "Menu Pengaturan diklik", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_logout) {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, Login_Page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }
}
