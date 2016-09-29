package mozat.rings.libffmpeg;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import com.mozat.moplayer.MoListener;
import com.mozat.moplayer.MoPlayer;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Wenjian on 2015/11/27.
 */
class PlayerControlThread extends Thread {

    private final static String LOG_TAG = PlayerControlThread.class.getSimpleName();

    private static final int ACT_PLAY_MRL = 1;

    private static final int ACT_PAUSE = ACT_PLAY_MRL + 1;

    private static final int ACT_RESUME = ACT_PAUSE + 1;

    private static final int ACT_STOP = ACT_RESUME + 1;

    private static final int ACT_ATTACH_SURFACE = ACT_STOP + 1;

    private static final int ACT_DETACH_SURFACE = ACT_ATTACH_SURFACE + 1;

    private static final int ACT_SET_STREAM_TYPE = ACT_DETACH_SURFACE + 1;

    private static final int ACT_SET_LOOP = ACT_SET_STREAM_TYPE + 1;

    private static final long BUFFER_TIME = 0L;

    private ArrayList<BasePlayTask> mControlQueue = new ArrayList<>();

    private MoPlayer moPlayer = null;

    private boolean mRunning = false;

    public PlayerControlThread(Context ctx, MoListener l) {
        super(LOG_TAG);
        moPlayer = new MoPlayer(ctx);
        moPlayer.setListener(l);
        moPlayer.setBufferTime(BUFFER_TIME);
        mRunning = true;
    }


    public final void exit() {
        synchronized (mControlQueue) {
            mControlQueue.clear();
            mRunning = false;
            mControlQueue.notify();
        }
    }

