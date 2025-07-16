package vn.edu.fpt.stackoverface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsOverlayFragment extends Fragment {

    public SettingsOverlayFragment() {
        super(R.layout.fragment_settings_overlay);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MaterialSwitch switchMusic = view.findViewById(R.id.switchMusic);
        MaterialSwitch switchSound = view.findViewById(R.id.switchSound);
        MaterialSwitch switchVibration = view.findViewById(R.id.switchVibration);
        Button btnClose = view.findViewById(R.id.btnCloseSettings);

        // Sync initial states with preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        switchMusic.setChecked(prefs.getBoolean("music_enabled", true));
        switchSound.setChecked(prefs.getBoolean("sound_enabled", true));
        switchVibration.setChecked(prefs.getBoolean("vibration_enabled", true));

        // Music switch listener
        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("music_enabled", isChecked).apply();

            Intent intent = new Intent(requireContext(), MusicService.class);
            intent.setAction(isChecked ? "RESUME" : "STOP_IF_DISABLED");
            requireContext().startService(intent);
        });

        // Sound switch listener
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        );

        // Vibration switch listener
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply();
        });

        // Close button handler
        btnClose.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit()
        );

        // Dismiss if user taps outside settings panel
        FrameLayout root = view.findViewById(R.id.settingsOverlay);
        root.setOnClickListener(v -> {
            if (v == root) {
                requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            }
        });
    }
}