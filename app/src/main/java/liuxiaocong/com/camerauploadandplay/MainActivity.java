package liuxiaocong.com.camerauploadandplay;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;

import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import liuxiaocong.com.camerauploadandplay.recorder.FFmpegFrameRecorder;
import liuxiaocong.com.camerauploadandplay.recorder.Frame;

public class MainActivity extends AppCompatActivity {
    String TAG = "CameraUploadAndPlay";

    @BindView(R.id.upload_wrap)
    LinearLayout mUploadWrap;
    @BindView(R.id.play_wrap)
    LinearLayout mPlayWrap;

    @BindView(R.id.upload)
    Button mUploadBtn;
    @BindView(R.id.play)
    Button mPlayBtn;

    @BindView(R.id.upload_url)
    EditText mEditText;

    @BindView(R.id.camera_texture)
    TextureView mCameraTextureView;

    CameraManager mCameraManager;
    AudioRecorder mAudioRecorder;
    private int mCameIndex = Camera.CameraInfo.CAMERA_FACING_FRONT;
    //audio
    private static final int SAMPLE_RATE = 44100;

    private String mPlayUrl = "http://d14jvptfm9jqfj.cloudfront.net:80/live/730/playlist.m3u8";
    private String mUploadUrl = "rtmp://wowza-loopstest.mozat.com:1935/live/730_2061254_636782_3";

    FFmpegFrameRecorder mRecorder;
    boolean mIsRecording = false;

    private Frame yuvIplImage = null;
    private byte[] mNV21Buffer = null;

    private int mPreviewWidth;
    private int mPreviewHeight;
    private int mDisplayWidth;
    private int mDisplayHeight;

    private boolean mIsInMirror = false;

    private static final VideoQuality VIDEO_QUALITY = new VideoQuality(
            640, 480, 15,
            SAMPLE_RATE,
            460800,// 640*480*15*0.1
            16000,
            "superfast"
    );

    private static final VideoQuality VIDEO_QUALITY_LANDSCAPE = new VideoQuality(
            480, 640, 15,
            SAMPLE_RATE,
            460800,// 480*640*15*0.1
            16000,
            "superfast"
    );

    private int mDegree = 0;

