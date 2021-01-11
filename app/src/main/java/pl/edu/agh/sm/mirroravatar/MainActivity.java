package pl.edu.agh.sm.mirroravatar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import android.util.Log;
import android.util.Range;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private ObjRenderer modelRenderer;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
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

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private ObjRenderer modelRenderer;

    private static final String FACE_DIR = "facelib";
    private static final String FACE_MODEL = "haarcascade_frontalface_alt2.xml";
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
    private CascadeClassifier faceDetector = null;
    private File faceDir;
    private Mat imageMat, grayMat;
    private int screenRotation = 0;

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

        // Create our Preview view and set it as the content of our activity.
//        cameraPreview = new CameraPreview(this, camera);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        preview.addView(cameraPreview);

        final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60.0);
        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);

        javaCameraView = (JavaCamera2View) findViewById(R.id.cameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCameraIndex(CameraCharacteristics.LENS_FACING_FRONT);
        javaCameraView.setCvCameraViewListener(this);
        rotationTextView = findViewById(R.id.rotation_tv);

        // Add mSurface to your root view
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));

        modelRenderer = new ObjRenderer(this);
        surface.setSurfaceRenderer(modelRenderer);
        surface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                modelRenderer.onTouchEvent(motionEvent);
                return true;
            }
        });

    }


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
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
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        imageMat = new Mat(width, height, CvType.CV_8UC4);
        grayMat = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        imageMat.release();
        grayMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        imageMat = inputFrame.rgba();
        // downsize gray for increase efficiency
        Mat graySrc = inputFrame.gray();
        Size imageSize = new Size(graySrc.width(), graySrc.height());
        Double imageRatio = OpenCvUtils.ratioTo480(imageSize);
        grayMat = OpenCvUtils.get480Image(graySrc, imageSize, imageRatio, screenRotation);

        // detect face rectangle
        OpenCvUtils.drawFaceRectangle(faceDetector, imageMat, grayMat, imageRatio, screenRotation);
        return imageMat;
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
        if (faceDetector != null && faceDetector.empty()) {
            faceDetector = null;
        } else {
            faceDir.delete();
        }
        javaCameraView.enableView();
    }

    private void loadFaceLib() {
        try {
            InputStream modelInputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            faceDir = getDir(FACE_DIR, Context.MODE_PRIVATE);
            File faceModel = new File(faceDir, FACE_MODEL);
            if (!faceModel.exists()) {
                FileOutputStream modelOutputStream = new FileOutputStream(faceModel);
                byte[] buffer = new byte[BYTE_SIZE];
                int bytesRead;
                while ((bytesRead = modelInputStream.read(buffer)) != -1) {
                    modelOutputStream.write(buffer, 0, bytesRead);
                }
                modelInputStream.close();
                modelOutputStream.close();
            }
            faceDetector = new CascadeClassifier(faceModel.getAbsolutePath());
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
                    screenRotation = 270;
                } else if (Range.create(135, 224).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_180_degree));
                    screenRotation = 180;
                } else if (Range.create(225, 314).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_90_degree));
                    screenRotation = 90;
                } else {
                    rotationTextView.setText(getString(R.string.n_0_degree));
                    screenRotation = 0;
                }
            }
        };
    }
}