    @Override
    public void run() {
        try {
            while (mRunning) {
                BasePlayTask task = null;

                synchronized (mControlQueue) {
                    if (mControlQueue.size() == 0) {
                        mControlQueue.wait();
                    } else {
                        task = mControlQueue.remove(0);
                    }
                }
                if (task==null) {
                    continue;
                }

                switch (task.mAction) {
                    case ACT_PLAY_MRL : {
                        PlayMRLTask _pt = (PlayMRLTask) task;
//                        if (BuildConfig.DEBUG) {
                            Log.d(LOG_TAG, "========= play ======== " + _pt.mMRL);
//                        } else {
//                            Log.d(LOG_TAG, onToken + " : ========= play ======== ");// + mPlayURL);//  + _pt.mMRL);
//                        }
                        moPlayer.setBufferTime(BUFFER_TIME);
                        try {
                            moPlayer.play(_pt.mMRL, _pt.mSeekTo, _pt.mStreamType, _pt.mChannels, _pt.mSampleRate, _pt.mProbeSize, _pt.mReportIntermissionThreshold);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                    case ACT_STOP : {
                        Log.d(LOG_TAG, "========= stop ======== ");
                        moPlayer.stop();
                    }
                    break;
                    case ACT_PAUSE : {
                        Log.d(LOG_TAG, "========= pause ======== ");
                        moPlayer.pause();
                    }
                    break;
                    case ACT_RESUME : {
                        Log.d(LOG_TAG, "========= resume ======== ");
                        moPlayer.resume();
                    }
                    break;
                    case ACT_ATTACH_SURFACE : {
                        AttachSurfaceTask _at = (AttachSurfaceTask) task;
                        Surface surface = _at.getSurface();
                        if (surface!=null) {
                            Log.d(LOG_TAG, "========= attach surface ======== ");
                            moPlayer.attachSurface(surface);
                        } else {
                            Log.d(LOG_TAG, "========= failed to attach surface, null ======== ");
                        }
                    }
                    break;
                    case ACT_DETACH_SURFACE : {
                        Log.d(LOG_TAG, "========= detach ======== ");
                        moPlayer.detachSurface();
//                        moPlayer.stop();
                    }
                    break;
                    case ACT_SET_STREAM_TYPE : {
                        SetStreamTypeTask _at = (SetStreamTypeTask) task;
                        Log.d(LOG_TAG, "========= set stream type ======== " + _at.mStreamType);
                        moPlayer.setStreamType(_at.mStreamType, _at.mSampleRate, _at.mChannels);
                    }
                    break;
                    case ACT_SET_LOOP : {
                        SetLoopTask _at = (SetLoopTask) task;
                        Log.d(LOG_TAG, "========= set loop ======== " + _at.mLoop);
                        moPlayer.setLoop(_at.mLoop);
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (moPlayer!=null) {
                moPlayer.stop();
//                moPlayer.setListener(null);
                moPlayer.removeListener();
                moPlayer.destroy();
                moPlayer = null;
            }
            Log.d(LOG_TAG, "========= player thread is running out ======== ");
        }
    }


    public long getDuration()
    {
        return moPlayer.getDuration();
    }

    public long getCurrentTime()
    {
        return moPlayer.getCurrentTime();
    }

    public void setPosition(long position)
    {
        moPlayer.setPosition(position);
    }

    public void setBufferTime(long bufferTime)
    {
        moPlayer.setBufferTime(bufferTime);
    }

    public boolean isSeekable()
    {
        return  moPlayer.isSeekable();
    }

    public boolean isPlaying()
    {
        return moPlayer.isPlaying();
    }

    public void setVolume(float vol) {
        if (moPlayer!=null) {
            moPlayer.setVolume(vol);
        }
    }

    public void play(String mrl, long seekTo, int streamType, int channels, int sampleRate, int probeSize, int reportIntermissionThreshold) {
        synchronized (mControlQueue) {
            mControlQueue.add(new PlayMRLTask(mrl, seekTo, streamType, channels, sampleRate, probeSize, reportIntermissionThreshold));
            mControlQueue.notify();
        }
    }

    public void stopPlaying() {
        synchronized (mControlQueue) {
            mControlQueue.add(new StopTask());
            mControlQueue.notify();
        }
    }

    public void pausePlaying() {
        synchronized (mControlQueue) {
            mControlQueue.add(new PauseTask());
            mControlQueue.notify();
        }
    }

    public void resumePlaying() {
        synchronized (mControlQueue) {
            mControlQueue.add(new ResumeTask());
            mControlQueue.notify();
        }
    }

    public void attachSurface(Surface sf) {
        synchronized (mControlQueue) {
            mControlQueue.add(new AttachSurfaceTask(sf, ""));
            mControlQueue.notify();
        }
    }

    public void detachSurface() {
        synchronized (mControlQueue) {
            mControlQueue.add(new DetachSurfaceTask());
            mControlQueue.notify();
        }
    }

    public void speexAECOpenD(int sample_rate, int frame_size, int filter_length, int record_buff_size) {
        moPlayer.speexAECOpen(sample_rate, frame_size, filter_length, record_buff_size);
    }

    public void speexAECCaptureD(ByteBuffer input, int size, ByteBuffer output) {
        moPlayer.speexAECCapture(input, size, output);
    }

    public void speexAECReset() {
        moPlayer.speexAECReset();
    }

    public void speexAECDestroyD() {
        moPlayer.speexAECDestroy();
    }

    public void clearBufferD() {
        moPlayer.clearBuffer();
    }

    public void setLatencyOptimizationD(int interval, int threshold_min, int threshold_max, int threshold_step, int keepDurationSeconds) {
        moPlayer.setLatencyOptimization(interval, threshold_min, threshold_max, threshold_step, keepDurationSeconds);
    }

    public void setMinimalAudioBufferD(long minimalADurationMs, int minimalABytes) {
        moPlayer.setMinimalAudioBuffer(minimalADurationMs, minimalABytes);
    }

    public void getPacketQueueInfoD(long[] longArr) {
        moPlayer.getPacketQueueInfo(longArr);
    }

    public void setLoop(boolean loop) {
        synchronized (mControlQueue) {
            mControlQueue.add(new SetLoopTask(loop));
            mControlQueue.notify();
        }
    }

    public void setStreamType(int streamType, int sampleRate, int channels) {
        synchronized (mControlQueue) {
            mControlQueue.add(new SetStreamTypeTask(streamType, sampleRate, channels));
            mControlQueue.notify();
        }
    }


    private static abstract class BasePlayTask {
        private int mAction = -1;
        public BasePlayTask(int task) {
            mAction = task;
        }
    }

    private static class PlayMRLTask extends BasePlayTask {
        private String mMRL;
        private long mSeekTo;
        private int mStreamType;
        private int mChannels;
        private int mSampleRate;
        private int mProbeSize;
        private int mReportIntermissionThreshold;
        public PlayMRLTask(String mrl, long seekTo, int streamType, int channels, int sampleRate, int probeSize, int reportIntermissionThreshold) {
            super(ACT_PLAY_MRL);
            mMRL = mrl;
            mSeekTo = seekTo;
            mChannels = channels;
            mChannels = channels;
            mSampleRate = sampleRate;
            mStreamType = streamType;
            mProbeSize = probeSize;
            mReportIntermissionThreshold = reportIntermissionThreshold;
        }
    }

    private static class StopTask extends BasePlayTask {
        public StopTask() {
            super(ACT_STOP);
        }
    }

    private static class PauseTask extends BasePlayTask {
        public PauseTask() {
            super(ACT_PAUSE);
        }
    }

    private static class ResumeTask extends BasePlayTask {
        public ResumeTask() {
            super(ACT_RESUME);
        }
    }

//    private static class SetPositionTask extends BasePlayTask {
//        private float mPosition;
//        public SetPositionTask(float pos) {
//            super(ACT_SET_POSITION);
//            mPosition = pos;
//        }
//    }

//    private static class SetVolumeTask extends BasePlayTask {
//        private int mVolume;
//        public SetVolumeTask(String onToken, int vol) {
//            super(onToken, ACT_SET_VOLUME);
//            mVolume = vol;
//        }
//    }

    private static class AttachSurfaceTask extends BasePlayTask {
        private Surface mSurface;

        private String mWhere;
        public AttachSurfaceTask(Surface surface, String where) {
            super(ACT_ATTACH_SURFACE);
            mSurface = surface;
            mWhere = where;
        }

        public Surface getSurface() {
            return mSurface;
        }
    }

    private static class DetachSurfaceTask extends BasePlayTask {
        public DetachSurfaceTask() {
            super(ACT_DETACH_SURFACE);
        }
    }

    private static class SetStreamTypeTask extends BasePlayTask {
        private int mStreamType;
        private int mSampleRate;
        private int mChannels;

        public SetStreamTypeTask(int streamType, int sampleRate, int channels) {
            super(ACT_SET_STREAM_TYPE);
            mStreamType = streamType;
            mSampleRate = sampleRate;
            mChannels = channels;
        }
    }

    private static class SetLoopTask extends BasePlayTask {
        private boolean mLoop;

        public SetLoopTask(boolean l) {
            super(ACT_SET_LOOP);
            mLoop = l;
        }
    }
}
