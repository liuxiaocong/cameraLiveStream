package liuxiaocong.com.camerauploadandplay;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

/**
 * Created by LiuXiaocong on 9/28/2016.
 */
public class AudioRecorder {
    private int mSampleRate = 44100;
    private Thread mAudioThread;
    private AudioRecordRunnable mAudioRecordRunnable;
    private boolean runAudioThread = false;
    private AudioRecord mAudioRecord;
    private int mFrameSize;
    private int mFilterLength;
    private int mBufferSize;

    interface AudioRecorderListener {
        void onAudioDataUpdate(ByteBuffer buffer);

        void onFail();
    }


    public AudioRecorder() {
        float samplesPerMilli = (float) mSampleRate / 1000.0f;

        int minBufferSize = AudioRecord.getMinBufferSize(
                mSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mFrameSize = (int) (40.0f * samplesPerMilli);
        if (mFrameSize * 2 > minBufferSize) {
            mBufferSize = mFrameSize * 2;
        } else {
            mBufferSize = minBufferSize;
            mFrameSize = minBufferSize / 2;
        }

        mFilterLength = (int) (200.0f * samplesPerMilli);
    }

    public void startRecorder(AudioRecorderListener audioRecorderListener) {
        mAudioRecordRunnable = new AudioRecordRunnable(audioRecorderListener);
        mAudioThread = new Thread(mAudioRecordRunnable);
        runAudioThread = true;
        mAudioThread.start();
    }

    public void release(){
        runAudioThread = false;
    }

    class AudioRecordRunnable implements Runnable {
        private AudioRecorderListener mAudioRecorderListener;

        public AudioRecordRunnable(AudioRecorderListener audioRecorderListener) {
            mAudioRecorderListener = audioRecorderListener;
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    mSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mBufferSize);
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            // Audio
            ByteBuffer audioData;
            int bufferReadResult;
            audioData = ByteBuffer.allocateDirect(mBufferSize);
            mAudioRecord.startRecording();
            while (runAudioThread) {
                if (mAudioRecord != null) {
                    bufferReadResult = mAudioRecord.read(audioData, mBufferSize);
                    if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE) {
                        mAudioRecorderListener.onFail();
                    } else {
                        if (bufferReadResult > 0) {
                            //audioData.position(0).limit(bufferReadResult);
                            mAudioRecorderListener.onAudioDataUpdate(audioData);
                        }
                    }
                }
            }
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
    }

}
