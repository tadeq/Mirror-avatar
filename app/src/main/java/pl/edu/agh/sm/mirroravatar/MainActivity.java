package pl.edu.agh.sm.mirroravatar;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.JavaCamera2View;
import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

public class MainActivity extends AppCompatActivity {

    private static final CameraSource CAMERA_SOURCE = CameraSource.FRONT;
    private static final String FACE_DIR = "facelib";
    private static final String FACE_MODEL = "haarcascade_frontalface_alt2.xml";
    private static final String LEFT_EYE_DIR = "lefteyelib";
    private static final String LEFT_EYE_MODEL = "haarcascade_lefteye_2splits.xml";
    private static final String RIGHT_EYE_DIR = "righteyelib";
    private static final String RIGHT_EYE_MODEL = "haarcascade_righteye_2splits.xml";
    private static final int BYTE_SIZE = 4096;
    private static final int REQUEST_CODE_PERMISSIONS = 111;
    private static final String[] REQUIRED_PERMISSIONS = {
            CAMERA,
            WRITE_EXTERNAL_STORAGE,
            READ_EXTERNAL_STORAGE,
            RECORD_AUDIO,
            ACCESS_FINE_LOCATION
    };

    private JavaCamera2View javaCameraView;
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

        javaCameraView = (JavaCamera2View) findViewById(R.id.cameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCameraIndex(CAMERA_SOURCE.getCameraIndex());
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
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkOpenCV();
        if (javaCameraView != null) {
            javaCameraView.enableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
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

    private void callFaceDetector() {
        loadFaceLib();
        loadLeftEyeDetector();
        loadRightEyeDetector();
        eyeTrackingProcessor = new OpenCvEyeTrackingProcessor(faceDetector, leftEyeDetector, rightEyeDetector);
        javaCameraView.setCvCameraViewListener(eyeTrackingProcessor);
        javaCameraView.enableView();
    }

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
                    eyeTrackingProcessor.setScreenRotation(180);
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

    private enum CameraSource {
        FRONT(98), BACK(99);

        private final int cameraIndex;

        CameraSource(int cameraIndex) {
            this.cameraIndex = cameraIndex;
        }

        private int getCameraIndex() {
            return cameraIndex;
        }
    }
}