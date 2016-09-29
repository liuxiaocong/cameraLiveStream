package liuxiaocong.com.camerauploadandplay.play;

import android.media.AudioManager;

import mozat.rings.libffmpeg.FFMPEGPlayer;

/**
 * Created by LiuXiaocong on 9/29/2016.
 */
public class PlayPresenterImpl implements PlayContact.Presenter {
    //private String mPlayUrl = "http://d14jvptfm9jqfj.cloudfront.net:80/live/730/playlist.m3u8";
    //rings dt
    private String mPlayUrl = "http://d14jvptfm9jqfj.cloudfront.net:80/live/729/playlist.m3u8";
    private PlayContact.View mView;

    @Override
    public void setView(PlayContact.View view) {
        mView = view;
        mView.setPlayerListener(mFFMPEGPlayerListener);
    }

    @Override
    public void startPlay() {
        startPlay(mPlayUrl);
    }

    @Override
    public void clear() {
        if(mView!=null)
        {
            mView.clear();
        }
        mFFMPEGPlayerListener = null;
    }

    private void startPlay(String url) {
        if (mView == null) return;
        if (mView.getFFMPEGPlayer() == null) return;
        String[] playUrls = new String[]{mPlayUrl};
        mView.getFFMPEGPlayer().requestFocus();
        mView.getFFMPEGPlayer().playA(
                playUrls,
                FFMPEGPlayer.ERENDER_MODE.ENORMAL_TEXTURE_VIEW,
                0,
                0,
                AudioManager.STREAM_MUSIC,
                0,
                0,
                0);// no intermission report
    }

    private FFMPEGPlayer.FFMPEGPlayerListener mFFMPEGPlayerListener = new FFMPEGPlayer.FFMPEGPlayerListener() {
        @Override
        public void onCompletion() {

        }

        @Override
        public void onBufferingUpdate(float percent) {

        }

        @Override
        public void onStateChanged(int st) {
            switch (st) {
                case FFMPEGPlayer.STATE_ERROR: {
                    //un normal stop

                }
                break;
                case FFMPEGPlayer.STATE_IDLE: {
                    //end

                }
                break;
                case FFMPEGPlayer.STATE_PREPARING: {

                }
                break;
                case FFMPEGPlayer.STATE_PREPARED: {

                }
                break;
                case FFMPEGPlayer.STATE_PLAYING: {

                }
                break;
                case FFMPEGPlayer.STATE_PAUSED: {

                }
                break;
                case FFMPEGPlayer.STATE_STOPPING: {

                }
                break;
            }
        }

        @Override
        public void onVideoSizeChanged(int width, int height) {

        }

        @Override
        public void onClicked() {

        }

        @Override
        public void onReportIntermission(long startAPts, long startVpts, int durationMS, int iccMS) {
            // no need
        }

        @Override
        public void onReportStalled() {
            // no need
        }

        @Override
        public void onDidLatencyOptimization(int cl, long apts) {
            // no need
        }
    };
}
