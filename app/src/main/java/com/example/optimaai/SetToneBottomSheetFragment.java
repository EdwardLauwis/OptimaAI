package com.example.optimaai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class SetToneBottomSheetFragment extends BottomSheetDialogFragment {

    // Interface untuk mengirim data kembali ke Activity
    public interface ToneSelectionListener {
        void onToneSelected(String tone);
    }

    private ToneSelectionListener listener;
    private String currentTone;

    // Factory method untuk membuat instance dengan data
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
            currentTone = getArguments().getString("current_tone", "Profesional");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_set_tone, container, false);

        ChipGroup chipGroup = view.findViewById(R.id.toneChipGroup);
        Button applyButton = view.findViewById(R.id.applyToneButton);

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(currentTone)) {
                chip.setChecked(true);
                break;
            }
        }

        applyButton.setOnClickListener(v -> {
            int selectedChipId = chipGroup.getCheckedChipId();
            if (selectedChipId != View.NO_ID) {
                Chip selectedChip = view.findViewById(selectedChipId);
                String selectedTone = selectedChip.getText().toString();
                if (listener != null) {
                    listener.onToneSelected(selectedTone);
                }
            }
            dismiss();
        });

        return view;
    }
}
