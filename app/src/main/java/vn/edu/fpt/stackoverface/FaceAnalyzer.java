package vn.edu.fpt.stackoverface;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import com.google.mlkit.vision.face.Face;

public class FaceAnalyzer implements ImageAnalysis.Analyzer {

    private final FaceDetector detector;
    private final Runnable blinkCallback;

    private long lastBlinkTime = 0;
    private static final long BLINK_COOLDOWN = 300;
    private static final float BLINK_THRESHOLD = 0.2f;

    public FaceAnalyzer(Runnable blinkCallback) {
        this.blinkCallback = blinkCallback;

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        detector = FaceDetection.getClient(options);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        Float leftEye = face.getLeftEyeOpenProbability();
                        Float rightEye = face.getRightEyeOpenProbability();

                        if (leftEye != null && rightEye != null &&
                                leftEye < BLINK_THRESHOLD &&
                                rightEye < BLINK_THRESHOLD) {

                            long now = System.currentTimeMillis();
                            if (now - lastBlinkTime > BLINK_COOLDOWN) {
                                lastBlinkTime = now;
                                blinkCallback.run(); // Call the lambda passed from MainActivity
                            }
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }
}