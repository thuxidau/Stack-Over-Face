package vn.edu.fpt.stackoverface;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends MusicBoundActivity {

    private PreviewView previewView;
    private BlockGameView gameView;
    private FaceAnalyzer faceAnalyzer;
    private AlertDialog faceAlertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.cameraPreview);
        gameView = findViewById(R.id.gameView);
        TextView tvScore = findViewById(R.id.tvScore);

        ImageButton btnPause = findViewById(R.id.btnPause);
        TextView tvCountdown = findViewById(R.id.tvCountdown);
        final boolean[] isPaused = {false};

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            // Granted
            startCamera();
        }

        // Initialize game
        gameView.post(() -> {
            tvScore.setText(getString(R.string.score, gameView.getScore())); // Set the initial score
            gameView.setTapEnabled(false); // Disable taps — game starts with blink input, not touch
        });

        // Set up face analyzer
        faceAnalyzer = new FaceAnalyzer(this, () -> gameView.post(() -> {
            gameView.setContext(this); // Pass context for sound setup
            gameView.dropBlock();
            tvScore.setText(getString(R.string.score, gameView.getScore()));
        }));

        FaceAnalyzer.resetState();

        // Show alert when no face is found for 3 seconds
        faceAnalyzer.setFaceNotDetectedCallback(() -> runOnUiThread(this::showTryAgainDialog));

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
            gameView.setGameOver(true); // Stop updates and input
            faceAnalyzer.stop(); // Stop face detection

            // Play game over sound
            SharedPreferences prefs_sound = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs_sound.getBoolean("sound_enabled", true)) {
                MediaPlayer gameOverPlayer = MediaPlayer.create(this, R.raw.game_over);
                gameOverPlayer.start();
            }

            // Save high score
            int score = gameView.getScore();
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            int best = prefs.getInt("best_score", 0);
            if (score > best) {
                prefs.edit().putInt("best_score", score).apply();
                best = score;
            }

            // Update overlay UI
            tvScore.setVisibility(View.GONE); // hide in-game score

            TextView tvFinalScore = findViewById(R.id.tvFinalScore);
            TextView tvHighScoreFinal = findViewById(R.id.tvHighScoreFinal);
            LinearLayout gameOverOverlay = findViewById(R.id.gameOverOverlay);

            // Set current and high score
            tvFinalScore.setText(getString(R.string.final_score, score));
            tvHighScoreFinal.setText(getString(R.string.high_score, best));

            // Show game over overlay
            gameOverOverlay.setVisibility(View.VISIBLE);
        }));

        // Restart the activity
        findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            finish(); // closes current MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent); // restarts it
        });

        // Navigate to StartActivity
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Always re-bind the camera when returning to app
            startCamera();
        }
    }

    private void startCamera() {
        // Get camera provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        // Set up the camera once ready
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                cameraProvider.unbindAll(); // Unbind any previous use of the camera

                // Set up camera preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up image analysis (face detection)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder() // Give access to the raw camera frames
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Only get the latest frame, avoid lag
                        .build();

                // Process each frame on the main thread
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceAnalyzer);

                // Bind this camera + preview + analysis to the activity lifecycle (starts and stops automatically)
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis);
            }
            // If failed, show alert
            catch (ExecutionException | InterruptedException e) {
                runOnUiThread(this::showNoCameraDialog);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void switchToTapMode() {
        Intent intent = new Intent(MainActivity.this, TapModeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clears the activity stack so the user can’t go "back" to the camera mode
        startActivity(intent);
        finish();
    }

    // Handle the result of the camera permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(); // Restart the camera preview and face analyzer
            } else {
                showPermissionDialog();
            }
        }
    }

    private void showTryAgainDialog() {
        // Make sure the activity is still alive before showing a dialog
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            // If face is detected, the alert will disappear
            if (faceAlertDialog != null && faceAlertDialog.isShowing()) return;

            faceAlertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Face Not Detected")
                    .setMessage("Face not detected. Try again or switch to Tap Mode?")
                    .setCancelable(false)
                    .setPositiveButton("Try Again", (d, w) -> {
                        // Reset the face detection timer so the warning can reappear later if needed
                        FaceAnalyzer.lastFaceTime = System.currentTimeMillis();
                        FaceAnalyzer.faceWarningShown = false; // allow warning to show again if still no face
                        faceAlertDialog = null;
                    })
                    .setNegativeButton("Switch to Tap Mode", (d, w) -> {
                        // Transition to tap mode
                        switchToTapMode();
                        faceAlertDialog = null;
                    })
                    .setOnDismissListener(dialog -> faceAlertDialog = null) // only one dialog shows at a time
                    .show();
        });
    }

    private void showPermissionDialog() {
        // Make sure the activity is still alive before showing a dialog
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;

            new AlertDialog.Builder(this)
                    .setTitle("Camera Permission Needed")
                    .setMessage("Allow camera access to play with blinks. Or switch to Tap Mode.")
                    .setCancelable(false)
                    .setPositiveButton("Switch to Tap Mode", (d, w) -> switchToTapMode())
                    .setNegativeButton("Go to Settings", (d, w) -> openAppSettings())
                    .show();
        });
    }

    private void showNoCameraDialog() {
        // Make sure the activity is still alive before showing a dialog
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;

            new AlertDialog.Builder(this)
                    .setTitle("Camera Not Available")
                    .setMessage("Your device does not support camera. Switch to Tap Mode?")
                    .setCancelable(false)
                    .setPositiveButton("Switch", (d, w) -> switchToTapMode())
                    .setNegativeButton("Exit", (d, w) -> finish())
                    .show();
        });
    }

    // Dismiss the "Face Not Detected" alert if it's currently detected
    public void dismissFaceAlert() {
        runOnUiThread(() -> {
            if (faceAlertDialog != null && faceAlertDialog.isShowing()) {
                faceAlertDialog.dismiss();
                faceAlertDialog = null;
            }
        });
    }

    // Open app settings
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}