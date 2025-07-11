package vn.edu.fpt.stackoverface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        SwitchPreferenceCompat musicToggle = findPreference("music_enabled");
        SwitchPreferenceCompat soundToggle = findPreference("sound_enabled");

        if (musicToggle != null) {
            musicToggle.setOnPreferenceChangeListener((pref, newValue) -> {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putBoolean("music_enabled", (boolean) newValue).apply();

                // Force the MusicService to react now
                Intent intent = new Intent(getContext(), MusicService.class);
                intent.setAction("STOP_IF_DISABLED");
                getContext().startService(intent);

                return true;
            });
        }

        if (soundToggle != null) {
            soundToggle.setOnPreferenceChangeListener((pref, newValue) -> {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putBoolean("sound_enabled", (boolean) newValue).apply();
                return true;
            });
        }
    }
}