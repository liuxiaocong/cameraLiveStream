package mozat.rings.libffmpeg;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.mozat.moplayer.MoListener;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import liuxiaocong.com.camerauploadandplay.BuildConfig;

/**
 * Created by Ken on 6/7/2015.
 */
public class FFMPEGPlayer extends ViewGroup {

    private final static String LOG_TAG = FFMPEGPlayer.class.getSimpleName();

    private final static boolean DEBUG = false;

    private final static boolean USE_SPEEX_DSP = true;

    public final static int DEFAULT_CHECK_LATENCY_INTERVAL = 10*1000*1000;

    public final static int DEFAULT_MIN_LATENCY_THRESHOLD = 5*1000*1000;

    public final static int DEFAULT_MAX_LATENCY_THRESHOLD = 10*1000*1000;

    public final static int DEFAULT_LATENCY_THRESHOLD_STEP = 500*1000;

    public final static int DEFAULT_KEEP_DURATION_SECONDS = 3;

    public final static int DEFAULT_MIN_AUDIO_BUFFER_MS = 500;// 500ms

    public final static int DEFAULT_MIN_AUDIO_BUFFER_BYTES = 51200;// 50k

    private static final int DEFAULT_PROBE_SIZE = 128*1024;

    public static final int DEFAULT_REPORT_INTERMISSION_THRESHOLD = 400000;

    public final static String LATENCY_OPTIMIZATION_SCHEME = "rtmp://";

    public enum EVIDEO_LAYOUT {
        /**
         * scale the video to fit parent's width, and then center in
         * height by keeping video's aspectratio.
         * There may be black paddings on vertical, and video may be
         * out of viewport on vertical.
         */
        EFIT_WIDTH_CENTER_HEIGHT,
        /**
         * scale the video to fit parent's width, and then top align to
         * parent by keeping video's aspectratio.
         * There may be black below video on vertical, and video may be
         * out of viewport on bottom.
         */
        EFIT_WIDTH_TOP_HEIGHT,
        /**
         * scale the video to fit parent's height, and then center in width
         * by keeping video's aspectratio.
         * There may be black paddings on horizontal, and video may be
         * out of viewport on horizontal.
         */
        EFIT_HEIGHT_CENTER_WIDTH,
        /**
         * scale the video to make content fill both side of parent while
         * keeping video's aspectratio, never leave a black padding, but
         * video may be out of viewport on both directions (even at
         * the same time).
         * To fit width or to fit height, depends on both video's aspectratio
         * and parent's.
         */
        EFILL_PARENT_KEEP_VIDEO_ASPECTRATIO,
        /**
         * just fill parent, no matter what the video aspectratio is.
         * Generally it's using on playing 360 degree videos.
         */
        EMATCH_PARENT
    }

    public enum ERENDER_MODE {

        ENORMAL,

        ENORMAL_TEXTURE_VIEW,

        ESTITCHED_SPHERICAL,

        ESINGLE_FISH_EYE,

        ESINGLE_FISH_EYE_UPSIDE_DOWN,

    }

    /**
     * the media controller
     */
    public interface IFFMPEGMediaController {

        void onPlayPositionChanged(float p);

        void onPlayTimeChanged(long t);

        void onStateChanged(int st);

        boolean isShowing();

        void hide();

        void show();

        void setPlayerView(FFMPEGPlayer player);
    }

    public interface FFMPEGPlayerListener {
        /**
         * Called when the end of a media source is reached during playback.
         * Note that, it will be followed by a onStateChanged with STATE_IDLE.
         */
        void onCompletion();

        /**
         * Called to update status in buffering a media stream.
         *
         * @param percent the percentage (0-1.0) of the buffer that has been filled thus
         *                far
         */
        void onBufferingUpdate(float percent);

        void onStateChanged(int st);

        void onVideoSizeChanged(int width, int height);

        void onClicked();

        /**
         * the player reports that it's been resume from intermission.
         * @param startAPts is the latest successfully decoded audio frame's pts before intermission
         * @param startVpts is the latest successfully decoded video frame's pts before intermission
         * @param durationMS is the intermission duration in milliseconds
         * @param iccMS is how many milliseconds of packet-cache has been cleared in one second before
         *            intermission. zero means no cache has been cleared or not been cleared
         *            in one second before intermission.
         */
        void onReportIntermission(long startAPts, long startVpts, int durationMS, int iccMS);

        /**
         * the player reports that it fails to read a packet from network and the buffer is almost empty.
         * For both live and replay
         */
        void onReportStalled();

        /**
         * we just did the latency optimization.
         * @param cl is how many milliseconds of packet has been cleared from cache.
         * @param apts is the latest successfully decoded audio frame's pts (before clearing).
         */
        void onDidLatencyOptimization(int cl, long apts);
    }


    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_STOPPING = 5;

    /**
     * arg1=>width, arg2=>height, argObj->rotation
     */
    private static final int MSG_ON_VIDEO_SIZE_CHANGED = 0x8080;
    /**
     * argObj=> { candidate urls, renderMode, overrideDuration, seekTo, streamType, channels, sampleRate, reportIntermissionThreshold }
     */
    private static final int MSG_PLAY = MSG_ON_VIDEO_SIZE_CHANGED + 1;
    private static final int MSG_PAUSE = MSG_PLAY + 1;
    /**
     * arg1=>restartLater
     */
    private static final int MSG_STOP = MSG_PAUSE + 1;
    private static final int MSG_RESUME = MSG_STOP + 1;
    /**
     * argObj=>vol
     */
    private static final int MSG_SET_VOLUME = MSG_RESUME + 1;
    /**
     * argObj=>surface
     */
    private static final int MSG_ATTACH_SURFACE = MSG_SET_VOLUME + 1;
    private static final int MSG_DETACH_SURFACE = MSG_ATTACH_SURFACE + 1;
    /**
     * argObj=>position
     */
    private static final int MSG_SEEK_TO = MSG_DETACH_SURFACE + 1;
    /**
     * argObj=>perfentage:float
     */
    private static final int MSG_ON_BUFFERING = MSG_SEEK_TO + 1;
    private static final int MSG_ON_PLAYING = MSG_ON_BUFFERING + 1;
    private static final int MSG_ON_STOPPED = MSG_ON_PLAYING + 1;
    private static final int MSG_ON_PAUSED = MSG_ON_STOPPED + 1;
    private static final int MSG_ON_RESUMED = MSG_ON_PAUSED + 1;
    private static final int MSG_ON_ERROR = MSG_ON_RESUMED + 1;
    private static final int MSG_ON_ENDED = MSG_ON_ERROR + 1;
    /**
     * arg1=>{ 0: let go, 1: suspend }
     */
    private static final int MSG_SUSPEND_STOP_EVENT = MSG_ON_ENDED + 1;
    /**
     * arg1=>{ 0: no, 1: loop }
     */
    private static final int MSG_SET_LOOP = MSG_SUSPEND_STOP_EVENT + 1;
    /**
     * argObj=>{ interval: int, threshold_min: int, threshold_max: int, threshold_step: int }
     */
    private static final int MSG_SET_LATENCY_OPT = MSG_SET_LOOP + 1;
    /**
     * argObj=>{ startAPts : long, startVPts : long, durationMs : int, iccMs : int }
     */
    private static final int MSG_ON_REPORT_INTERMISSION = MSG_SET_LATENCY_OPT + 1;
    /**
     * argObj=>{ cl : int, apts : long }
     */
    private static final int MSG_ON_DID_LATENCY_OPTIMIZATION = MSG_ON_REPORT_INTERMISSION + 1;
    /**
     * no args
     */
    private static final int MSG_ON_REPORT_STALLED = MSG_ON_DID_LATENCY_OPTIMIZATION + 1;


