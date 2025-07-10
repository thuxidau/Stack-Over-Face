package vn.edu.fpt.stackoverface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TapModeActivity extends AppCompatActivity {

    private BlockGameView gameView;
    private TextView tvScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_mode); // this layout will use an image background

        gameView = findViewById(R.id.gameView);
        tvScore = findViewById(R.id.tvScore);

        gameView.post(() -> {
            tvScore.setText(getString(R.string.score, gameView.getScore()));
            gameView.setTapEnabled(true);
        });

        gameView.setScoreUpdateCallback(() -> {
            runOnUiThread(() -> {
                tvScore.setText(getString(R.string.score, gameView.getScore()));
            });
        });

        gameView.setGameOverCallback(() -> {
            runOnUiThread(() -> {
                int score = gameView.getScore();

                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                int best = prefs.getInt("best_score", 0);
                if (score > best) {
                    prefs.edit().putInt("best_score", score).apply();
                    best = score;
                }

                tvScore.setVisibility(View.GONE);

                TextView tvFinalScore = findViewById(R.id.tvFinalScore);
                TextView tvHighScoreFinal = findViewById(R.id.tvHighScoreFinal);
                LinearLayout gameOverOverlay = findViewById(R.id.gameOverOverlay);

                tvFinalScore.setText(getString(R.string.score, score));
                tvHighScoreFinal.setText(getString(R.string.high_score, best));

                gameOverOverlay.setVisibility(View.VISIBLE);
            });
        });

        findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            finish();
            startActivity(new Intent(this, TapModeActivity.class));
        });

        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}