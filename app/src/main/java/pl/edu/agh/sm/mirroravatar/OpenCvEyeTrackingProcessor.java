package pl.edu.agh.sm.mirroravatar;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.util.Optional;

import pl.edu.agh.sm.mirroravatar.camera.HardwareCamera;

import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE;

public class OpenCvEyeTrackingProcessor implements HardwareCamera.CameraListener {

    private final MainActivity.EyeDetectionHandler eyeDetectionHandler;
    private final CascadeClassifier faceDetector;
    private final CascadeClassifier leftEyeDetector;
    private final CascadeClassifier rightEyeDetector;

    private Mat imageMat, grayMat;
    private Double imageRatio;
    private int screenRotation = 0;

    public OpenCvEyeTrackingProcessor(MainActivity.EyeDetectionHandler eyeDetectionHandler, CascadeClassifier faceDetector, CascadeClassifier leftEyeDetector, CascadeClassifier rightEyeDetector) {
        this.eyeDetectionHandler = eyeDetectionHandler;
        this.faceDetector = faceDetector;
        this.leftEyeDetector = leftEyeDetector;
        this.rightEyeDetector = rightEyeDetector;
    }

    public void setScreenRotation(int screenRotation) {
        this.screenRotation = screenRotation;
    }

    @Override
    public void onCameraStarted(int width, int height) {
        imageMat = new Mat(width, height, CvType.CV_8UC4);
        grayMat = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraStopped() {
        imageMat.release();
        grayMat.release();
    }

    @Override
    public void onCameraFrame(HardwareCamera.CameraFrame inputFrame) {
        imageMat = inputFrame.rgba();
        // downsize gray for increase efficiency
        Mat graySrc = inputFrame.gray();
        Size imageSize = new Size(graySrc.width(), graySrc.height());
        imageRatio = ratioTo(600, imageSize);
        // imageRatio = 1.0;
        grayMat = getScaledImage(graySrc, imageSize);

        // detect face rectangle
        detectFace();
    }


    public void detectFace() {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(grayMat, faceDetections);
        for (Rect faceRect : faceDetections.toArray()) {
            detectEyes(faceRect);
        }
    }

    private void detectEyes(Rect rect) {
        // compute eyes area
        Rect eyearea_right = new Rect(rect.x + rect.width / 7,
                (int) (rect.y + (rect.height / 4.0)),
                (rect.width - 2 * rect.width / 7) / 2, (int) (rect.height / 4.0));
        Rect eyearea_left = new Rect(rect.x + rect.width / 7
                + (rect.width - 2 * rect.width / 7) / 2,
                (int) (rect.y + (rect.height / 4.0)),
                (rect.width - 2 * rect.width / 7) / 2, (int) (rect.height / 4.0));

        detectIris(eyearea_left, leftEyeDetector).ifPresent(pointPointPair ->
                setCenterPointAndIrisTextViews(pointPointPair, MainActivity.LEFT_EYE_MESSAGE_ID));
        detectIris(eyearea_right, rightEyeDetector).ifPresent(pointPointPair ->
                setCenterPointAndIrisTextViews(pointPointPair, MainActivity.RIGHT_EYE_MESSAGE_ID));
    }

    private void setCenterPointAndIrisTextViews(Pair<Point, Point> eye, int eyeMessageId) {
        Message msg = eyeDetectionHandler.obtainMessage();
        msg.what = eyeMessageId;
        Bundle b = new Bundle();
        Point pseudoEyeCenter = eye.first;
        Point iris = eye.second;
        b.putString(MainActivity.EYE_CENTER_POINT_X_ID, String.valueOf(pseudoEyeCenter.x));
        b.putString(MainActivity.EYE_CENTER_POINT_Y_ID, String.valueOf(pseudoEyeCenter.y));
        b.putString(MainActivity.IRIS_POINT_X_ID, String.valueOf(iris.x));
        b.putString(MainActivity.IRIS_POINT_Y_ID, String.valueOf(iris.y));
        msg.setData(b);
        eyeDetectionHandler.sendMessage(msg);
    }

    private Optional<Pair<Point, Point>> detectIris(Rect area, CascadeClassifier classifier) {
        Mat mROI = grayMat.submat(area);
        MatOfRect eyes = new MatOfRect();
        classifier.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());
        Rect[] eyesArray = eyes.toArray();
        if (eyesArray.length > 0) {
            Rect eye = eyesArray[0];
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eye_only_rectangle = new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4),
                    eye.width, (int) (eye.height * 0.6));
            Point pseudoEyeCenter = new Point(eye_only_rectangle.x + eye_only_rectangle.width / 2.0,
                    eye_only_rectangle.y + eye_only_rectangle.height / 2.0);
            mROI = grayMat.submat(eye_only_rectangle);
            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
            Point iris = new Point(mmG.minLoc.x + eye_only_rectangle.x, mmG.minLoc.y + eye_only_rectangle.y);
            Log.d("EyesDetector", iris.toString());
            return Optional.of(new Pair<>(pseudoEyeCenter, iris));
        }
        return Optional.empty();
    }

    public Mat getScaledImage(Mat src, Size imageSize) {
        Mat dst = new Mat();
        if (!imageRatio.equals(1.0)) {
            Size dstSize = new Size(imageSize.width * imageRatio, imageSize.height * imageRatio);
            Imgproc.resize(src, dst, dstSize);
        } else {
            dst = src.clone();
        }

        switch (screenRotation) {
            case 0:
                Core.rotate(dst, dst, ROTATE_90_CLOCKWISE);
                Core.flip(dst, dst, 1);
                break;
            case 180:
                Core.rotate(dst, dst, ROTATE_90_COUNTERCLOCKWISE);
                break;
            case 270:
                Core.flip(dst, dst, 0);
        }

        return dst;
    }

    public static Double ratioTo(double heightMax, Size src) {
        double w = src.width;
        double h = src.height;
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

}
