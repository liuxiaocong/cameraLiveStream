package mozat.rings.libffmpeg;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import liuxiaocong.com.camerauploadandplay.R;

public class FFMPEGMediaController extends FrameLayout implements FFMPEGPlayer.IFFMPEGMediaController {

    private static final String LOG_TAG = FFMPEGMediaController.class.getSimpleName();

    private static final int SHOW_PROGRESS = 2;
    private FFMPEGPlayer mPlayer;
    private Context mContext;
    private SeekBar mProgress;
    private ImageView mControlBtn;
    private TextView mEndTime, mCurrentTime;
    private long mDuration;
    private boolean mDragging;
    private boolean mInstantSeeking = false;
    private AudioManager mAM;
    private OnShownListener mShownListener;
    private OnHiddenListener mHiddenListener;
    private boolean mIsUserStop = false;

    private int mPlayerState = FFMPEGPlayer.STATE_IDLE;

    public void setOnShownListener(OnShownListener l) {
        mShownListener = l;
    }

    public void setOnHiddenListener(OnHiddenListener l) {
        mHiddenListener = l;
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            long pos;
            switch (msg.what) {
                case SHOW_PROGRESS:
//                    pos = setProgress();
                    setProgress();
                    if (!mDragging && getVisibility()==VISIBLE) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000);// - (pos % 1000));
                    }
                    break;
            }
        }
    };

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
            show();
            mHandler.removeMessages(SHOW_PROGRESS);
            if (mInstantSeeking) {
                mAM.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser)
                return;

            long newposition = mProgress.getProgress();
            String time = generateTime(newposition);
            if (mInstantSeeking) {
//                float _pos = (float)progress / (float)bar.getMax();
//                mPlayer.seekToPositionA(_pos);
                mPlayer.seekToA(mProgress.getProgress()*1000L);
            }
            if (mCurrentTime != null) {
                mCurrentTime.setText(time);
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            if (!mInstantSeeking) {
//                float _pos = (float)bar.getProgress() / (float)bar.getMax();
//                mPlayer.seekToPositionA(_pos);
                mPlayer.seekToA(mProgress.getProgress()*1000L);
            }
            show();
            mHandler.removeMessages(SHOW_PROGRESS);
            mAM.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mDragging = false;
            mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
        }
    };

    public FFMPEGMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initController(context);
    }

    public FFMPEGMediaController(Context context) {
        super(context);
        initController(context);
    }

    private boolean initController(Context context) {
        mContext = context;
        mAM = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return true;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                getResources().getIdentifier("mediacontroller", "layout", mContext.getPackageName()), this);
        mProgress = (SeekBar) findViewById(getResources().getIdentifier("mediacontroller_seekbar", "id", mContext.getPackageName()));
        mControlBtn = (ImageView) findViewById(getResources().getIdentifier("mediacontroller_control_btn", "id", mContext.getPackageName()));
        if(mControlBtn!=null)
        {
            mControlBtn.setOnClickListener(mOnClickListener);
        }
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) findViewById(getResources().getIdentifier("mediacontroller_time_total", "id", mContext.getPackageName()));
        mCurrentTime = (TextView) findViewById(getResources().getIdentifier("mediacontroller_time_current", "id", mContext.getPackageName()));
    }

    public void destroy() {
        mHandler.removeMessages(SHOW_PROGRESS);
        mPlayer = null;
        mAM = null;
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int i = view.getId();
            if (i == R.id.mediacontroller_control_btn) {
                if(mPlayer.isPlaying())
                {
                    mIsUserStop = true;
                    mPlayer.togglePlayAndPause();
                    mControlBtn.setImageResource(R.drawable.bt_play);
                }else {
                    mIsUserStop = false;
                    mPlayer.togglePlayAndPause();
                    mControlBtn.setImageResource(R.drawable.bt_stop);
                }
            }
        }
    };

    public void resetPlayButton()
    {
        if(mControlBtn!=null){
            mControlBtn.setImageResource(R.drawable.bt_stop);
        }
    }

    public boolean isUserStop()
    {
        return mIsUserStop;
    }

    /**
     * Control the action when the seekbar dragged by user
     *
     * @param seekWhenDragging True the media will seek periodically
     */
    public void setInstantSeeking(boolean seekWhenDragging) {
        mInstantSeeking = seekWhenDragging;
    }

    @Override
    public void setPlayerView(FFMPEGPlayer player) {
        mPlayer = player;
    }

    /**
     * Show the controller on screen.
     */
    public void show() {
        if (getVisibility()!=VISIBLE) {
            setVisibility(View.VISIBLE);
            if (mShownListener != null) {
                mShownListener.onShown();
            }
        }
        mHandler.removeMessages(SHOW_PROGRESS);
        if (mPlayer.isSeekable()) {
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    }

    @Override
    public void onPlayPositionChanged(float p) {

    }

    @Override
    public void onPlayTimeChanged(long t) {

    }

    @Override
    public void onStateChanged(int st) {
        if (st== FFMPEGPlayer.STATE_PLAYING) {
            if (mPlayer.isSeekable()) {
                mProgress.setMax((int) (mPlayer.getDuration()/1000L));
            } else {
                mProgress.setMax(0);
                mProgress.setProgress(0);
            }
        }
        mPlayerState = st;
    }

    public boolean isShowing() {
        return getVisibility()==VISIBLE;
    }

    public void hide() {
        if (getVisibility()!=GONE) {
            setVisibility(View.GONE);
            if (mHiddenListener != null) {
                mHiddenListener.onHidden();
            }
        }
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private void setProgress() {
        if (mPlayer == null || mDragging
                || !mPlayer.isPlaying())
            return;// 0;

        long currentTime = mPlayer.getCurrentTime()/1000L;
        long duration = mPlayer.getDuration()/1000L;
        if (mProgress != null) {

            mProgress.setProgress((int) currentTime);

        }

        mDuration = duration;

        if (mEndTime != null)
            mEndTime.setText(generateTime(mDuration));
        if (mCurrentTime != null)
            mCurrentTime.setText(generateTime(currentTime));


    }



    @Override
    public void setEnabled(boolean enabled) {
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    public interface OnShownListener {
        public void onShown();
    }

    public interface OnHiddenListener {
        public void onHidden();
    }


    public static String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }
}