    private ERENDER_MODE mRenderMode = ERENDER_MODE.ENORMAL_TEXTURE_VIEW;
    private EVIDEO_LAYOUT mVideoLayout = EVIDEO_LAYOUT.EFIT_WIDTH_CENTER_HEIGHT;
    private SurfaceHolderCallback mSFCallback = null;
    private SurfaceTextureCallback mSTCallback = null;
    private GlRenderer mGLRenderer = null;
    private int mFieldOfView = 240;

    private PlayerControlThread mPlayer;
    //    private PowerManager.WakeLock mWakeLock = null;
    private int mCurrentState = STATE_IDLE;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private int mRotation = 0;

    private Handler mHandler = new MyHandler(this);

    private String[] mCandidateUrls = null;
    private int mCurrentUrlIdx = -1;
    private int mLastStreamType = AudioManager.STREAM_MUSIC;
    private int mLastReportIntermissionThreshold = 0;

    private FFMPEGPlayerListener mPlayerListener = null;
    private FFMPEGMediaController mMediaController = null;

    private AcousticEchoCanceler mBuildinAEC = null;
    private NoiseSuppressor mBuildinNS = null;

    private long mPlayStartTime = 0L;
//    private String mLastPlayMRL = "";

    private long mOverrideDuration = 0L;

    private boolean mSuspendStopEvent = false;

    private float mTouch_slop = 8.0f;

    public FFMPEGPlayer(Context context) {
        this(context, null);
    }

    public FFMPEGPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FFMPEGPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouch_slop = ViewConfigurationCompat.getScaledPagingTouchSlop(vc);
        mVideoWidth = 0;
        mVideoHeight = 0;
        mCurrentState = STATE_IDLE;

        mPlayer = new PlayerControlThread(context, mMoPlayerListener);
        mPlayer.start();

        mPlayer.setMinimalAudioBufferD(DEFAULT_MIN_AUDIO_BUFFER_MS, DEFAULT_MIN_AUDIO_BUFFER_BYTES);

//        getHolder().setFormat(PixelFormat.RGB_565); // PixelFormat.RGB_565 // FIXME : pixel format?
//        getHolder().addCallback(mSHCallback);
//        setFocusable(true);
//        setFocusableInTouchMode(true);
//        requestFocus();

        if (context instanceof Activity) {
            ((Activity) context).setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    /**
     * sets the render mode. NOTE that if the mode is the same as
     * exist one, the renderer and view will NOT re-create.
     *
     * @param mode
     */
    public void setRenderMode(ERENDER_MODE mode) {
        if (mRenderMode != mode) {
            uninstallRenderView(false);
            mRenderMode = mode;
            installRenderView();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        FFMPEGMediaController control = null;
        int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            View v = getChildAt(i);
            if (v instanceof FFMPEGMediaController) {
                control = (FFMPEGMediaController) v;
            }
        }
        if (control != null) {
            setMediaController(control);
        }

        installRenderView();
    }

    private void installRenderView() {
        if (mRenderMode == ERENDER_MODE.ESTITCHED_SPHERICAL) {
            GLSurfaceView gv = new GLSurfaceView(getContext());
            gv.setEGLConfigChooser(5, 6, 5, 0, 16, 0);
            gv.getHolder().setFormat(PixelFormat.RGB_565);
            mGLRenderer = GlRenderer.createSphereRenderer(getContext());
            mGLRenderer.setGlRendererCallback(new GlSurfaceCallback(this));
            mGLRenderer.setSensorEnable(false);
            gv.setRenderer(mGLRenderer);
            gv.setOnTouchListener(mOnTouchListener);

            addView(gv, 0);
        } else if (mRenderMode == ERENDER_MODE.ESINGLE_FISH_EYE) {
            GLSurfaceView gv = new GLSurfaceView(getContext());
            gv.setEGLConfigChooser(5, 6, 5, 0, 16, 0);
            gv.getHolder().setFormat(PixelFormat.RGB_565);
            mGLRenderer = GlRenderer.createSingleFisheyeRenderer(getContext(), false, mFieldOfView);
            mGLRenderer.setGlRendererCallback(new GlSurfaceCallback(this));
            gv.setRenderer(mGLRenderer);
            gv.setOnTouchListener(mOnTouchListener);

            addView(gv, 0);
        } else if (mRenderMode == ERENDER_MODE.ESINGLE_FISH_EYE_UPSIDE_DOWN) {
            GLSurfaceView gv = new GLSurfaceView(getContext());
            gv.setEGLConfigChooser(5, 6, 5, 0, 16, 0);
            gv.getHolder().setFormat(PixelFormat.RGB_565);
            mGLRenderer = GlRenderer.createSingleFisheyeRenderer(getContext(), true, mFieldOfView);
            mGLRenderer.setGlRendererCallback(new GlSurfaceCallback(this));
            gv.setRenderer(mGLRenderer);
            gv.setOnTouchListener(mOnTouchListener);

            addView(gv, 0);
        } else if (mRenderMode == ERENDER_MODE.ENORMAL_TEXTURE_VIEW) {
            mSTCallback = new SurfaceTextureCallback(this);

            TextureView tv = new TextureView(getContext());
            tv.setSurfaceTextureListener(mSTCallback);
            tv.setOnTouchListener(mOnTouchListener);

            addView(tv, 0);
        } else {// if (mRenderMode==ERENDER_MODE.ENORMAL) {
            // surfaceView
            mSFCallback = new SurfaceHolderCallback(this);

            SurfaceView sv = new SurfaceView(getContext());
            sv.getHolder().setFormat(PixelFormat.RGB_565); // PixelFormat.RGB_565 // FIXME : pixel format?
            sv.getHolder().addCallback(mSFCallback);

            sv.setOnTouchListener(mOnTouchListener);

            addView(sv, 0);
        }
    }

    private void uninstallRenderView(boolean isdestroy) {
        int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            View v = getChildAt(i);
            if (v instanceof GLSurfaceView) {
                detachSurface();
                removeViewAt(i);
                i--;

                v.setOnTouchListener(null);
                if (mGLRenderer != null) {
                    mGLRenderer.destroy();
                    mGLRenderer = null;
                }
            } else if (v instanceof SurfaceView) {
                if (!isdestroy && mSFCallback != null) {
                    // to prevent a redundant surface detaching
                    mSFCallback.destroy();
                    mSFCallback = null;
                }
                detachSurface();
                removeViewAt(i);
                i--;

                v.setOnTouchListener(null);
                SurfaceView sv = (SurfaceView) v;
                if (mSFCallback != null) {
                    mSFCallback.destroy();
                    sv.getHolder().removeCallback(mSFCallback);
                    mSFCallback = null;
                }
            } else if (v instanceof TextureView) {
                if (!isdestroy && mSTCallback != null) {
                    // to prevent a redundant surface detaching
                    mSTCallback.destroy();
                    mSTCallback = null;
                }
                detachSurface();
                removeViewAt(i);
                i--;

                v.setOnTouchListener(null);
                TextureView tv = ((TextureView) v);
                tv.setSurfaceTextureListener(null);
                SurfaceTexture st = tv.getSurfaceTexture();
                if (st != null) {
                    st.release();
                }
                if (mSTCallback != null) {
                    mSTCallback.destroy();
                    mSTCallback = null;
                }
            }
        }
    }

