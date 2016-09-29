package liuxiaocong.com.camerauploadandplay.common;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by LiuXiaocong on 9/28/2016.
 */
public class CameraManager {
    String TAG = "CameraManager";
    private static CameraManager gCameraManager = null;
    private Camera mCameraDevice;
    private CameraThreadHandler mCameraThreadHandler = null;
    private UIThreadHandler mUIThreadHandler = null;
    private static final int MSG_ON_OPEN_AND_PREVIEW = 0x2000;

    public interface OpenCameraListener {
        void onCallbackCameraInfo(int realwidth, int realheight, int degree, int cindex);
    }


    public static CameraManager getInstance() {
        if (gCameraManager == null) {
            gCameraManager = new CameraManager();
        }
        return gCameraManager;
    }

    //call on ui thread
    private CameraManager() {
        HandlerThread handlerThread = new HandlerThread("CameraManager");
        handlerThread.start();
        mCameraThreadHandler = new CameraThreadHandler(handlerThread.getLooper());
        mUIThreadHandler = new UIThreadHandler(Looper.getMainLooper());
    }

    public void releaseCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.setPreviewCallbackWithBuffer(null);
            mCameraDevice.cancelAutoFocus();
            mCameraDevice.release();
            mCameraDevice = null;
        }
    }

    public void openCacmeraAndPreview(Activity activity, int cameraIndex, VideoQuality videoQuality,
                                      TextureView textureView, int parentWidth, int parentHeight,
                                      OpenCameraListener openCameraListener,
                                      android.hardware.Camera.PreviewCallback previewCallback) {
        mCameraThreadHandler.removeMessages(MSG_ON_OPEN_AND_PREVIEW);

        Message message = new Message();
        message.what = MSG_ON_OPEN_AND_PREVIEW;
        message.obj = new Object[]{new WeakReference<>(activity), cameraIndex, videoQuality, new WeakReference<>(textureView), parentWidth, parentHeight,
                new WeakReference<>(openCameraListener), new WeakReference<>(previewCallback)};
        mCameraThreadHandler.sendMessage(message);
    }


    //need call when you use setPreviewCallbackWithBuffer
    public void addBuffer(byte[] data) {
        if (mCameraDevice != null) {
            mCameraDevice.addCallbackBuffer(data);
        }
    }

    public void openCacmeraAndPreviewInCameraThread(Activity activity, int cameraIndex, VideoQuality videoQuality,
                                                    TextureView textureView, int parentWidth, int parentHeight,
                                                    OpenCameraListener openCameraListener,
                                                    android.hardware.Camera.PreviewCallback previewCallback) {
        if (mCameraDevice != null) {
            return;
        } else {
            Camera.CameraInfo cameraInfo = null;
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == cameraIndex) {
                    try {
                        mCameraDevice = Camera.open(i);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            Camera.Parameters camParams = mCameraDevice.getParameters();
            int orientationDegrees = CameraUtil.getCameraDisplayOrientation(activity, cameraInfo);

            int expectWidth = videoQuality.getVideoWidth();
            int expectHeight = videoQuality.getVideoHeight();

            int[] outBestPreviewSize = new int[2];
            getBestSizeFitShorterSide(expectWidth, expectHeight, camParams.getSupportedPreviewSizes(), outBestPreviewSize);
            if (outBestPreviewSize[0] == 0 || outBestPreviewSize[1] == 0) {
                if (expectWidth <= expectHeight) {
                    outBestPreviewSize[0] = 240;
                    outBestPreviewSize[1] = 320;
                } else {
                    outBestPreviewSize[0] = 320;
                    outBestPreviewSize[1] = 240;
                }
            }
            Log.d(TAG, "camParams.setPreviewSize size.width = " + outBestPreviewSize[0] + "; size.height = " + outBestPreviewSize[1]);
            camParams.setPreviewSize(outBestPreviewSize[0], outBestPreviewSize[1]);
            if (openCameraListener != null) {
                openCameraListener.onCallbackCameraInfo(outBestPreviewSize[0], outBestPreviewSize[1], orientationDegrees, cameraIndex);
            }

            int[] outPicSizes = new int[2];
            if (Math.min(expectWidth, expectHeight) > 1080) {
                getBestSizeFitShorterSide(expectWidth, expectHeight, camParams.getSupportedPictureSizes(), outPicSizes);
            } else {
                getBestSizeFitShorterSide(720, 1280, camParams.getSupportedPictureSizes(), outPicSizes);
            }

            if (outPicSizes[0] == 0 || outPicSizes[1] == 0) {
                if (expectWidth <= expectHeight) {
                    outPicSizes[0] = 480;
                    outPicSizes[1] = 640;
                } else {
                    outPicSizes[0] = 640;
                    outPicSizes[1] = 480;
                }
            }
            camParams.setPictureSize(outPicSizes[0], outPicSizes[1]);
            if (Build.MODEL.toLowerCase().equals("sm-c115") && cameraIndex == Camera.CameraInfo.CAMERA_FACING_BACK) {
                List<Camera.Size> ls = camParams.getSupportedPictureSizes();
                if (ls != null)
                    for (Camera.Size l : ls) {
                        if (l.height == 1944 && l.width == 2592) {
                            camParams.setPictureSize(l.width, l.height);
                        }
                    }
            }
            mCameraDevice.setDisplayOrientation(orientationDegrees);
            camParams.setRotation(orientationDegrees);
            if (activity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                camParams.set("orientation", "portrait");
            } else {
                camParams.set("orientation", "landscape");
            }
            List<String> focusModes = camParams.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            List<int[]> supportedPreviewFps = camParams.getSupportedPreviewFpsRange();
            int expect_min_fps = (videoQuality.getFrameRate() - 10) * 1000;
            int expect_max_fps = videoQuality.getFrameRate() * 1000;
            int min_fps = expect_min_fps;
            int max_fps = expect_max_fps;

            int dist_max_fps = Integer.MAX_VALUE;
            for (int i = 0; i < supportedPreviewFps.size(); i++) {
                int[] _sizes = supportedPreviewFps.get(i);
                Log.d(TAG, "supportedPreviewFps: " + _sizes[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " - " + _sizes[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                int _dist_max = Math.abs(_sizes[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] - expect_max_fps);
                if (_dist_max < dist_max_fps) {

                    dist_max_fps = _dist_max;
                    min_fps = _sizes[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
                    max_fps = _sizes[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                }
            }
            Log.d(TAG, "chosen preview fps: " + min_fps + " - " + max_fps);
            camParams.setPreviewFpsRange(min_fps, max_fps);// expect_max_fps);//
            camParams.set("mode", "smart-auto");
            mCameraDevice.setParameters(camParams);
            mCameraDevice.addCallbackBuffer(new byte[outBestPreviewSize[0] * outBestPreviewSize[1] * 3 / 2]);
            mCameraDevice.addCallbackBuffer(new byte[outBestPreviewSize[0] * outBestPreviewSize[1] * 3 / 2]);
            if (Runtime.getRuntime().maxMemory() > 80 * 1000 * 1024) {// 80M
                mCameraDevice.addCallbackBuffer(new byte[outBestPreviewSize[0] * outBestPreviewSize[1] * 3 / 2]);
            }
            mCameraDevice.setPreviewCallbackWithBuffer(previewCallback);
            try {
                mCameraDevice.setPreviewTexture(textureView.getSurfaceTexture());
            } catch (IOException e) {
                e.printStackTrace();
            }
            startPreview();
        }
    }

    private void startPreview() {
        if (mCameraDevice != null) {
            mCameraDevice.startPreview();
        }
    }

    private class CameraThreadHandler extends Handler {

        public CameraThreadHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_OPEN_AND_PREVIEW: {
                    Object[] args = (Object[]) msg.obj;
                    WeakReference<Activity> activityWeakReference = (WeakReference<Activity>) args[0];
                    int cameraIndex = (int) args[1];
                    VideoQuality videoQuality = (VideoQuality) args[2];
                    WeakReference<TextureView> textureViewWeakReference = (WeakReference<TextureView>) args[3];
                    int parentWidth = (int) args[4];
                    int parentHeight = (int) args[5];
                    WeakReference<OpenCameraListener> openCameraListenerWeakReference = (WeakReference<OpenCameraListener>) args[6];
                    WeakReference<android.hardware.Camera.PreviewCallback> previewCallbackWeakReference = (WeakReference<android.hardware.Camera.PreviewCallback>) args[7];

                    if (activityWeakReference.get() == null || textureViewWeakReference.get() == null || openCameraListenerWeakReference.get() == null || previewCallbackWeakReference.get() == null) {
                        return;
                    } else {
                        openCacmeraAndPreviewInCameraThread(activityWeakReference.get(), cameraIndex, videoQuality, textureViewWeakReference.get(), parentWidth, parentHeight, openCameraListenerWeakReference.get(),
                                previewCallbackWeakReference.get());
                    }
                }
                break;
            }
        }
    }

    private class UIThreadHandler extends Handler {

        public UIThreadHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

            }

        }
    }

    private void getBestSizeFitShorterSide(int expect_width, int expect_height, List<Camera.Size> sizes, int[] out) {
        int shorter_side = Math.min(expect_width, expect_height);
        int area_preview = Integer.MAX_VALUE;
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size _size = sizes.get(i);
            Log.d(TAG, "supportedSize: w=" + _size.width + ", h=" + _size.height);
            if (Math.min(_size.width, _size.height) >= shorter_side
                    && _size.width != _size.height
                    && _size.width * _size.height < area_preview) {
                out[0] = _size.width;
                out[1] = _size.height;
                area_preview = out[0] * out[1];
            }
        }
    }
}

