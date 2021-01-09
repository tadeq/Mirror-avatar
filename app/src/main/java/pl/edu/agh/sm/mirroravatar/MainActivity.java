package pl.edu.agh.sm.mirroravatar;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.OrientationEventListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import pl.edu.agh.sm.mirroravatar.camera.HardwareCamera;

import static android.Manifest.permission.CAMERA;
import static android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

public class MainActivity extends AppCompatActivity {

    private static final String FACE_DIR = "facelib";
    private static final String FACE_MODEL = "haarcascade_frontalface_alt2.xml";
    private static final String LEFT_EYE_DIR = "lefteyelib";
    private static final String LEFT_EYE_MODEL = "haarcascade_lefteye_2splits.xml";
    private static final String RIGHT_EYE_DIR = "righteyelib";
    private static final String RIGHT_EYE_MODEL = "haarcascade_righteye_2splits.xml";
    private static final int BYTE_SIZE = 4096;
    private static final int REQUEST_CODE_PERMISSIONS = 111;
    private static final String[] REQUIRED_PERMISSIONS = {
            CAMERA
    };

    private HardwareCamera hardwareCamera;
    private TextView rotationTextView;
    private OpenCvEyeTrackingProcessor eyeTrackingProcessor;
    private CascadeClassifier faceDetector = null;
    private CascadeClassifier leftEyeDetector = null;
    private CascadeClassifier rightEyeDetector = null;
    private File faceDir;
    private File leftEyeDir;
    private File rightEyeDir;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        if (allPermissionsGranted()) {
            checkOpenCV();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        rotationTextView = findViewById(R.id.rotation_tv);

        OrientationEventListener orientationEventListener = initLocationListener();
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {
            orientationEventListener.disable();
        }
        callFaceDetector();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hardwareCamera != null && hardwareCamera.isConnected()) {
            hardwareCamera.disconnectCamera();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkOpenCV();
        if (hardwareCamera != null && !hardwareCamera.isConnected()) {
            hardwareCamera.connectCamera();
        }
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onDestroy() {
        super.onDestroy();
        if (hardwareCamera != null && hardwareCamera.isConnected()) {
            hardwareCamera.disconnectCamera();
        }
        if (faceDir.exists()) {
            faceDir.delete();
        }
        if (leftEyeDir.exists()) {
            leftEyeDir.delete();
        }
        if (rightEyeDir.exists()) {
            rightEyeDir.delete();
        }
    }


    private void checkOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV loaded successfully!");
        } else {
            Log.d("OpenCV", "Cannot load OpenCV!");
        }
    }

    private boolean allPermissionsGranted() {
        return Arrays.stream(REQUIRED_PERMISSIONS).allMatch(e ->
                ContextCompat.checkSelfPermission(getBaseContext(), e) == PackageManager.PERMISSION_GRANTED
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Checking the request code of our request
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                checkOpenCV();
            } else {
                Log.d("Permissions", "Permissions not granted by the user.");
                finish();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void callFaceDetector() {
        loadFaceLib();
        loadLeftEyeDetector();
        loadRightEyeDetector();
        eyeTrackingProcessor = new OpenCvEyeTrackingProcessor(this, faceDetector, leftEyeDetector, rightEyeDetector);
        hardwareCamera = new HardwareCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        hardwareCamera.setCameraListener(eyeTrackingProcessor);
        hardwareCamera.connectCamera();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void loadFaceLib() {
        InputStream modelInputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
        faceDir = getDir(FACE_DIR, Context.MODE_PRIVATE);
        File faceModel = new File(faceDir, FACE_MODEL);
        loadModel(modelInputStream, faceModel);
        faceDetector = new CascadeClassifier(faceModel.getAbsolutePath());
        if (faceDetector.empty()) {
            faceDetector = null;
        } else {
            faceDir.delete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void loadLeftEyeDetector() {
        InputStream modelInputStream = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
        leftEyeDir = getDir(LEFT_EYE_DIR, Context.MODE_PRIVATE);
        File leftEyeModel = new File(leftEyeDir, LEFT_EYE_MODEL);
        loadModel(modelInputStream, leftEyeModel);
        leftEyeDetector = new CascadeClassifier(leftEyeModel.getAbsolutePath());
        if (leftEyeDetector.empty()) {
            leftEyeDetector = null;
        } else {
            leftEyeDir.delete();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void loadRightEyeDetector() {
        InputStream modelInputStream = getResources().openRawResource(R.raw.haarcascade_righteye_2splits);
        rightEyeDir = getDir(RIGHT_EYE_DIR, Context.MODE_PRIVATE);
        File rightEyeModel = new File(rightEyeDir, RIGHT_EYE_MODEL);
        loadModel(modelInputStream, rightEyeModel);
        rightEyeDetector = new CascadeClassifier(rightEyeModel.getAbsolutePath());
        if (rightEyeDetector.empty()) {
            rightEyeDetector = null;
        } else {
            rightEyeDir.delete();
        }
    }

    private void loadModel(InputStream modelInputStream, File model) {
        try {
            if (!model.exists()) {
                FileOutputStream modelOutputStream = new FileOutputStream(model);
                byte[] buffer = new byte[BYTE_SIZE];
                int bytesRead;
                while ((bytesRead = modelInputStream.read(buffer)) != -1) {
                    modelOutputStream.write(buffer, 0, bytesRead);
                }
                modelInputStream.close();
                modelOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private OrientationEventListener initLocationListener() {
        return new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (Range.create(45, 134).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_270_degree));
                    eyeTrackingProcessor.setScreenRotation(270);
                } else if (Range.create(135, 224).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_180_degree));
                    eyeTrackingProcessor.setScreenRotation(0);
                } else if (Range.create(225, 314).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_90_degree));
                    eyeTrackingProcessor.setScreenRotation(90);
                } else {
                    rotationTextView.setText(getString(R.string.n_0_degree));
                    eyeTrackingProcessor.setScreenRotation(180);
                }
            }
        };
    }

}