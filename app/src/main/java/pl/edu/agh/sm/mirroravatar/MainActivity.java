package pl.edu.agh.sm.mirroravatar;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
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
    private Double imageRatio = 0.0;
    private Mat imageMat, grayMat;

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

        javaCameraView = findViewById(R.id.cameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCameraIndex(CameraCharacteristics.LENS_FACING_FRONT);
        javaCameraView.setCvCameraViewListener(this);
        rotationTextView = findViewById(R.id.rotation_tv);

        OrientationEventListener orientationEventListener = initLocationListener();
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {
            orientationEventListener.disable();
        }

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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseCallback);
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
        grayMat = inputFrame.gray();
//        grayMat = get480Image(inputFrame.gray());
        imageRatio = 1.0;

        // detect face rectangle
        drawFaceRectangle();

        return imageMat;
    }

    private Mat get480Image(Mat src) {
        Size imageSize = new Size(src.width(), src.height());
        imageRatio = ratioTo480(imageSize);
        if (imageRatio.equals(1.0)) return src;
        Size dstSize = new Size(imageSize.width*imageRatio, imageSize.height*imageRatio);
        Mat dst = new Mat();
        Imgproc.resize(src, dst, dstSize);
        return dst;
    }

    private Double ratioTo480(Size src) {
        double w = src.width;
        double h = src.height;
        double heightMax = 480;
        double ratio;
        if (w > h) {
            if (w < heightMax) return 1.0;
            ratio = heightMax / w;
        } else {
            if (h < heightMax) return 1.0;
            ratio = heightMax / h;
        }
        return ratio;
    }

    private void drawFaceRectangle() {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(grayMat, faceDetections);

        Log.d("Found faces", String.valueOf(faceDetections.toArray().length));
        for (Rect rect : faceDetections.toArray()) {
            double x, y, w, h;
            if (imageRatio.equals(1.0)) {
                x = rect.x;
                y = rect.y;
                w = x + rect.width;
                h = y + rect.height;
            } else {
                x = rect.x / imageRatio;
                y = rect.y / imageRatio;
                w = x + (rect.width / imageRatio);
                h = y + (rect.height / imageRatio);
            }

            Imgproc.rectangle(imageMat,
                    new Point(x, y),
                    new Point(w, h),
                    new Scalar(255, 0, 0));
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

    private void shortMsg(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Checking the request code of our request
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                checkOpenCV();
            } else {
                shortMsg(this, "Permissions not granted by the user.");
                finish();
            }
        }
    }

    private final BaseLoaderCallback baseCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                shortMsg(getApplicationContext(), "gut");
                loadFaceLib();
                if (faceDetector != null && faceDetector.empty()) {
                    faceDetector = null;
                } else {
                    faceDir.delete();
                }
                javaCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

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
                } else if (Range.create(135, 224).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_180_degree));
                } else if (Range.create(225, 314).contains(orientation)) {
                    rotationTextView.setText(getString(R.string.n_90_degree));
                } else {
                    rotationTextView.setText(getString(R.string.n_0_degree));
                }
            }
        };
    }
}