    public void destroy() {
//        getHolder().removeCallback(mSHCallback);
        uninstallRenderView(true);
        mPlayerListener = null;
        closeAcousticEchoCancelation();

        if (mMediaController != null) {
            mMediaController.destroy();
            mMediaController = null;
        }
        if (mPlayer != null && mPlayer.isAlive()) {
            mPlayer.exit();
            mPlayer = null;
        }
        setKeepScreenOn(false);
    }

    public void onActivityStart() {
        // do nothing
    }

    protected final void onStateChanged(int st) {
        mCurrentState = st;
        if (mMediaController != null) {
            mMediaController.onStateChanged(mCurrentState);
        }
        if (mPlayerListener != null) {
            mPlayerListener.onStateChanged(mCurrentState);
        }
    }


    public void setPlayerListener(FFMPEGPlayerListener l) {
        mPlayerListener = l;
    }

    public void setMediaControllerVisible(boolean b) {
        if (mMediaController != null) {
            if (b && isSeekable()) {
                mMediaController.show();
            } else {
                mMediaController.hide();
            }
        }
    }

    public void togglePlayAndPause() {
        if (mCurrentState == STATE_PAUSED) {
            resumeA();
        } else if (mCurrentState == STATE_PLAYING) {
            pauseA();
        }
    }

    private void setMediaController(FFMPEGMediaController c) {
        mMediaController = c;
        attachMediaController();
    }

    public void setmMediaControllerRightMargin(int marginR) {
        if (mMediaController != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mMediaController.getLayoutParams();
            params.rightMargin = marginR;
            mMediaController.setLayoutParams(params);
        }
    }

    private void attachMediaController() {
        if (mMediaController != null) {
            mMediaController.setPlayerView(this);

//            if (mUri != null) {
//                List<String> paths = mUri.getPathSegments();
//                String name = paths == null || paths.isEmpty() ? "null" : paths.get(paths.size() - 1);
//                mMediaController.setFileName(name);
//            }
        }
    }

    private void onBuffering(float percentage) {
        if (mPlayerListener != null) {
            mPlayerListener.onBufferingUpdate(percentage);
        }
    }

    private void onError() {
        if (mCandidateUrls != null && mCurrentUrlIdx > -1 && mCurrentUrlIdx < mCandidateUrls.length - 1 && mPlayer != null) {

            mCurrentUrlIdx++;

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "====== onError, next play candidate : " + mCurrentUrlIdx + " : " + mCandidateUrls[mCurrentUrlIdx]);
            } else {
                Log.d(LOG_TAG, "====== onError, next play candidate : " + mCurrentUrlIdx);
            }

