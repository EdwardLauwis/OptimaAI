package com.example.optimaai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class    SetToneBottomSheetFragment extends BottomSheetDialogFragment {

    public interface ToneSelectionListener {
        void onToneSelected(String tone);
    }

    private ToneSelectionListener listener;
    private String currentTone;

    public static SetToneBottomSheetFragment newInstance(String currentTone) {
        SetToneBottomSheetFragment fragment = new SetToneBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("current_tone", currentTone);
        fragment.setArguments(args);
        return fragment;
    }

    public void setToneSelectionListener(ToneSelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTone = getArguments().getString("current_tone", "Professional");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_set_tone, container, false);

        ChipGroup chipGroup = view.findViewById(R.id.toneChipGroup);
        Button applyButton = view.findViewById(R.id.applyToneButton);

        TextInputEditText customToneEditText = view.findViewById(R.id.customToneEditText);

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(currentTone)) {
                chip.setChecked(true);
                break;
            }
        }

        customToneEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    chipGroup.clearCheck();
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        applyButton.setOnClickListener(v -> {
            String selectedTone;
            String customTone = Objects.requireNonNull(customToneEditText.getText()).toString().trim();
            if (!TextUtils.isEmpty(customTone)) {
                selectedTone = customTone;
            } else {
                int selectedChipId = chipGroup.getCheckedChipId();
                if (selectedChipId != View.NO_ID) {
                    Chip selectedChip = view.findViewById(selectedChipId);
                    selectedTone = selectedChip.getText().toString();
                } else {
                    selectedTone = currentTone;
                }
            }

            if (listener != null) {
                listener.onToneSelected(selectedTone);
            }

            dismiss();
        });

        return view;
    }
}
