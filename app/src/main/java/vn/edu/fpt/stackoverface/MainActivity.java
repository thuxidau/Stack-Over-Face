package vn.edu.fpt.stackoverface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private BlockGameView gameView;
    private FaceAnalyzer faceAnalyzer;

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
        });

        faceAnalyzer = new FaceAnalyzer(() -> {
            gameView.post(() -> {
                gameView.dropBlock();
                tvScore.setText(getString(R.string.score, gameView.getScore()));
            });
        });

        gameView.setGameOverCallback(() -> {
            runOnUiThread(() -> {
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
            startActivity(getIntent()); // restarts it
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

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera initialization failed.", Toast.LENGTH_SHORT).show();
                Log.e("CameraX", "Error binding camera use cases", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
}