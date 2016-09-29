package liuxiaocong.com.camerauploadandplay.common;


public class VideoQuality {
    private String mX264Profile = "superfast";
    private int mVideoWidth = 240;
    private int mVideoHeight = 320;
    private int mFrameRate = 30;
    private int mSampleAudioRateInHz = 22050;
    private int mVideoBitrate = 100000;
    private int mAudioBitrate = 16000;

    public VideoQuality(int videoWidth, int videoHeight, int frameRate, int audioSampleRate,
                        int videoBitrate, int audioBitrate, String x264Profile) {
        mX264Profile = x264Profile;
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
        mFrameRate = frameRate;
        mSampleAudioRateInHz = audioSampleRate;
        mVideoBitrate = videoBitrate;
        mAudioBitrate = audioBitrate;

    }

    /**
     * change video size, keep BPP to update the videBitrate
     *
     * @param w
     * @param h
     */
    public void changeVideSize(int w, int h) {
        double bpp = mVideoWidth * mVideoHeight * mFrameRate / (double) mVideoBitrate;
        mVideoWidth = w;
        mVideoHeight = h;
        mVideoBitrate = (int) (w * h * mFrameRate / bpp);
    }

    public String getX264Profile() {
        return mX264Profile;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public int getSampleAudioRateInHz() {
        return mSampleAudioRateInHz;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VideoQuality)) {
            return false;
        }

        VideoQuality quality = (VideoQuality) o;

        return (quality.mX264Profile.equals(this.mX264Profile) && quality.mVideoWidth == this.mVideoWidth && quality.mVideoHeight == this.mVideoHeight && quality.mFrameRate == this.mFrameRate && quality.mSampleAudioRateInHz == this.mSampleAudioRateInHz);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + mVideoWidth;
        result = 31 * result + mVideoHeight;
        result = 31 * result + mFrameRate;
        result = 31 * result + mSampleAudioRateInHz;
        result += mX264Profile.hashCode();
        return result;
    }

    @Override
    public VideoQuality clone() {
        return new VideoQuality(mVideoWidth, mVideoHeight, mFrameRate, mSampleAudioRateInHz, mVideoBitrate, mAudioBitrate, mX264Profile);
    }

    public int getAudioBitrate() {
        return mAudioBitrate;
    }

    public int getVideoBitrate() {
        return mVideoBitrate;
    }

    @Override
    public String toString() {
        return "x264Profile = " + mX264Profile
                + "videoWidth = " + mVideoWidth
                + "; videoHeight = " + mVideoHeight
                + "; frameRate = " + mFrameRate
                + "; audioSampleRate = " + mSampleAudioRateInHz
                + "; videoBitrate = " + mVideoBitrate
                + "; audioBitrate = " + mAudioBitrate
                ;
    }
}
