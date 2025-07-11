package vn.edu.fpt.stackoverface;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class StartActivity extends MusicBoundActivity {

    private String instructions = "HOW TO PLAY:\n\n" +
            "🧠 Two Modes Available:\n" +
            "• Blink Mode: Blink your eyes to drop blocks.\n" +
            "• Tap Mode: (If your face is not detected) Tap the screen to drop blocks.\n\n" +
            "🎯 Your Goal:\n" +
            "• Drop each moving block onto the stack below.\n" +
            "• The more accurately you stack, the higher your tower grows.\n\n" +
            "⚠️ Missed the Stack?\n" +
            "• If your block completely misses the one below... it's GAME OVER!\n\n" +
            "🧩 Bonus Tips:\n" +
            "• Only the overlapping part of the block remains.\n" +
            "• The rest will fall off as debris.\n" +
            "• Alternate directions every drop for extra challenge.\n\n" +
            "🔊 Use Settings:\n" +
            "• Turn sound or music on/off anytime via the settings button.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        startService(new Intent(this, MusicService.class));

        TextView tvHighScore = findViewById(R.id.tvHighScore);

        // Read from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int best = prefs.getInt("best_score", 0);

        // Update the TextView
        tvHighScore.setText(getString(R.string.high_score, best));

        Button btnBlinkPlay = findViewById(R.id.btnBlinkPlay);
        btnBlinkPlay.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class); // <- this should be your gameplay activity
            startActivity(intent);
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.btnInstructions).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Game Instructions")
                    .setMessage(instructions)
                    .setPositiveButton("Got it!", null)
                    .show();
        });
    }
}