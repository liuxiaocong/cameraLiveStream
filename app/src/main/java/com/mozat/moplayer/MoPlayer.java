package com.mozat.moplayer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.view.Surface;

import java.nio.ByteBuffer;


/**
 * Created by Administrator on 2015/11/10 0010.
 */
public class MoPlayer {
    static {

//        System.loadLibrary("ffmpeg");
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("moplayer");

    }
    private Context context;
    private AudioTrack audioTrack = null;
    private long mPtrContext;

    public MoPlayer(Context context)
    {
        this.context = context;
        mPtrContext = initPlayer();
    }

    public void destroy() {
        speexAECDestroy(mPtrContext);
        destroyPlayer(mPtrContext);
        audioTrack = null;
        context = null;
    }

    private native long initPlayer();

    private native void destroyPlayer(long ptrContext);

    /**
     * play media,when media start play MoListener.onPlay() will called
     * @param path play url
     * @param seekTo where to position we seek to, right after started. In micro seconds.
     * @param stream_type audio track stream type. e.g. AudioManager.STREAM_MUSIC .
     * @param channels specific audio channels to be play or merged to. 0 means use the value parsed from stream.
     * @param sampleRate is the sample rate we want the player to playback (by re-sampling), in unit hz.
     *                        generally, the value is 22100, 44100, 48000, or 0 if you dont want to
     *                        specify a value and just use the meta data from stream.
     * @param probeSize is used to override the default probe size, in bytes unit.
     * @param reportIntermissionThreshold threshold to report intermission via callbacks. the
     *                  intermissions less than this will be ignored. At least 0.05 seconds.
     *@return void
     */
    public void play(String path, long seekTo, int stream_type, int channels, int sampleRate, int probeSize, int reportIntermissionThreshold) {
        play(mPtrContext, path, seekTo, stream_type, channels, sampleRate, probeSize, reportIntermissionThreshold);
    }
    private native void play(long ptrContext, String path, long seekTo, int stream_type, int channels, int sampleRate, int probeSize, int reportIntermissionThreshold);


    /**
     * puase play, MoListener.onPause() will be called
     */
    public void pause() {
        pause(mPtrContext);
    }
    private native void pause(long ptrContext);

    /**
     * resume play,MoListener.onResume will be called
     */
    public void resume() {
        resume(mPtrContext);
    }
    private native void resume(long ptrContext);

    /**
     * stop play,MoListener.onStop() will be called
     */
    public void stop() {
        stop(mPtrContext);
    }
    private native void stop(long ptrContext);

    /**
     * @return  true or false,only media is playing return value is true,or is false
     *
     */
    public boolean isPlaying() {
        return isPlaying(mPtrContext);
    }
    private native boolean isPlaying(long ptrContext);

    /**
     * attach surface
     * @param surface,render Surface,used by video output
     */
    public void attachSurface(Surface surface) {
        attachSurface(mPtrContext, surface);
    }
    private native void attachSurface(long ptrContext, Surface surface);

    /**
     * detath surface
     */
    public void detachSurface() {
        detachSurface(mPtrContext);
    }
    private native void detachSurface(long ptrContext);

    /**
     * setListener,when play event happen,listener callback function will be called
     * @param listener
     */
    public void setListener(MoListener l) {
        setListener(mPtrContext, l);
    }
    private native void setListener(long ptrContext, MoListener listener);

    /**
     * remove the listener
     */
    public void removeListener() {
        removeListener(mPtrContext);
    }
    private native void removeListener(long ptrContext);

    /**
     * check the media if can seekable
     * @return true or false,true can seekable,false can not seekable
     */
    public boolean isSeekable() {
        return isSeekable(mPtrContext);
    }
    private native  boolean isSeekable(long ptrContext);

    /**
     * set media posistion
     * @param position,time unit is micro second
     */
    public void setPosition(long position) {
        setPosition(mPtrContext, position);
    }
    private native void setPosition(long ptrContext, long position);

    /**
     * get media current postion
     * @return media postion,time unit is microsecond
     */
    public long getPosition() {
        return getPosition(mPtrContext);
    }
    private native long getPosition(long ptrContext);

    /**
     * get media current play time
     * @return time unit is micro second
     */
    public long getCurrentTime() {
        return getCurrentTime(mPtrContext);
    }
    private native long getCurrentTime(long ptrContext);

    /**
     * get media duration
     * @return time unit is micro second
     */
    public long getDuration() {
        return getDuration(mPtrContext);
    }
    private native long getDuration(long ptrContext);

    /**
     * set network buffering time
     * @param bufferTime time unit is millisecond
     */
    public void setBufferTime(long bufferTime) {
        setBufferTime(mPtrContext, bufferTime);
    }
    private native void setBufferTime(long ptrContext, long bufferTime);

    public void speexAECOpen(int sample_rate, int frame_size, int filter_length, int record_buff_size) {
        speexAECOpen(mPtrContext, sample_rate, frame_size, filter_length, record_buff_size);
    }
    private native void speexAECOpen(long ptrContext, int sample_rate, int frame_size, int filter_length, int record_buff_size);

