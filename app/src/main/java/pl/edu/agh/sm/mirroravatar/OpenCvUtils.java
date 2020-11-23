package pl.edu.agh.sm.mirroravatar;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;
import static org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE;

public class OpenCvUtils {

    private static final Scalar RED = new Scalar(255.0, 0.0, 0.0);
    private static final Scalar GREEN = new Scalar(0.0, 255.0, 0.0);

    public static void drawFaceRectangle(CascadeClassifier faceDetector, Mat imageMat, Mat grayMat, Double imageRatio, int screenRotation) {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(grayMat, faceDetections);

        double scrW = imageMat.width();
        double scrH = imageMat.height();

        Log.d("FaceDetector", String.valueOf(faceDetections.toArray().length));
        for (Rect rect : faceDetections.toArray()) {
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
                    drawRect(imageMat, x, y, w, h);
                    drawDot(imageMat, x, y);
                    break;
                case 0:
                    drawRect(imageMat, y, x, h, w);
                    drawDot(imageMat, y, x);
                    break;
                case 180:
                    double yFix = scrW - y;
                    double hFix = yFix - rh;
                    drawRect(imageMat, yFix, x, hFix, w);
                    drawDot(imageMat, yFix, x);
                    break;
                case 270:
                    yFix = scrH - y;
                    hFix = yFix - rh;
                    drawRect(imageMat, x, yFix, w, hFix);
                    drawDot(imageMat, x, yFix);
                    break;
            }
        }
    }

    private static void drawRect(Mat imageMat, Double a, Double b, Double w, Double h) {
        Imgproc.rectangle(imageMat, new Point(a, b), new Point(w, h), RED, 3);
    }

    private static void drawDot(Mat imageMat, Double a, Double b) {
        Imgproc.circle(imageMat, new Point(a, b), 4, GREEN, -1, 8);
    }

    public static Mat get480Image(Mat src, Size imageSize, Double imageRatio, int screenRotation) {
        if (imageRatio.equals(1.0)) return src;
        Mat dst = new Mat();
        Size dstSize = new Size(imageSize.width * imageRatio, imageSize.height * imageRatio);
        Imgproc.resize(src, dst, dstSize);

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
