package vn.edu.fpt.stackoverface;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class StartActivity extends MusicBoundActivity {

    private final String instructions = "• Blink your eyes to drop blocks.\n" +
            "• (If your face is not detected) Tap the screen to drop blocks.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Start the music service (in the background) when the app is launched
        startService(new Intent(this, MusicService.class));

        TextView tvHighScore = findViewById(R.id.tvHighScore);

        // Display high score
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int best = prefs.getInt("best_score", 0);

        // Update the TextView
        tvHighScore.setText(getString(R.string.high_score, best));

        // Button start game handler
        findViewById(R.id.btnBlinkPlay).setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class); // <- this should be your gameplay activity
            startActivity(intent);
        });

        // Button settings handler
        findViewById(R.id.btnSettings).setOnClickListener(v -> getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new SettingsOverlayFragment())
                .commit());

        // Button show instruction handler
        findViewById(R.id.btnInstructions).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Game Instructions")
                .setMessage(instructions)
                .setPositiveButton("Got it!", null)
                .show());
    }
}