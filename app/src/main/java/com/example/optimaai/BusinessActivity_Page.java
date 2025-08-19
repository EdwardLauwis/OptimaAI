package com.example.optimaai;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class BusinessActivity_Page extends AppCompatActivity {

    private static final String TAG = "BusinessActivity";

    private TextInputEditText businessName, standDate, address;
    private AutoCompleteTextView BusinessCategory;
    private Button saveButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        businessName = findViewById(R.id.etNamaBisnis);
        BusinessCategory = findViewById(R.id.actvKategoriBisnis);
        standDate = findViewById(R.id.etTanggalBerdiri);
        address = findViewById(R.id.etAlamat);
        saveButton = findViewById(R.id.btnSimpan);

        setupToolbar();
        setupCategoryDropdown();
        setupDatePicker();

        saveButton.setOnClickListener(v -> saveBusinessInfo());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupCategoryDropdown() {
        String[] categories = new String[]{"Kuliner", "Fashion", "Jasa", "Teknologi", "Pendidikan", "Kesehatan", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        BusinessCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, day);
            updateLabel();
        };

        standDate.setOnClickListener(v -> new DatePickerDialog(BusinessActivity_Page.this, date,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateLabel() {
        String myFormat = "dd MMMM yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, new Locale("id", "ID"));
        standDate.setText(sdf.format(myCalendar.getTime()));
    }

    private void saveBusinessInfo() {
        Log.d(TAG, "The Save button is clicked, initiating the saveBusinessInfo process.");

        String businessNameStr = businessName.getText() != null ? businessName.getText().toString().trim() : "";
        String businessCategoryStr = BusinessCategory.getText() != null ? BusinessCategory.getText().toString().trim() : "";
        String businessStartDateStr = standDate.getText() != null ? standDate.getText().toString().trim() : "";
        String businessAddressStr = address.getText() != null ? address.getText().toString().trim() : "";

        if (TextUtils.isEmpty(businessNameStr) || TextUtils.isEmpty(businessCategoryStr) ||
                TextUtils.isEmpty(businessStartDateStr) || TextUtils.isEmpty(businessAddressStr)) {
            Log.d(TAG, "Validation FAILED: There is an empty field.");
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Validation Passed: All fields are filled.");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User is not authenticated!");
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Current user UID: " + userId); // Pastikan UID ini sesuai dengan yang di Console

        // Buat map dengan fields bisnis langsung (tanpa wrapper)
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("businessName", businessNameStr);
        userUpdate.put("businessCategory", businessCategoryStr);
        userUpdate.put("businessStartDate", businessStartDateStr);
        userUpdate.put("businessLocation", businessAddressStr);

        Log.d(TAG, "Data to save: " + userUpdate.toString()); // Log data sebelum disimpan
        db.collection("users").document(userId)
                .set(userUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Data successfully saved to users/" + userId);
                    Toast.makeText(BusinessActivity_Page.this, "Business information saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILED to save data to Firestore. Error: " + e.getMessage());
                    Toast.makeText(BusinessActivity_Page.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
