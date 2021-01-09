package pl.edu.agh.sm.mirroravatar.camera;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("deprecation")
public class HardwareCamera implements Camera.PreviewCallback {

    private final static String TAG = "HardwareCamera";
    private final int cameraIndex;
    private HardwareCamera.CameraListener mListener;
    private Camera mCamera;
    private int mFrameWidth;
    private int mFrameHeight;
    private CameraAccessFrame mCameraFrame;
    private CameraHandlerThread mThread = null;
    private SurfaceTexture texture = new SurfaceTexture(0);
    private byte[] mBuffer;

    public HardwareCamera(int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    public void setCameraListener(HardwareCamera.CameraListener listener) {
        mListener = listener;
    }

    public static Camera getCameraInstance(int facing) {

        Camera camera = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        int index = -1;
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == facing) {
                try {
                    camera = Camera.open(camIdx);
                    index = camIdx;
                    break;
                } catch (RuntimeException e) {
                    Log.e(TAG, String.format("Camera is not available (in use or does not exist). " +
                            "Facing: %s Index: %s Error: %s", facing, camIdx, e.getMessage()));
                }
            }
        }
        if (camera != null) {
            Log.d(TAG, String.format("Camera opened. Facing: %s Index: %s", facing, index));
        } else {
            Log.e(TAG, "Could not find any camera matching facing: " + facing);
        }
        return camera;
    }

    private synchronized void connectLocalCamera() {
        if (mThread == null) {
            mThread = new CameraHandlerThread(this);
        }
        synchronized (mThread) {
            mThread.openCamera();
        }
        mListener.onCameraStarted(mFrameWidth, mFrameHeight);
    }

    private void oldConnectCamera() {
        mCamera = getCameraInstance(cameraIndex);
        if (mCamera == null)
            return;

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        sizes.sort(Comparator.comparing(e -> e.width, Comparator.nullsLast(Comparator.naturalOrder())));
        Camera.Size previewSize = sizes.get(0);
        for (Camera.Size s : sizes) {
            if (s == null) break;
            previewSize = s;
        }
        params.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(params);
        params = mCamera.getParameters();
        mFrameWidth = params.getPreviewSize().width;
        mFrameHeight = params.getPreviewSize().height;

        int size = mFrameWidth * mFrameHeight;
        size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;

        this.mBuffer = new byte[size];
        Log.d(TAG, "Created callback buffer of size (bytes): " + size);

        Mat mFrame = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);
        mCameraFrame = new CameraAccessFrame(mFrame, mFrameWidth, mFrameHeight);

        if (this.texture != null) {
            this.texture.release();
        }
        this.texture = new SurfaceTexture(0);
        try {
            mCamera.setPreviewTexture(texture);
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();

            Log.d(TAG, String.format("Camera preview started with %sx%s. " +
                            "Rendering to SurfaceTexture dummy while receiving preview frames.",
                    mFrameWidth, mFrameHeight));
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public synchronized void onPreviewFrame(byte[] frame, Camera arg1) {
        mCameraFrame.put(frame);
        mListener.onCameraFrame(mCameraFrame);
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }

    @SuppressWarnings("unused")
    public interface CameraFrame extends CameraBridgeViewBase.CvCameraViewFrame {
        Bitmap toBitmap();

        @Override
        Mat rgba();

        @Override
        Mat gray();
    }

    private static class CameraAccessFrame implements CameraFrame {
        private final Mat mYuvFrameData;
        private final Mat mRgba;
        private final int mWidth;
        private final int mHeight;
        private final Bitmap mCachedBitmap;
        private boolean mRgbaConverted;
        private boolean mBitmapConverted;

        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            if (!mRgbaConverted) {
                Imgproc.cvtColor(mYuvFrameData, mRgba,
                        Imgproc.COLOR_YUV2BGR_NV12, 4);
                mRgbaConverted = true;
            }
            return mRgba;
        }

        @Override
        public synchronized Bitmap toBitmap() {
            if (mBitmapConverted) {
                return mCachedBitmap;
            }
            Mat rgba = this.rgba();
            Utils.matToBitmap(rgba, mCachedBitmap);
            mBitmapConverted = true;
            return mCachedBitmap;
        }

        public CameraAccessFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
            this.mCachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        public synchronized void put(byte[] frame) {
            mYuvFrameData.put(0, 0, frame);
            invalidate();
        }

        public void release() {
            mRgba.release();
            mCachedBitmap.recycle();
        }

        public void invalidate() {
            mRgbaConverted = false;
            mBitmapConverted = false;
        }
    }

    private static class CameraHandlerThread extends HandlerThread {
        Handler mHandler;
        HardwareCamera owner;

        CameraHandlerThread(HardwareCamera owner) {
            super("CameraHandlerThread");
            this.owner = owner;
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(() -> {
                owner.oldConnectCamera();
                notifyCameraOpened();
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }

    public void connectCamera() {
        connectLocalCamera();
    }

    public void disconnectCamera() {
        synchronized (this) {
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                try {
                    mCamera.setPreviewTexture(null);
                } catch (IOException e) {
                    Log.e(TAG, "Could not release preview-texture from camera.");
                }
                mCamera.release();
                Log.d(TAG, "Preview stopped and camera released");
            }
            mCamera = null;

            if (mCameraFrame != null) {
                mCameraFrame.release();
            }
            if (texture != null) {
                texture.release();
            }
            mListener.onCameraStopped();
        }
    }

    public boolean isConnected() {
        return mCamera != null;
    }

    public interface CameraListener {

        void onCameraStarted(int width, int height);

        void onCameraStopped();

        void onCameraFrame(CameraFrame inputFrame);
    }
}
