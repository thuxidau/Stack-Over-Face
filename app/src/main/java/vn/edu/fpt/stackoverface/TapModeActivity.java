package vn.edu.fpt.stackoverface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

public class TapModeActivity extends MusicBoundActivity {

    private BlockGameView gameView;
    private TextView tvScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_mode); // this layout will use an image background

        gameView = findViewById(R.id.gameView);
        tvScore = findViewById(R.id.tvScore);

        ImageButton btnPause = findViewById(R.id.btnPause);
        TextView tvCountdown = findViewById(R.id.tvCountdown);
        final boolean[] isPaused = {false};

        // Enable tap mode and set initial score
        gameView.post(() -> {
            tvScore.setText(getString(R.string.score, gameView.getScore()));
            gameView.setTapEnabled(true); // Enable tap-based control so users can tap to drop blocks
            gameView.setContext(this); // Pass context for sound setup
        });

        // Update the on-screen score when it changes during gameplay
        gameView.setScoreUpdateCallback(() -> runOnUiThread(() ->
                tvScore.setText(getString(R.string.score, gameView.getScore()))));

        // Pause listener
        btnPause.setOnClickListener(v -> {
            if (!isPaused[0]) {
                // Pause
                isPaused[0] = true;
                gameView.pauseGame();
                btnPause.setImageResource(R.drawable.ic_play);
            } else {
                // Resume with countdown
                btnPause.setVisibility(View.GONE);
                tvCountdown.setVisibility(View.VISIBLE);
                new CountDownTimer(3000, 1000) {
                    int count = 3;

                    // Countdown
                    @Override
                    public void onTick(long millisUntilFinished) {
                        tvCountdown.setText(String.valueOf(count--));
                    }

                    // After finishing countdown
                    @Override
                    public void onFinish() {
                        isPaused[0] = false;
                        gameView.resumeGame();
                        btnPause.setImageResource(R.drawable.ic_pause);
                        btnPause.setVisibility(View.VISIBLE);
                        tvCountdown.setVisibility(View.GONE);
                    }
                }.start();
            }
        });

        // Game over callback
        gameView.setGameOverCallback(() -> runOnUiThread(() -> {
            // game over sound
            SharedPreferences prefs_sound = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs_sound.getBoolean("sound_enabled", true)) {
                MediaPlayer gameOverPlayer = MediaPlayer.create(this, R.raw.game_over);
                gameOverPlayer.start();
            }

            gameView.setGameOver(true); // stop updates and input

            // Show current and high score
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

            tvFinalScore.setText(getString(R.string.final_score, score));
            tvHighScoreFinal.setText(getString(R.string.high_score, best));

            gameOverOverlay.setVisibility(View.VISIBLE); // Show game over overlay
        }));

        // Show play again button
        findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            finish();
            startActivity(new Intent(this, TapModeActivity.class));
        });

        // Show home button
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}