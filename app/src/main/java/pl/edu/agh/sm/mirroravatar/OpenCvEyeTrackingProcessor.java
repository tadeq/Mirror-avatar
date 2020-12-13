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
    private static final int TRAIN_FRAMES = 5;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;
    private static final int METHOD = 2;

    private final CascadeClassifier faceDetector;
    private final CascadeClassifier leftEyeDetector;

    private Mat imageMat, grayMat;
    private Double imageRatio;
    private int screenRotation = 0;
    private int learn_frames = 0;
    private Mat templateR;
    private Mat templateL;

    public OpenCvEyeTrackingProcessor(CascadeClassifier faceDetector, CascadeClassifier leftEyeDetector) {
        this.faceDetector = faceDetector;
        this.leftEyeDetector = leftEyeDetector;
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
//        imageRatio = ratioTo480(imageSize);
        imageRatio = 1.0;
        grayMat = getScaledImage(graySrc, imageSize);

        // detect face rectangle
        drawFaceRectangle();
        return imageMat;
    }


    public void drawFaceRectangle() {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(grayMat, faceDetections);
        Log.d("FaceDetector", String.valueOf(faceDetections.toArray().length));

        if (faceDetections.toArray().length > 0) {
            Rect biggestFaceRect = faceDetections.toArray()[0];
            for (Rect faceRect : faceDetections.toArray()) {
                if (faceRect.area() > biggestFaceRect.area()) {
                    biggestFaceRect = faceRect;
                }
            }
            drawRect(biggestFaceRect);
            drawEyeRects(biggestFaceRect);
        }
    }

    private void drawEyeRects(Rect rect) {
        // compute eyes area
        Rect eyearea_right = new Rect(rect.x + rect.width / 16,
                (int) (rect.y + (rect.height / 4.5)),
                (rect.width - 2 * rect.width / 16) / 2, (int) (rect.height / 3.0));
        Rect eyearea_left = new Rect(rect.x + rect.width / 16
                + (rect.width - 2 * rect.width / 16) / 2,
                (int) (rect.y + (rect.height / 4.5)),
                (rect.width - 2 * rect.width / 16) / 2, (int) (rect.height / 3.0));
        drawRect(eyearea_left);
        drawRect(eyearea_right);
//        templateR = get_template(eyearea_right);
//        templateL = get_template(eyearea_left);
//        learn_frames++;
        if (learn_frames < TRAIN_FRAMES) {
            templateR = get_template(eyearea_right);
            templateL = get_template(eyearea_left);
            if (templateL != null && templateR != null) {
                learn_frames++;
            }
        } else {
            // Learning finished, use the new templates for template
            // matching
            match_eye(eyearea_right, templateR, METHOD);
            match_eye(eyearea_left, templateL, METHOD);
        }
    }

    private void match_eye(Rect area, Mat mTemplate, int type) {
        Point matchLoc;
        Mat mROI = grayMat.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() <= 0 || mTemplate.rows() <= 0) {
            return;
        }
        if (result_cols <= 0 || result_rows <= 0)
            return;
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);
        Rect matchLoc_t = new Rect(matchLoc_tx, matchLoc_ty);
        drawRect(matchLoc_t);
        drawDot(imageMat, new Point(matchLoc.x + mTemplate.cols() / 2.0 + area.x, matchLoc.y + mTemplate.rows() / 2.0 + area.y));
    }

    private Mat get_template(Rect area) {
        Mat template = new Mat();
        Mat mROI = grayMat.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        leftEyeDetector.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());


        Log.d("EyesDetector", String.valueOf(eyes.toArray().length));
        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect eye = eyesArray[i];
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eye_only_rectangle = new Rect((int) eye.tl().x,
                    (int) (eye.tl().y + eye.height * 0.4), (int) eye.width,
                    (int) (eye.height * 0.6));
//            drawRect(eye_only_rectangle);
            mROI = grayMat.submat(eye_only_rectangle);
            Rect rotated_eye_only_rectangle = rotateRect(eye_only_rectangle);
            Mat vyrez = imageMat.submat(rotated_eye_only_rectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
            drawDot(vyrez, mmG.minLoc);

            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;

            Rect eye_template = new Rect((int) iris.x - 24 / 2, (int) iris.y
                    - 24 / 2, 24, 24);
            drawRect(eye_template);

            template = (grayMat.submat(eye_template)).clone();
            return template;
        }
        return null;
    }

    private void drawRect(Rect rect) {
        Rect rotatedRect = rotateRect(rect);
        drawRect(rotatedRect.tl().x, rotatedRect.tl().y, rotatedRect.br().x, rotatedRect.br().y);
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

    private void drawRect(Double a, Double b, Double w, Double h) {
        Imgproc.rectangle(imageMat, new Point(a, b), new Point(w, h), RED, 2);
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
        Imgproc.circle(mat, new Point(a, b), 4, GREEN, -1, 8);
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