            mPlayer.play(mCandidateUrls[mCurrentUrlIdx], mPlayer.getCurrentTime(), mLastStreamType, 0, 0, DEFAULT_PROBE_SIZE, mLastReportIntermissionThreshold);
            if (mCandidateUrls[mCurrentUrlIdx]!=null
                    && mCandidateUrls[mCurrentUrlIdx].startsWith(LATENCY_OPTIMIZATION_SCHEME)) {
                doSetLatencyOptimization(DEFAULT_CHECK_LATENCY_INTERVAL,
                        DEFAULT_MIN_LATENCY_THRESHOLD,
                        DEFAULT_MAX_LATENCY_THRESHOLD,
                        DEFAULT_LATENCY_THRESHOLD_STEP);
            } else {
                doSetLatencyOptimization(0,0,0,0);
            }
        } else {
            mPlayStartTime = 0L;
            onStateChanged(STATE_ERROR);
        }
    }

    private void onVideoSizeChanged(int w, int h, int rotation) {
//        Log.d(LOG_TAG, String.format("onVideoSizeChanged: (%dx%d)", w, h));
        mRotation = rotation;
        mVideoWidth = w;
        mVideoHeight = h;

        if (mPlayerListener != null) {
            if ((mRotation/90)%2==1) {
                mPlayerListener.onVideoSizeChanged(h, w);
            } else {
                mPlayerListener.onVideoSizeChanged(w, h);
            }
        }
//        SurfaceHolder holder = getHolder();
//        if (holder!=null) {
//            holder.setFixedSize(w, h);
//        }
    }

    public void setVolumeA(float volume) {
        mHandler.obtainMessage(MSG_SET_VOLUME, 0, 0, volume).sendToTarget();
    }

    private void doSetVolume(float vol) {
        if (mPlayer != null) {
            mPlayer.setVolume(vol);
        }
    }

    /**
     * sets the latency optimization parameters.
     * @param interval is the checking interval, in microseconds unit,
     * stop checking if interval less than 1 second.
     * @param threshold_min indicates how many latency we accept at least (if
     * checking ongoing), in microseconds unit.
     * @param threshold_max is the max threshold when we increasing by @param threashold_step .
     * Note that, after calling this method, we ALWAYS set threshold from the @param threshold_min .
     * Default parameter values see:
     * {@link #DEFAULT_CHECK_LATENCY_INTERVAL},
     * {@link #DEFAULT_MIN_LATENCY_THRESHOLD},
     * {@link #DEFAULT_MAX_LATENCY_THRESHOLD},
     * {@link #DEFAULT_LATENCY_THRESHOLD_STEP}
     */
    public void setLatencyOptimizationA(int interval, int threshold_min, int threshold_max, int threshold_step) {
        mHandler.obtainMessage(MSG_SET_LATENCY_OPT, new int[]{
                interval,
                threshold_min,
                threshold_max,
                threshold_step
        }).sendToTarget();
    }

    private void doSetLatencyOptimization(int interval, int threshold_min, int threshold_max, int threshold_step) {
        if (mPlayer!=null) {
            mPlayer.setLatencyOptimizationD(interval, threshold_min, threshold_max, threshold_step, DEFAULT_KEEP_DURATION_SECONDS);        }
    }

    public void playA(String[] mrls, ERENDER_MODE renderMode, long overrideDuration, long seekTo, int streamType, int channels, int sampleRate, int reportIntermissionThreshold) {
//        if (mLastPlayMRL!=null && mLastPlayMRL.equals(mrls[0])) {
//            // has submitted, ignore
//            return;
//        }
//        mLastPlayMRL = mrls[0];
        if (mMediaController != null) {
            mMediaController.resetPlayButton();
        }
        mPlayStartTime = System.currentTimeMillis();
        mHandler.obtainMessage(MSG_PLAY, new Object[]{
                mrls,
                renderMode,
                overrideDuration,
                seekTo,
                streamType,
                channels,
                sampleRate,
                reportIntermissionThreshold
                }
        ).sendToTarget();
    }

    private void doPlay(String[] mrl, ERENDER_MODE renderMode, long overrideDuration, long seekTo, int streamType, int channels, int sampleRate, int reportIntermissionThreshold) {
        if (mPlayer == null || mrl == null || mrl.length == 0) {
            return;
        }

        if (mCurrentState == STATE_PLAYING
                || mCurrentState == STATE_PREPARING
                || mCurrentState == STATE_PREPARED
                || mCurrentState == STATE_PAUSED
                || mPlayer.isPlaying()) {
            if (getHash(mCandidateUrls) == getHash(mrl)) {
                // the same
                Log.d(LOG_TAG, "====== same candidates on playing, ignore ======");
                // UI may have pre-loading
                onStateChanged(mCurrentState);
//                onVideoSizeChanged(mVideoWidth, mVideoHeight);
                // mind the rotation, do not call onVideoSizeChanged directly
                if (mPlayerListener != null) {
                    if (((mRotation)/90)%2==1) {
                        mPlayerListener.onVideoSizeChanged(mVideoHeight, mVideoWidth);
                    } else {
                        mPlayerListener.onVideoSizeChanged(mVideoWidth, mVideoHeight);
                    }
                }
                return;
            }

            mCandidateUrls = mrl;
            mCurrentUrlIdx = 0;
            Log.d(LOG_TAG, "====== playing, we stop first ======");
            mHandler.obtainMessage(MSG_SUSPEND_STOP_EVENT, 1, 0).sendToTarget();
            mPlayer.stopPlaying();
        } else {
            // not playing
            mCandidateUrls = mrl;
            mCurrentUrlIdx = 0;
        }
        mOverrideDuration = overrideDuration * 1000L;
        mLastStreamType = streamType;
        mLastReportIntermissionThreshold = reportIntermissionThreshold;

        setRenderMode(renderMode);

        setKeepScreenOn(true);
        onStateChanged(STATE_PREPARING);
        mPlayer.play(mCandidateUrls[mCurrentUrlIdx], seekTo, streamType, channels, sampleRate, DEFAULT_PROBE_SIZE, reportIntermissionThreshold);
        if (mCandidateUrls[mCurrentUrlIdx]!=null
                && mCandidateUrls[mCurrentUrlIdx].startsWith(LATENCY_OPTIMIZATION_SCHEME)) {
            doSetLatencyOptimization(DEFAULT_CHECK_LATENCY_INTERVAL,
                    DEFAULT_MIN_LATENCY_THRESHOLD,
                    DEFAULT_MAX_LATENCY_THRESHOLD,
                    DEFAULT_LATENCY_THRESHOLD_STEP);
        } else {
            doSetLatencyOptimization(0,0,0,0);
        }
    }

    /**
     * set loop playing or not. Taking effect after calling {@link #playA(String[], ERENDER_MODE, long, long, int, int, int, int)}  }
     * @param loop
     */
    public void setLoopA(boolean loop) {
        mHandler.obtainMessage(MSG_SET_LOOP, loop? 1 : 0, 0).sendToTarget();
    }

    private void doSetLoop(boolean loop) {
        mPlayer.setLoop(loop);
    }

    private final static int getHash(String[] strArr) {
        int hash = 0;
        if (strArr != null && strArr.length > 0) {
            for (int i = 0; i < strArr.length; i++) {
                hash += strArr[i].hashCode();
            }
        }
        return hash;
    }

    /**
     * check if the android build-in aec is available
     * @return
     */
    public static boolean isBuildinAECAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && AcousticEchoCanceler.isAvailable();
    }

    /**
     * open the aec module
     * @param sample_rate
     * @param frame_size
     * @param filter_length
     * @param record_buff_size
     * @return -1 if failed or no aec module is available.
     *      0 if the build-in aec module is opened, and the stream_type is set to STREAM_VOICE_CALL.
     *      1 if speex aec module is opened, stream_type not changed.
     */
    public int openAcousticEchoCancelation(int audioRecordSessionId, int sample_rate, int frame_size, int filter_length, int record_buff_size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && AcousticEchoCanceler.isAvailable()) {
            // enable build-in aec, set stream type to voice_call

            if (mBuildinAEC!=null) {
                mBuildinAEC.setEnabled(false);
                mBuildinAEC.release();
            }
            mBuildinAEC = AcousticEchoCanceler.create(audioRecordSessionId);
            int ret = mBuildinAEC.setEnabled(true);
            Log.i(LOG_TAG, "====== acousticEchoCanceler setEnabled(true): " + ret);

            if (NoiseSuppressor.isAvailable()) {
                if (mBuildinNS != null) {
                    mBuildinNS.setEnabled(false);
                    mBuildinNS.release();
                }
                mBuildinNS = NoiseSuppressor.create(audioRecordSessionId);
                ret = mBuildinNS.setEnabled(true);
                Log.i(LOG_TAG, "====== noiseSuppressor setEnabled(true): " + ret);
            } else {
                Log.i(LOG_TAG, "====== NoiseSuppressor is not available");
            }

            // we set stream type in case of forgetting:)
            setStreamType(AudioManager.STREAM_VOICE_CALL);

            return 0;
        } else if (USE_SPEEX_DSP) {
            mPlayer.speexAECOpenD(sample_rate, frame_size, filter_length, record_buff_size);
            Log.i(LOG_TAG, "====== Acoustic Echo Cancelation is enabled 1 ======");
            return 1;
        }

        Log.i(LOG_TAG, "====== Acoustic Echo Cancelation is not available ======");
        return -1;
    }

    /**
     * close the acoustic echo cancelation if any opened. Note that the stream type
     * will NOT change here.
     */
    public void closeAcousticEchoCancelation() {
        if (isBuildinAECAvailable()) {
            if (mBuildinAEC!=null) {
                mBuildinAEC.setEnabled(false);
                mBuildinAEC.release();
                mBuildinAEC = null;
            }

            if (mBuildinNS != null
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && NoiseSuppressor.isAvailable() ) {
                mBuildinNS.setEnabled(false);
                mBuildinNS.release();
                mBuildinNS = null;
            }
        } else {
            if (mPlayer != null)
                mPlayer.speexAECDestroyD();
            Log.i(LOG_TAG, "====== Speex Acoustic Echo Cancelation is disabled 1 ======");
        }
    }

    /**
     * reset the speex aec module. It does nothing if
     * {@link #isBuildinAECAvailable()} returns true.
     */
    public void speexAECReset() {
        if (!isBuildinAECAvailable()) {
            mPlayer.speexAECReset();
        }
    }

    /**
     * send captured data to speex aec module. It does nothing if
     * {@link #isBuildinAECAvailable()} returns true.
     * @param input
     * @param size
     * @param output
     */
    public void speexAECCapture(ByteBuffer input, int size, ByteBuffer output) {
        if (BuildConfig.DEBUG) {
            if (isBuildinAECAvailable()) {
                throw new UnsupportedOperationException(
                        "the build-in aec module is available, should not use speex");
            }
        }
        mPlayer.speexAECCaptureD(input, size, output);
    }

    public void getPacketQueueInfoD(long[] longArr) {
        mPlayer.getPacketQueueInfoD(longArr);
    }

    /**
     * sets the stream type of the player. if we are already using the specific stream type,
     * nothing will happen.
     */
    public void setStreamType(int streamType) {
        mLastStreamType = streamType;
        mPlayer.setStreamType(streamType, 0, 0);
    }

    public void stopA() {
        mHandler.obtainMessage(MSG_STOP).sendToTarget();
    }

    private void doStop() {
        mCandidateUrls = null;
        mCurrentUrlIdx = 0;
        if (mPlayer != null) {
            mPlayer.stopPlaying();
        }
        setKeepScreenOn(false);
    }

    public void pauseA() {
        mHandler.obtainMessage(MSG_PAUSE).sendToTarget();
    }

    private void doPause() {
        if (mPlayer != null) {
            mPlayer.pausePlaying();
        }
    }

    public void resumeA() {
        mHandler.obtainMessage(MSG_RESUME).sendToTarget();
    }

    private void doResume() {
        if (mPlayer != null) {
            if (mMediaController != null && mMediaController.isUserStop()) {
                return;
            } else {
                mPlayer.resumePlaying();
            }
        }
    }

    public void attachSurface(Surface sh) {
//        if (sh==null) {
//            sh = getHolder();
//        }
        mHandler.obtainMessage(MSG_ATTACH_SURFACE, sh).sendToTarget();
    }

    private void doAttachSurface(Surface sf) {
        if (mPlayer != null) {
            mPlayer.attachSurface(sf);
        }
    }

    public void detachSurface() {
        mHandler.obtainMessage(MSG_DETACH_SURFACE).sendToTarget();
    }

    private void doDetachSurface() {
        if (mPlayer != null) {
            mPlayer.detachSurface();
        }
    }

