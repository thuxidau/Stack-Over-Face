package vn.edu.fpt.stackoverface;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceAnalyzer implements ImageAnalysis.Analyzer {

    private final Context context; // Android context
    private final FaceDetector detector; // ML Kit’s FaceDetector
    private final Runnable blinkCallback; // Runnable to be called when a blink is detected
    private Runnable faceNotDetectedCallback; // Runnable (optional) called when no face is found
    public static boolean faceWarningShown = false; // Ensure the user is warned if their face is missing
    private boolean active = true; // Indicate whether face detection is active
    private long lastBlinkTime = 0; // Time of the last detected blink
    public static long lastFaceTime = System.currentTimeMillis(); // Time when a face was last seen
    private static final long BLINK_COOLDOWN = 300; // Prevents rapid multiple blink triggers
    private static final float BLINK_THRESHOLD = 0.4f; // Minimum eye open probability to consider an eye as closed

    public FaceAnalyzer(Context context, Runnable blinkCallback) {
        this.context = context;
        this.blinkCallback = blinkCallback;

        // Set up face detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Prioritizes speed over accuracy
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enables eye open probabilities
                .build();

        detector = FaceDetection.getClient(options);
    }

    public void setFaceNotDetectedCallback(Runnable callback) {
        this.faceNotDetectedCallback = callback;
    }

    public void stop() {
        active = false;
    }

    @OptIn(markerClass = ExperimentalGetImage.class) // Suppress a warning because getImage() is marked experimental in CameraX
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        // Skips processing if the camera frame is null
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Skips processing if face detection is currently disabled
        if (!active) {
            imageProxy.close();
            return;
        }

        // Convert to ML Kit format
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // Run face detection
        detector.process(image)
            .addOnSuccessListener(faces -> {
                // Face is detected
                if (!faces.isEmpty()) {
                    lastFaceTime = System.currentTimeMillis(); // Face found
                    faceWarningShown = false; // Reset flag when face returns

                    // Dismiss alert if it's showing
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).dismissFaceAlert();
                    }

                    // Blink detection
                    Face face = faces.get(0);
                    // Gets eye open probabilities (0.0 = closed, 1.0 = open)
                    Float leftEye = face.getLeftEyeOpenProbability();
                    Float rightEye = face.getRightEyeOpenProbability();

                    // Both eyes are detected and mostly closed
                    if (blinkCallback != null && leftEye != null && rightEye != null &&
                            leftEye < BLINK_THRESHOLD && rightEye < BLINK_THRESHOLD) {
                        // The last blink was over 300ms ago
                        if (System.currentTimeMillis() - lastBlinkTime > BLINK_COOLDOWN) {
                            lastBlinkTime = System.currentTimeMillis();
                            // If face is detected
                            if (active) blinkCallback.run();
                        }
                    }
                }

                // Face is not detected
                else {
                    // No face is seen for > 3 seconds, and we haven’t already shown a warning
                    if (!faceWarningShown && System.currentTimeMillis() - lastFaceTime > 3000) {
                        faceWarningShown = true;
                        faceNotDetectedCallback.run();
                    }
                }
            })
            .addOnCompleteListener(task -> imageProxy.close()); // Always release the frame
    }
}