    long mLastRecorderTime = 0;
    int mExpectIntervalTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setKeepScreenStatus(true);
        ButterKnife.bind(this);
        mCameraManager = CameraManager.getInstance();
        mCameraTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mEditText.setText(mUploadUrl);
        mExpectIntervalTime = (int) ((float) 1000 / (float) VIDEO_QUALITY.getFrameRate());

    }

    @OnClick(R.id.preview)
    public void onClickPreview(View view) {
        mCameraManager.openCacmeraAndPreview(this, mCameIndex, VIDEO_QUALITY, mCameraTextureView, mUploadWrap.getWidth(), mUploadWrap.getHeight(), mOpenCameraListener, mPreviewCallback);
    }


    @OnClick(R.id.upload)
    public void onClickUpload(View view) {
        mAudioRecorder = new AudioRecorder();
        mAudioRecorder.startRecorder(mAudioRecorderListener);
        mIsRecording = true;
    }

    private AudioRecorder.AudioRecorderListener mAudioRecorderListener = new AudioRecorder.AudioRecorderListener() {
        @Override
        public void onAudioDataUpdate(ByteBuffer buffer) {
            if (mIsRecording) {
                if (mRecorder != null) {
                    try {
                        mRecorder.recordSamples(buffer.asShortBuffer());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onFail() {

        }
    };

    private FFmpegFrameRecorder genRecoder() {
        if (mDegree == 90 || mDegree == 270) {
            mRecorder = new FFmpegFrameRecorder(
                    mEditText.getText().toString(),
                    mPreviewHeight,
                    mPreviewWidth,
                    1);
        } else {
            mRecorder = new FFmpegFrameRecorder(
                    mEditText.getText().toString(),
                    mPreviewWidth,
                    mPreviewHeight,
                    1);
        }
        mRecorder.setFormat("flv");
        mRecorder.setSampleRate(VIDEO_QUALITY.getSampleAudioRateInHz());
        mRecorder.setFrameRate(VIDEO_QUALITY.getFrameRate());
        mRecorder.setGopSize(VIDEO_QUALITY.getFrameRate());// * GAP_OF_PFRAMES_IN_SECONDS);
        mRecorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        mRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        mRecorder.setAudioBitrate(VIDEO_QUALITY.getAudioBitrate());
        mRecorder.setVideoBitrate(VIDEO_QUALITY.getVideoBitrate());
        mRecorder.setVideoOption("threads", "4");
        mRecorder.setVideoOption("preset", VIDEO_QUALITY.getX264Profile());
        mRecorder.setVideoOption("tune", "zerolatency");
        mRecorder.setVideoOption("crf", "23");//0(loseless)-51(worst), default is 23
        mRecorder.setMetadata("poweredby", "Rings/Android");
        try {
            mRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mRecorder;
    }


    private CameraManager.OpenCameraListener mOpenCameraListener = new CameraManager.OpenCameraListener() {
        @Override
        public void onCallbackCameraInfo(final int realwidth, final int realheight, final int degree, int cindex) {
            if (mCameIndex == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mIsInMirror = true;
            }
            mDegree = degree;
            int previewWidth = realwidth;
            int previewHeight = realheight;
            if (degree == 90 || degree == 270) {
                previewWidth = realheight;
                previewHeight = realwidth;
            }
            yuvIplImage = new Frame(previewWidth, previewHeight * 3 / 2, Frame.DEPTH_UBYTE, 1);
            mNV21Buffer = new byte[previewWidth * previewHeight * 3 / 2];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resetCameraViewSize(mCameraTextureView, realwidth, realheight, degree);
                }
            });
        }
    };


    public void resetCameraViewSize(View targetView, int realWidth, int realHeight, int degree) {
        int currentWidth = targetView.getMeasuredWidth();
        int currentHeight = targetView.getMeasuredHeight();
        mPreviewWidth = realWidth;
        mPreviewHeight = realHeight;
        mDisplayWidth = realWidth;
        mDisplayHeight = realHeight;
        if (degree == 90 || degree == 270) {
            mDisplayWidth = realHeight;
            mDisplayHeight = realWidth;
        }
        float scaleRatioOfWidth = (float) mDisplayWidth / (float) currentWidth;
        float scaleRatioOfHeight = (float) mDisplayHeight / (float) currentHeight;
        float expectRatio = scaleRatioOfWidth < scaleRatioOfHeight ? scaleRatioOfWidth : scaleRatioOfHeight;
        int expectWidth = (int) ((float) mDisplayWidth / expectRatio);
        int expectHeight = (int) ((float) mDisplayHeight / expectRatio);
        int parentWidth = ((View) targetView.getParent()).getMeasuredWidth();
        int parentHeight = ((View) targetView.getParent()).getMeasuredHeight();
        Log.d(TAG, "degree:" + degree);
        Log.d(TAG, "currentWidth:" + currentWidth);
        Log.d(TAG, "currentHeight:" + currentHeight);
        Log.d(TAG, "mDisplayWidth:" + mDisplayWidth);
        Log.d(TAG, "mDisplayHeight:" + mDisplayHeight);
        Log.d(TAG, "scaleRatioOfWidth:" + scaleRatioOfWidth);
        Log.d(TAG, "scaleRatioOfHeight:" + scaleRatioOfHeight);
        Log.d(TAG, "expectRatio:" + expectRatio);
        Log.d(TAG, "expectWidth:" + expectWidth);
        Log.d(TAG, "expectHeight:" + expectHeight);
        Log.d(TAG, "parentWidth:" + parentWidth);
        Log.d(TAG, "parentHeight:" + parentHeight);

        ViewGroup.LayoutParams lp = targetView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            lp.width = expectWidth;
            lp.height = expectHeight;
            int marginTop = (int) ((float) (parentHeight - expectHeight) / 2);
            int marginLeft = (int) ((float) (parentWidth - expectWidth) / 2);
            ((ViewGroup.MarginLayoutParams) lp).setMargins(marginLeft, marginTop, 0, 0);
            targetView.setLayoutParams(lp);
            Log.d(TAG, "marginTop:" + marginTop);
            Log.d(TAG, "marginLeft:" + marginLeft);
        }
    }

    private android.hardware.Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.d(TAG, "onPreviewFrame");
            if (mIsRecording) {
                if (mRecorder == null) {
                    genRecoder();
                }
                long _currentTick = System.nanoTime();
                if ((_currentTick - mLastRecorderTime) / 1000000L > mExpectIntervalTime) {
                    mLastRecorderTime = _currentTick;
                    if (yuvIplImage != null) {
                        try {
                            byte[] mYuv420spBuffer = bytes;
                            if (mDegree == 90) {
                                if (mIsInMirror) {
                                    YuvHelper.rotateNV21Degree270(bytes, mNV21Buffer, mPreviewWidth, mPreviewHeight);
                                    YuvHelper.nv21HorizontalMirrorChange(mNV21Buffer, mYuv420spBuffer, mDisplayWidth, mDisplayHeight);
                                } else {
                                    YuvHelper.rotateNV21Degree90(bytes, mNV21Buffer, mPreviewWidth, mPreviewHeight);
                                    mYuv420spBuffer = mNV21Buffer;
                                }
                            } else if (mDegree == 180) {
                                if (mIsInMirror) {
                                    YuvHelper.rotateNV21Degree180(bytes, mNV21Buffer, mPreviewWidth, mPreviewHeight);
                                    YuvHelper.nv21HorizontalMirrorChange(mNV21Buffer, mYuv420spBuffer, mDisplayWidth, mDisplayHeight);
                                } else {
                                    YuvHelper.rotateNV21Degree180(bytes, mNV21Buffer, mPreviewWidth, mPreviewHeight);
                                    mYuv420spBuffer = mNV21Buffer;
                                }
                            } else if (mDegree == 270) {
                                if (mIsInMirror) {
                                    YuvHelper.rotateNV21Degree90(bytes, mNV21Buffer, mPreviewWidth, mPreviewHeight);
                                    YuvHelper.nv21HorizontalMirrorChange(mNV21Buffer, mYuv420spBuffer, mDisplayWidth, mDisplayHeight);
                                } else {
                                    YuvHelper.rotateNV21Degree270(bytes, mNV21Buffer, mPreviewWidth, mPreviewHeight);
                                    mYuv420spBuffer = mNV21Buffer;
                                }
                            } else {
                                if (mIsInMirror) {
                                    YuvHelper.nv21HorizontalMirrorChange(bytes, mNV21Buffer, mDisplayWidth, mDisplayHeight);
                                    mYuv420spBuffer = mNV21Buffer;
                                } else {
                                    mYuv420spBuffer = bytes;
                                }
                            }
                            yuvIplImage.image[0].position(0).clear();
                            ((ByteBuffer) yuvIplImage.image[0].position(0)).put(mYuv420spBuffer);

                            yuvIplImage.imageWidth = mDisplayWidth;
                            yuvIplImage.imageHeight = mDisplayHeight;
                            Log.d(TAG, "Record video to service");
                            //service
                            mRecorder.record(yuvIplImage, avutil.AV_PIX_FMT_NV21);
                            //mRecorder.record(yuvIplImage, avutil.AV_PIX_FMT_NV21);
                        } catch (Throwable e) {
                            Log.v(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            CameraManager.getInstance().addBuffer(bytes);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraManager != null) {
            mCameraManager.releaseCamera();
        }
        setKeepScreenStatus(false);
    }

    public void setKeepScreenStatus(boolean isOn) {
        if (isOn) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            Log.d(TAG, "onSurfaceTextureAvailable");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
            Log.d(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            //Log.d(TAG,"onSurfaceTextureUpdated");
        }
    };
}