//    public void setVolume() {
//        mHandler.obtainMessage(MSG_SET_VOLUME, )
//    }

    public long getDuration() {
        if (mPlayer == null) {
            return 0L;
        } else if (mOverrideDuration > 0L) {
            return mOverrideDuration;
        } else {
            return mPlayer.getDuration();
        }
    }

    public long getCurrentTime() {
        if (mPlayer == null) {
            return 0L;
        } else {
            return mPlayer.getCurrentTime();
        }
    }

    public boolean isSeekable() {
        return mPlayer != null && mPlayer.isSeekable();
    }

    public void seekToA(long position) {
        mHandler.obtainMessage(MSG_SEEK_TO, position).sendToTarget();
    }

    private void doSeekTo(long position) {
        if (mPlayer != null) {
            mPlayer.setPosition(position);
        }
    }

    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }


    public void setVideoLayout(EVIDEO_LAYOUT layout, boolean fullScreenMode) {
        mVideoLayout = layout;


        //
        // setup video layout
        //
        if (getContext() == null || mVideoWidth <= 0 || mVideoHeight <= 0) {
            return;
        }

        float videoAspectRatio = (float) mVideoWidth / (float) mVideoHeight;

        int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            View v = getChildAt(i);
            if (v instanceof GLSurfaceView) {
                // the 360 videos always do match parent
                FFMPEGPlayerLayoutParams lp = (FFMPEGPlayerLayoutParams) v.getLayoutParams();
                lp.mVideoAspectRatio = videoAspectRatio;
                lp.mVideoRotation = mRotation;
                lp.mFullScreenMode = false;
                lp.mLayout = EVIDEO_LAYOUT.EMATCH_PARENT;
                v.setLayoutParams(lp);
            } else if ((v instanceof SurfaceView) || (v instanceof TextureView)) {
                FFMPEGPlayerLayoutParams lp = (FFMPEGPlayerLayoutParams) v.getLayoutParams();
                lp.mVideoAspectRatio = videoAspectRatio;
                lp.mVideoRotation = mRotation;
                lp.mLayout = layout;
                lp.mFullScreenMode = fullScreenMode;
                v.setLayoutParams(lp);
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null && p instanceof FFMPEGPlayerLayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new FFMPEGPlayerLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        /*
        VLCPlayerLayoutParams rp = new VLCPlayerLayoutParams(p.width, p.height);
        return rp;*/
        return new FFMPEGPlayerLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FFMPEGPlayerLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int cnt = getChildCount();
//        int rw, rh;
//        if (cnt==0) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            return;
//        } else {
//            rw = getSuggestedMinimumWidth();
//            rh = getSuggestedMinimumHeight();
//        }
//
//        int pw = MeasureSpec.getSize(widthMeasureSpec);
//        for (int i=0;i<cnt;i++) {
//            View child = getChildAt(i);
//            VLCPlayerLayoutParams vlp = (VLCPlayerLayoutParams) child.getLayoutParams();
//            switch (vlp.mLayout) {
//                case EFILL_PARENT_KEEP_VIDEO_ASPECTRATIO: {
//                    float parentAspectRatio = parentWidth / parentHeight;
//                    if (videoAspectRatio < parentAspectRatio) {// narrow video
//                        lp.width = (int)parentWidth;
//                        lp.height = (int)(parentWidth / videoAspectRatio);
//                        lp.gravity = Gravity.CENTER_VERTICAL;
//                    } else {
//                        lp.height = (int) parentHeight;
//                        lp.width = (int) (parentHeight * videoAspectRatio);
//                        lp.gravity = Gravity.CENTER_HORIZONTAL;
//                    }
//                }
//                break;
//                case EFIT_HEIGHT_CENTER_WIDTH: {
//                    lp.height = (int) parentHeight;
//                    lp.width = (int) (parentHeight * videoAspectRatio);
//                    lp.gravity = Gravity.CENTER_HORIZONTAL;
//                }
//                break;
//                case EFIT_WIDTH_CENTER_HEIGHT: {
//                    lp.width = (int)parentWidth;
//                    lp.height = (int)(parentWidth / videoAspectRatio);
//                    lp.gravity = Gravity.CENTER_VERTICAL;
//                }
//                break;
//                case EFIT_WIDTH_TOP_HEIGHT: {
//                    lp.width = (int)parentWidth;
//                    lp.height = (int)(parentWidth / videoAspectRatio);
//                    lp.gravity = Gravity.TOP;
//                }
//                break;
//            }
//        }
//
//        setMeasuredDimension(rw, rh);

        int cnt = getChildCount();
        int myWidth = MeasureSpec.getSize(widthMeasureSpec);
        int myHeight = MeasureSpec.getSize(heightMeasureSpec);
        for (int i = 0; i < cnt; i++) {
            View child = getChildAt(i);
            MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
            if (child instanceof FFMPEGMediaController) {
                int cWidth = myWidth - getPaddingLeft() - getPaddingRight() - params.leftMargin - params.rightMargin;
                int cHeight = myHeight - params.topMargin - params.bottomMargin;
                if (cWidth < getSuggestedMinimumWidth()) {
                    cWidth = getSuggestedMinimumWidth();
                }
                if (cHeight < getSuggestedMinimumHeight()) {
                    cHeight = getSuggestedMinimumHeight();
                }
                child.measure(
                        MeasureSpec.makeMeasureSpec(cWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(cHeight, MeasureSpec.AT_MOST)
                );
            } else {
                child.measure(
                        MeasureSpec.makeMeasureSpec(myWidth - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(myHeight - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY)
                );
            }
        }
        setMeasuredDimension(
                myWidth,
                myHeight
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed && !isLayoutRequested()) {
            return;
        }

        int cnt = getChildCount();

        float scrWidth;
        float scrHeight;
        View contentView = getRootView();// findViewById(Window.ID_ANDROID_CONTENT);
        if (contentView == null) {
            Pair<Integer, Integer> res = ScreenResolution.getResolution(getContext());
            scrWidth = res.first.intValue();
            scrHeight = res.second.intValue();
        } else {
            scrWidth = contentView.getWidth();
            scrHeight = contentView.getHeight();
        }

        for (int i = 0; i < cnt; i++) {
            View child = getChildAt(i);
            if (child instanceof FFMPEGMediaController) {
                int controllerWidth = child.getMeasuredWidth() < (r - l) ? child.getMeasuredWidth() : (r - l);
                child.layout(getPaddingLeft(), b - t - getPaddingBottom() - child.getMeasuredHeight(), controllerWidth, b - t - getPaddingBottom());
                Log.d("PLAYER_CONTROL", "====== media control layout (" +
                        (changed ? "changed: " : "not_changed: ") +
                        0 + "," +
                        Integer.toString(b - t - getPaddingBottom() - child.getMeasuredHeight()) + "," +
                        Integer.toString(r - l) + "," +
                        Integer.toString(b - t - getPaddingBottom()) + ") ======");
            } else if (changed || child.isLayoutRequested()) {
                Log.d("PLAYER_CONTROL", "relayout player");
                FFMPEGPlayerLayoutParams vlp = (FFMPEGPlayerLayoutParams) child.getLayoutParams();
                float maxWidth;
                float maxHeight;
                if (vlp.mFullScreenMode) {
                    maxWidth = scrWidth;
                    maxHeight = scrHeight;
                } else {
                    maxWidth = r - l;
                    maxHeight = b - t;
                }
                switch (vlp.mLayout) {
                    case EFILL_PARENT_KEEP_VIDEO_ASPECTRATIO: {
                        // FIXME : video rotation
                        float parentAspectRatio = maxWidth / maxHeight;
                        if (vlp.mVideoAspectRatio < parentAspectRatio) {// narrow video
                            vlp.width = (int) maxWidth;
                            vlp.height = (int) (maxWidth / vlp.mVideoAspectRatio);
                            // vlp.gravity = Gravity.CENTER_VERTICAL;
                            int _childTop = (int) ((maxHeight - vlp.height) / 2);
                            child.layout(
                                    0,
                                    _childTop,
                                    vlp.width,
                                    _childTop + vlp.height
                            );
                        } else {
                            vlp.height = (int) maxHeight;
                            vlp.width = (int) (maxHeight * vlp.mVideoAspectRatio);
                            //lp.gravity = Gravity.CENTER_HORIZONTAL;
                            int _childLeft = (int) ((maxWidth - vlp.width) / 2);
                            child.layout(
                                    _childLeft,
                                    0,
                                    _childLeft + vlp.width,
                                    vlp.height
                            );
                        }
                    }
                    break;
                    case EFIT_HEIGHT_CENTER_WIDTH: {
                        child.setRotation(vlp.mVideoRotation);
                        if ((vlp.mVideoRotation/90)%2==1) {
                            vlp.width = (int) maxHeight;
                            vlp.height = (int) (maxHeight / vlp.mVideoAspectRatio);
                            int _left = (int)((maxWidth-vlp.width)/2);
                            int _top = (int) ((maxHeight-vlp.height)/2);
                            child.layout(
                                    _left,
                                    _top,
                                    _left + vlp.width,
                                    _top + vlp.height
                            );
                        } else {
                            vlp.height = (int) maxHeight;
                            vlp.width = (int) (maxHeight * vlp.mVideoAspectRatio);
                            //lp.gravity = Gravity.CENTER_HORIZONTAL;
                            int _childLeft = (int) ((maxWidth - vlp.width) / 2);
                            child.layout(
                                    _childLeft,
                                    0,
                                    _childLeft + vlp.width,
                                    vlp.height
                            );
                        }
                    }
                    break;
                    case EFIT_WIDTH_CENTER_HEIGHT: {
                        child.setRotation(vlp.mVideoRotation);
                        if ((vlp.mVideoRotation/90)%2==1) {
                            vlp.height = (int)maxWidth;
                            vlp.width = (int)(maxWidth * vlp.mVideoAspectRatio);
                            int _left = (int)((maxWidth-vlp.width)/2);
                            int _top = (int) ((maxHeight-vlp.height)/2);
                            child.layout(
                                    _left,
                                    _top,
                                    _left + vlp.width,
                                    _top + vlp.height
                            );
                        } else {
                            vlp.width = (int) maxWidth;
                            vlp.height = (int) (maxWidth / vlp.mVideoAspectRatio);
//                    lp.gravity = Gravity.CENTER_VERTICAL;
                            int _childTop = (int) ((maxHeight - vlp.height) / 2);
                            child.layout(
                                    0,
                                    _childTop,
                                    vlp.width,
                                    _childTop + vlp.height
                            );
                        }
                    }
                    break;
                    case EFIT_WIDTH_TOP_HEIGHT: {
                        child.setRotation(vlp.mVideoRotation);
                        if ((vlp.mVideoRotation/90)%2==1) {
                            if (vlp.mVideoAspectRatio>1) {
                                // landscape to portrait
                                vlp.height = (int)maxWidth;
                                vlp.width = (int)(maxWidth * vlp.mVideoAspectRatio);
                                int _left = (int)((vlp.width-maxWidth)/2);
                                int _top = (vlp.height-vlp.width)/2;
                                child.layout(
                                        _left,
                                        _top,
                                        _left + vlp.width,
                                        _top + vlp.height
                                );
                            } else {
                                // portrait to landscape
                                vlp.height = (int) maxWidth;
                                vlp.width = (int) (maxWidth * vlp.mVideoAspectRatio);
                                int _left = (int) ((maxWidth-vlp.width) / 2);
                                int _top = 0 - (vlp.height-vlp.width)/2;
                                child.layout(
                                        _left,
                                        _top,
                                        _left + vlp.width,
                                        _top + vlp.height
                                );
                            }
                        } else {
                            vlp.width = (int) maxWidth;
                            vlp.height = (int) (maxWidth / vlp.mVideoAspectRatio);
                            //lp.gravity = Gravity.TOP;
                            child.layout(
                                    0,
                                    0,
                                    vlp.width,
                                    vlp.height
                            );
                        }
                    }
                    break;
                    case EMATCH_PARENT: {
                        // FIXME : the rotation
                        vlp.width = (int) maxWidth;
                        vlp.height = (int) (maxHeight);
                        child.layout(
                                0,
                                0,
                                vlp.width,
                                vlp.height + 1
                        );
                    }
                    break;
                }

                if (child instanceof SurfaceView) {
                    SurfaceHolder holder = ((SurfaceView) child).getHolder();
                    if (holder != null) {
                        holder.setFixedSize(vlp.width, vlp.height);
                    }
                } else if (child instanceof GLSurfaceView) {
                    SurfaceHolder holder = ((GLSurfaceView) child).getHolder();
                    if (holder != null) {
                        holder.setFixedSize(vlp.width, vlp.height);
                    }
                }
            }
        }
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {

        private float mDownX, mDownY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            int act = event.getActionMasked();
            if (act == MotionEvent.ACTION_DOWN) {
                mDownX = x;
                mDownY = y;
            } else if (act == MotionEvent.ACTION_UP && mPlayerListener != null
                    && Math.abs(x - mDownX) < mTouch_slop && Math.abs(y - mDownY) < mTouch_slop) {
                // click
                mPlayerListener.onClicked();
            }
            if (mGLRenderer != null) {
                mGLRenderer.onTouch(event.getActionMasked(), x, y);
            }
            return true;
        }
    };

    public class FFMPEGPlayerLayoutParams extends MarginLayoutParams {

        public float mVideoAspectRatio = 0.75f;

        public int mVideoRotation = 0;

        /**
         * in this mode, we will use screen width/height rather than parent size to layout.
         */
        public boolean mFullScreenMode = false;

        public EVIDEO_LAYOUT mLayout = EVIDEO_LAYOUT.EFIT_WIDTH_TOP_HEIGHT;

        public FFMPEGPlayerLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public FFMPEGPlayerLayoutParams(int width, int height) {
            super(width, height);
        }

        public FFMPEGPlayerLayoutParams(LayoutParams source) {
            super(source);
        }


    }

    private static class SurfaceHolderCallback implements SurfaceHolder.Callback {

        private FFMPEGPlayer mPlayer;

        public SurfaceHolderCallback(FFMPEGPlayer player) {
            mPlayer = player;
        }

        public void destroy() {
            mPlayer = null;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mPlayer != null) {
                mPlayer.attachSurface(holder.getSurface());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mPlayer != null) {
                mPlayer.detachSurface();
            }
        }
    }

    private static class SurfaceTextureCallback implements TextureView.SurfaceTextureListener {

        private FFMPEGPlayer mPlayer;

        public SurfaceTextureCallback(FFMPEGPlayer player) {
            mPlayer = player;
        }

        public void destroy() {
            mPlayer = null;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (mPlayer != null) {
                mPlayer.attachSurface(new Surface(surface));
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (mPlayer != null) {
                mPlayer.detachSurface();
            }
            return true;// let it auto release
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    private static class GlSurfaceCallback implements GlRenderer.GlRendererCallback {

        private FFMPEGPlayer mPlayer;

        private GlSurfaceCallback(FFMPEGPlayer player) {
            mPlayer = player;
        }

        public void destroy() {
            mPlayer = null;
        }

        @Override
        public void onGlSurfaceCreated(Surface surface) {
            if (mPlayer != null) {
                mPlayer.attachSurface(surface);
            }
        }
    }

    private MoListener mMoPlayerListener = new MoListener() {

        @Override
        public void onBufferingEvent(float percentage) {
            Log.d(LOG_TAG, "====== onBufferingEvent, " + percentage);
            mHandler.obtainMessage(MSG_ON_BUFFERING, percentage).sendToTarget();
        }

        @Override
        public void onPlayEvent() {
            Log.d(LOG_TAG, "====== onPlayEvent ======");
            mHandler.obtainMessage(MSG_ON_PLAYING).sendToTarget();
        }

        @Override
        public void onPauseEvent() {
            Log.d(LOG_TAG, "====== onPauseEvent ======");
            mHandler.obtainMessage(MSG_ON_PAUSED).sendToTarget();
        }

        @Override
        public void onResumeEvent() {
            Log.d(LOG_TAG, "====== onResumeEvent ======");
            mHandler.obtainMessage(MSG_ON_RESUMED).sendToTarget();
        }

        @Override
        public void onStopEvent() {
            Log.d(LOG_TAG, "====== onStopEvent ======");
            mHandler.obtainMessage(MSG_ON_STOPPED).sendToTarget();
        }

        @Override
        public void onEndEvent() {
            Log.d(LOG_TAG, "====== onEndEvent ======");
            mHandler.obtainMessage(MSG_ON_ENDED).sendToTarget();
        }

        @Override
        public void onErrorEvent() {
            Log.d(LOG_TAG, "====== onErrorEvent ======");
            mHandler.obtainMessage(MSG_ON_ERROR).sendToTarget();
        }

        @Override
        public void onPlayTimeEvent(long time) {
//            Log.d(LOG_TAG, "====== onPlayTimeEvent, " + time);
        }

        @Override
        public void onVideoSizeChangedEvent(int width, int height, int rotation) {
            Log.d(LOG_TAG, "====== onVideoSizeChangedEvent width=" + width + ", height=" + height);
            mHandler.obtainMessage(MSG_ON_VIDEO_SIZE_CHANGED, width, height, rotation).sendToTarget();
        }

        @Override
        public void onReportIntermission(long startAPts, long startVPts, int durationMS, int iccMS) {
            Log.i(LOG_TAG, "====== onReportIntermission: durationMs: " + durationMS + ", iccMs: " + iccMS + ", vPts:" + startVPts);
            mHandler.obtainMessage(MSG_ON_REPORT_INTERMISSION,
                    new Object[]{
                            startAPts,
                            startVPts,
                            durationMS,
                            iccMS
                    }).sendToTarget();
        }

        @Override
        public void onReportStalled() {
            Log.i(LOG_TAG, "====== onReportStalled ======");
            mHandler.sendEmptyMessage(MSG_ON_REPORT_STALLED);
        }

        @Override
        public void onDidLatencyOptimization(int cl, long apts) {
            Log.i(LOG_TAG, "====== onDidLatencyOptimization: cl=" + cl + ", apts: " + apts);
            mHandler.obtainMessage(MSG_ON_DID_LATENCY_OPTIMIZATION,
                    new Object[]{
                            cl,
                            apts
                    }).sendToTarget();
        }

        @Override
        public void onDebug(String msg) {
            Log.d(LOG_TAG, msg);
        }
    };

    private static class MyHandler extends Handler {

        private WeakReference<FFMPEGPlayer> mWeakPlayerView = null;

        public MyHandler(FFMPEGPlayer v) {
            mWeakPlayerView = new WeakReference<>(v);
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PLAY: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        Object[] objArr = (Object[]) msg.obj;
                        v.doPlay((String[]) objArr[0],
                                (ERENDER_MODE) objArr[1],
                                (Long) objArr[2],
                                (Long) objArr[3],
                                (Integer)objArr[4],
                                (Integer)objArr[5],
                                (Integer)objArr[6],
                                (Integer)objArr[7]);
                    }
                }
                break;
                case MSG_STOP: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doStop();
                    }
                }
                break;
                case MSG_PAUSE: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doPause();
                    }
                }
                break;
                case MSG_RESUME: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doResume();
                    }
                }
                break;
                case MSG_ATTACH_SURFACE: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doAttachSurface((Surface) msg.obj);
                    }
                }
                break;
                case MSG_DETACH_SURFACE: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doDetachSurface();
                    }
                }
                break;
                case MSG_SEEK_TO: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doSeekTo((Long) msg.obj);
                    }
                }
                break;
                case MSG_SET_VOLUME: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doSetVolume((float)msg.obj);
                    }
                }
                break;
                case MSG_ON_VIDEO_SIZE_CHANGED: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.onVideoSizeChanged(msg.arg1, msg.arg2, (int)msg.obj);
                    }
                }
                break;
                case MSG_ON_BUFFERING: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.onBuffering((Float) msg.obj);
                    }
                }
                break;
                case MSG_ON_PLAYING: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.mSuspendStopEvent = false;
                        if (v.mCurrentState == STATE_PREPARING) {
                            v.onStateChanged(STATE_PREPARED);
                        }
