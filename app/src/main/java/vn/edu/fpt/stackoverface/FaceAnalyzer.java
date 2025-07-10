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

    private final Context context;
    private final FaceDetector detector;
    private final Runnable blinkCallback;
    private Runnable faceNotDetectedCallback;
    public static boolean faceWarningShown = false;
    private boolean active = true;
    private long lastBlinkTime = 0;
    public static long lastFaceTime = System.currentTimeMillis();
    private static final long BLINK_COOLDOWN = 300;
    private static final float BLINK_THRESHOLD = 0.4f;

    public FaceAnalyzer(Context context, Runnable blinkCallback) {
        this.context = context;
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

        if (!active) {
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
                    lastFaceTime = System.currentTimeMillis(); // face found
                    faceWarningShown = false; // reset flag when face returns

                    // dismiss alert if it's showing
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).dismissFaceAlert();
                    }

                    Face face = faces.get(0);
                    Float leftEye = face.getLeftEyeOpenProbability();
                    Float rightEye = face.getRightEyeOpenProbability();

                    if (blinkCallback != null && leftEye != null && rightEye != null &&
                            leftEye < BLINK_THRESHOLD &&
                            rightEye < BLINK_THRESHOLD) {

                        if (System.currentTimeMillis() - lastBlinkTime > BLINK_COOLDOWN) {
                            lastBlinkTime = System.currentTimeMillis();
                            if (active) blinkCallback.run();
                        }
                    }
                }
                // face not found
                else {
                    long now = System.currentTimeMillis();
                    if (!faceWarningShown && now - lastFaceTime > 3000) {
                        faceWarningShown = true;
                        faceNotDetectedCallback.run();
                    }
                }
            })
            .addOnCompleteListener(task -> imageProxy.close());
    }

    public void setFaceNotDetectedCallback(Runnable callback) {
        this.faceNotDetectedCallback = callback;
    }

    public void stop() {
        active = false;
    }
}