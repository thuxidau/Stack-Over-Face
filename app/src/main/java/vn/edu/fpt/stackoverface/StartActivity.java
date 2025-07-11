package vn.edu.fpt.stackoverface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class StartActivity extends MusicBoundActivity {

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
    }
}