//                        if (DEBUG && v.mPlayStartTime>0L
//                                && v.mCandidateUrls!=null
//                                && v.mCurrentUrlIdx>-1
//                                && v.mCurrentUrlIdx<v.mCandidateUrls.length) {
//                            long pastSec = (System.currentTimeMillis()-v.mPlayStartTime)/1000L;
//                            v.mPlayStartTime = 0L;
//                            Toast.makeText(v.getContext(),
//                                    v.mCandidateUrls[v.mCurrentUrlIdx].substring(0, 4)
//                                            + "\nloading time: "
//                                            + pastSec
//                                            + " seconds",
//                                    Toast.LENGTH_SHORT).show();
//                        }
                        if (v.mMediaController != null) {
                            v.mMediaController.setEnabled(v.mPlayer.isSeekable());
                        }
                        v.onStateChanged(STATE_PLAYING);
                    }
                }
                break;
                case MSG_ON_STOPPED: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null && !v.mSuspendStopEvent) {
                        v.mPlayStartTime = 0L;
                        v.onStateChanged(STATE_IDLE);
                    }
                }
                break;
                case MSG_ON_PAUSED: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.onStateChanged(STATE_PAUSED);
                    }
                }
                break;
                case MSG_ON_RESUMED: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.onStateChanged(STATE_PLAYING);
                    }
                }
                break;
                case MSG_ON_ERROR: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.onError();
                    }
                }
                break;
                case MSG_ON_ENDED: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null && !v.mSuspendStopEvent) {
                        v.mPlayStartTime = 0L;
                        if (v.mPlayerListener != null) {
                            v.mPlayerListener.onCompletion();
                        }
                        v.onStateChanged(STATE_IDLE);
                    }
                }
                break;
                case MSG_SUSPEND_STOP_EVENT: {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.mSuspendStopEvent = msg.arg1 != 0;
                    }
                }
                break;
                case MSG_SET_LOOP : {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        v.doSetLoop(msg.arg1 != 0);
                    }
                }
                break;
                case MSG_SET_LATENCY_OPT : {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null) {
                        int[] intArr = (int[]) msg.obj;
                        v.doSetLatencyOptimization(
                                intArr[0],
                                intArr[1],
                                intArr[2],
                                intArr[3]
                        );
                    }
                }
                break;
                case MSG_ON_REPORT_INTERMISSION : {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null && v.mPlayerListener!=null) {
                        Object[] objArr = (Object[]) msg.obj;
                        v.mPlayerListener.onReportIntermission(
                                (Long)objArr[0],
                                (Long)objArr[1],
                                (Integer)objArr[2],
                                (Integer)objArr[3]
                        );
                    }
                }
                break;
                case MSG_ON_REPORT_STALLED : {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null && v.mPlayerListener!=null) {
                        //
                    }
                }
                break;
                case MSG_ON_DID_LATENCY_OPTIMIZATION : {
                    FFMPEGPlayer v = mWeakPlayerView.get();
                    if (v != null && v.mPlayerListener!=null) {
                        Object[] objArr = (Object[]) msg.obj;
                        v.mPlayerListener.onDidLatencyOptimization(
                                (Integer)objArr[0],
                                (Long)objArr[1]
                        );
                    }
                }
                break;
            }
        }
    }


}
