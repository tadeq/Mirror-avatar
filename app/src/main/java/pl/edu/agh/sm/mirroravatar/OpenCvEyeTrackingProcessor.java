package pl.edu.agh.sm.mirroravatar;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE;

public class OpenCvEyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final Scalar RED = new Scalar(255.0, 0.0, 0.0);
    private static final Scalar GREEN = new Scalar(0.0, 255.0, 0.0);
    private static final Scalar BLUE = new Scalar(0.0, 0.0, 255.0);
    private static final Scalar YELLOW = new Scalar(255.0, 255.0, 0.0);
    private static final Scalar PINK = new Scalar(255.0, 0.0, 255.0);
    private static final Scalar CYAN = new Scalar(0.0, 255.0, 255.0);
    private static final Scalar WHITE = new Scalar(255.0, 255.0, 255.0);

    private final CascadeClassifier faceDetector;
    private final CascadeClassifier leftEyeDetector;
    private final CascadeClassifier rightEyeDetector;

    private Mat imageMat, grayMat;
    private Double imageRatio;
    private int screenRotation = 0;

    public OpenCvEyeTrackingProcessor(CascadeClassifier faceDetector, CascadeClassifier leftEyeDetector, CascadeClassifier rightEyeDetector) {
        this.faceDetector = faceDetector;
        this.leftEyeDetector = leftEyeDetector;
        this.rightEyeDetector = rightEyeDetector;
    }

    public void setScreenRotation(int screenRotation) {
        this.screenRotation = screenRotation;
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
        imageRatio = ratioTo480(imageSize);
        // imageRatio = 1.0;
        grayMat = getScaledImage(graySrc, imageSize);

        // detect face rectangle
        drawFaceRectangle();
        return imageMat;
    }


    public void drawFaceRectangle() {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(grayMat, faceDetections);
        Log.d("FaceDetector", String.valueOf(faceDetections.toArray().length));

        for (Rect faceRect : faceDetections.toArray()) {
            // drawRect(faceRect, WHITE);
            drawEyeRectangles(faceRect);
        }
    }

    private void drawEyeRectangles(Rect rect) {
        // compute eyes area
        Rect eyearea_right = new Rect(rect.x + rect.width / 7,
                (int) (rect.y + (rect.height / 4.0)),
                (rect.width - 2 * rect.width / 7) / 2, (int) (rect.height / 4.0));
        Rect eyearea_left = new Rect(rect.x + rect.width / 7
                + (rect.width - 2 * rect.width / 7) / 2,
                (int) (rect.y + (rect.height / 4.0)),
                (rect.width - 2 * rect.width / 7) / 2, (int) (rect.height / 4.0));
        // drawRect(eyearea_left, RED);
        // drawRect(eyearea_right, RED);
        drawIris(eyearea_right, rightEyeDetector);
        drawIris(eyearea_left, leftEyeDetector);
    }

    private void drawIris(Rect area, CascadeClassifier classifier) {
        Mat mROI = grayMat.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        classifier.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Log.d("EyesDetector", String.valueOf(eyes.toArray().length));
        Rect[] eyesArray = eyes.toArray();
        if (eyesArray.length > 0) {
            Rect eye = eyesArray[0];
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eye_only_rectangle = new Rect((int) eye.tl().x,
                    (int) (eye.tl().y + eye.height * 0.4), (int) eye.width,
                    (int) (eye.height * 0.6));
            // drawRect(eye_only_rectangle, RED);
            mROI = grayMat.submat(eye_only_rectangle);
            Rect rotated_eye_only_rectangle = rotateRect(eye_only_rectangle);
            Mat vyrez = imageMat.submat(rotated_eye_only_rectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
            drawDot(vyrez, mmG.minLoc);

            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;

            Rect eye_template = new Rect((int) iris.x - 24 / 2, (int) iris.y
                    - 24 / 2, 24, 24);
            drawRect(eye_template, RED);
        }
    }

    private void drawRect(Rect rect, Scalar color) {
        Rect rotatedRect = rotateRect(rect);
        drawRect(rotatedRect.tl().x, rotatedRect.tl().y, rotatedRect.br().x, rotatedRect.br().y, color);
    }

    private Rect rotateRect(Rect rect) {
        int scrW = imageMat.width();
        int scrH = imageMat.height();
        double x = rect.x, y = rect.y, rw = rect.width, rh = rect.height;
        if (!imageRatio.equals(1.0)) {
            x /= imageRatio;
            y /= imageRatio;
            rw /= imageRatio;
            rh /= imageRatio;
        }
        double w = x + rw;
        double h = y + rh;

        switch (screenRotation) {
            case 90:
                return new Rect(new Point(x, y), new Point(w, h));
            case 0:
                return new Rect(new Point(y, x), new Point(h, w));
            case 180:
                double yFix = scrW - y;
                double hFix = yFix - rh;
                return new Rect(new Point(yFix, x), new Point(hFix, w));
            case 270:
                yFix = scrH - y;
                hFix = yFix - rh;
                return new Rect(new Point(x, yFix), new Point(w, hFix));
            default:
                return rect;
        }
    }

    private void drawRect(Double a, Double b, Double w, Double h, Scalar color) {
        Imgproc.rectangle(imageMat, new Point(a, b), new Point(w, h), color, 2);
    }

    private void drawDot(Mat mat, Point point) {
        int scrW = mat.width();
        int scrH = mat.height();
        double x = point.x, y = point.y;
        if (!imageRatio.equals(1.0)) {
            x /= imageRatio;
            y /= imageRatio;
        }

        switch (screenRotation) {
            case 90:
                drawDot(mat, x, y);
                break;
            case 0:
                drawDot(mat, y, x);
                break;
            case 180:
                double yFix = scrW - y;
                drawDot(mat, yFix, x);
                break;
            case 270:
                yFix = scrH - y;
                drawDot(mat, x, yFix);
                break;
        }
    }

    private void drawDot(Mat mat, Double a, Double b) {
        Imgproc.circle(mat, new Point(a, b), 4, BLUE, -1, 8);
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

    public static Double ratioTo480(Size src) {
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

}
