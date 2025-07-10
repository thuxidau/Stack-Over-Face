package vn.edu.fpt.stackoverface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            startCamera();
        }

        // Set initial score (0) after layout is ready
        gameView.post(() -> {
            tvScore.setText(getString(R.string.score, gameView.getScore()));
            gameView.setTapEnabled(false);
        });

        faceAnalyzer = new FaceAnalyzer(this, () -> {
            gameView.post(() -> {
                gameView.dropBlock();
                gameView.setContext(this); // Pass context for sound setup
                tvScore.setText(getString(R.string.score, gameView.getScore()));
            });
        });

        faceAnalyzer.setFaceNotDetectedCallback(() -> runOnUiThread(this::showTryAgainDialog));

        gameView.setGameOverCallback(() -> {
            runOnUiThread(() -> {
                gameView.setGameOver(true); // stop updates and input
                faceAnalyzer.stop();

                MediaPlayer gameOverPlayer = MediaPlayer.create(this, R.raw.game_over);
                gameOverPlayer.start();

                int score = gameView.getScore();

                // Save high score
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

                tvFinalScore.setText(getString(R.string.score, score));
                tvHighScoreFinal.setText(getString(R.string.high_score, best));

                gameOverOverlay.setVisibility(View.VISIBLE);
            });
        });

        findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            finish(); // closes current MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent); // restarts it
        });

        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceAnalyzer);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                runOnUiThread(this::showNoCameraDialog);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void switchToTapMode() {
        Intent intent = new Intent(MainActivity.this, TapModeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showPermissionDialog();
            }
        }
    }

    private void showTryAgainDialog() {
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (faceAlertDialog != null && faceAlertDialog.isShowing()) return;

            faceAlertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Face Not Detected")
                    .setMessage("Face not detected. Try again or switch to Tap Mode?")
                    .setCancelable(false)
                    .setPositiveButton("Try Again", (d, w) -> {
                        FaceAnalyzer.lastFaceTime = System.currentTimeMillis();
                        FaceAnalyzer.faceWarningShown = false; // allow warning to show again if still no face
                        faceAlertDialog = null;
                    })
                    .setNegativeButton("Switch to Tap Mode", (d, w) -> {
                        switchToTapMode();
                        faceAlertDialog = null;
                    })
                    .setOnDismissListener(dialog -> faceAlertDialog = null)
                    .show();
        });
    }

    private void showPermissionDialog() {
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;

            new AlertDialog.Builder(this)
                    .setTitle("Camera Permission Needed")
                    .setMessage("Allow camera access to play with blinks. Or switch to Tap Mode.")
                    .setCancelable(false)
                    .setPositiveButton("Switch to Tap Mode", (d, w) -> switchToTapMode())
                    .setNegativeButton("Close", null)
                    .show();
        });
    }

    private void showNoCameraDialog() {
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

    public void dismissFaceAlert() {
        runOnUiThread(() -> {
            if (faceAlertDialog != null && faceAlertDialog.isShowing()) {
                faceAlertDialog.dismiss();
                faceAlertDialog = null;
            }
        });
    }
}