    public void speexAECCapture(ByteBuffer input, int size, ByteBuffer output) {
        speexAECCapture(mPtrContext, input, size, output);
    }
    private native void speexAECCapture(long ptrContext, ByteBuffer input, int size, ByteBuffer output);

    public void speexAECReset() {
        speexAECReset(mPtrContext);
    }
    private native void speexAECReset(long ptrContext);

    public void speexAECDestroy() {
        speexAECDestroy(mPtrContext);
    }
    private native void speexAECDestroy(long ptrContext);

    /**
     * sets the stream type of the player. if we are already using the specific stream type,
     * nothing will happen.
     * @param streamType
     */
    public void setStreamType(int streamType, int sampleRate, int channels) {
        setStreamType(mPtrContext, streamType, sampleRate, channels);
    }

    private native void setStreamType(long ptrContext, int streamType, int sampleRate, int channels);

    public void clearBuffer() {
        clearBuffer(mPtrContext);
    }

    private native void clearBuffer(long ptrContext);

    /**
     * sets the latency optimization parameters.
     * @param interval is the checking interval, in microseconds unit,
     * stop checking if interval less than 1 second.
     * @param threshold_min indicates how many latency we accept at least (if
     * checking ongoing), in microseconds unit.
     * @param threshold_max is the max threshold when we increasing by @param threashold_step .
     * Note that, after calling this method, we ALWAYS set threshold from the @param threshold_min .
     */
    public void setLatencyOptimization(int interval, int threshold_min, int threshold_max, int threshold_step, int keepDurationSeconds) {
        setLatencyOptimization(mPtrContext, interval, threshold_min, threshold_max, threshold_step, keepDurationSeconds);
    }

    private native void setLatencyOptimization(long ptrContext, int interval, int threshold_min, int threshold_max, int threshold_step, int keepDurationSeconds);

    /**
     * sets the minimal audio packet buffer we should keep before starting playing.
     * by default is's 300 millseconds and 50kB.
     * @param minimalADurationMs is the minimal audio packet duration we should keep, in millisedonds
     * @param minimalABytes is the minimal audio packet buffer size we should keep, ONLY valid if the duration is less than 10 milliseconds.
     */
    public void setMinimalAudioBuffer(long minimalADurationMs, int minimalABytes) {
        setMinimalAudioBuffer(mPtrContext, minimalADurationMs, minimalABytes);
    }

    private native void setMinimalAudioBuffer(long ptrContext, long minimalADurationMs, int minimalABytes);

    public void setLoop(boolean loop) {
        setLoop(mPtrContext, loop);
    }

    private native void setLoop(long ptrContext, boolean loop);

    public void getPacketQueueInfo(long[] longArr) {
        getPacketQueueInfo(mPtrContext, longArr);
    }

    private native void getPacketQueueInfo(long ptrContext, long[] longArr);

//    /**
//     * get system volume
//     * @return the system volume
//     */
//    public int getVolume()
//    {
//        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//    }

    /**
     * sets the output volume of this player.
     * @param volume is between [0.0, 1.0]
     */
    public void setVolume(float volume)
    {
//        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        if (audioTrack!=null) {
            audioTrack.setStereoVolume(volume, volume);
        }
    }

    /**
     * called by jni,jni will use it play audio
     * @param sampleRateInHz
     * @param numberOfChannels
     * @return
     */
    private AudioTrack prepareAudioTrack(int stream_type, int sampleRateInHz, int numberOfChannels) {

        for (;;) {
            int channelConfig;
            if (numberOfChannels == 1) {
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            } else if (numberOfChannels == 2) {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            } else if (numberOfChannels == 3) {
                channelConfig = AudioFormat.CHANNEL_OUT_FRONT_CENTER
                        | AudioFormat.CHANNEL_OUT_FRONT_RIGHT
                        | AudioFormat.CHANNEL_OUT_FRONT_LEFT;
            } else if (numberOfChannels == 4) {
                channelConfig = AudioFormat.CHANNEL_OUT_QUAD;
            } else if (numberOfChannels == 5) {
                channelConfig = AudioFormat.CHANNEL_OUT_QUAD
                        | AudioFormat.CHANNEL_OUT_LOW_FREQUENCY;
            } else if (numberOfChannels == 6) {
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
            } else if (numberOfChannels == 8) {
                channelConfig = AudioFormat.CHANNEL_OUT_7POINT1;
            } else {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            }
            try {
                int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz,
                        channelConfig, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(
                        stream_type, sampleRateInHz,
                        channelConfig, AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize, AudioTrack.MODE_STREAM);
                return audioTrack;
            } catch (IllegalArgumentException e) {
                if (numberOfChannels > 2) {
                    numberOfChannels = 2;
                } else if (numberOfChannels > 1) {
                    numberOfChannels = 1;
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * FOR JNI
     * @param data
     * @param size
     * @return
     */
    public final int playAudio(ByteBuffer data, int size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return audioTrack.write(data, size, AudioTrack.WRITE_BLOCKING);
        } else {
            return audioTrack.write(data.array(), 0, size);
        }
    }

    /**
     * FOR JNI
     * @param capacity
     * @return
     */
    public ByteBuffer newDirectByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }

}
