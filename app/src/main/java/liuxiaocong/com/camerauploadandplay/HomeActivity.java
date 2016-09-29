package liuxiaocong.com.camerauploadandplay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import liuxiaocong.com.camerauploadandplay.play.PlayContact;
import liuxiaocong.com.camerauploadandplay.play.PlayPresenterImpl;
import liuxiaocong.com.camerauploadandplay.record.RecordContact;
import liuxiaocong.com.camerauploadandplay.record.RecordPresenterImpl;
import mozat.rings.libffmpeg.FFMPEGPlayer;

public class HomeActivity extends AppCompatActivity implements PlayContact.View, RecordContact.View {
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

    private FFMPEGPlayer.EVIDEO_LAYOUT mTargetLayout = FFMPEGPlayer.EVIDEO_LAYOUT.EFIT_HEIGHT_CENTER_WIDTH;
    @BindView(R.id.player)
    public FFMPEGPlayer mFFMPEGPlayer;

    PlayContact.Presenter mPlayPresenter;
    RecordContact.Presenter mRecordPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setKeepScreenStatus(true);
        ButterKnife.bind(this);
        mEditText.setText(RecordPresenterImpl.mUploadUrl);


        mFFMPEGPlayer.setRenderMode(FFMPEGPlayer.ERENDER_MODE.ENORMAL_TEXTURE_VIEW);
        mFFMPEGPlayer.setVideoLayout(mTargetLayout, false);

        mPlayPresenter = new PlayPresenterImpl();
        mPlayPresenter.setView(this);

        mRecordPresenter = new RecordPresenterImpl(this);
        mRecordPresenter.setView(this);
    }


    @OnClick(R.id.play)
    public void onClickPlay(View view) {
        if (mPlayPresenter != null) {
            mPlayPresenter.startPlay();
        }
    }


    @OnClick(R.id.preview)
    public void onClickPreview(View view) {
        if (mRecordPresenter != null) {
            mRecordPresenter.startPreview();
        }
    }


    @OnClick(R.id.upload)
    public void onClickUpload(View view) {
        if (mRecordPresenter != null) {
            mRecordPresenter.startUpload();
        }
    }

    public void setKeepScreenStatus(boolean isOn) {
        if (isOn) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.destroy();
        }
        if (mPlayPresenter != null) {
            mPlayPresenter.clear();
        }
        if (mRecordPresenter != null) {
            mRecordPresenter.clear();
        }
        setKeepScreenStatus(false);
    }


    @Override
    public FFMPEGPlayer getFFMPEGPlayer() {
        return mFFMPEGPlayer;
    }

    @Override
    public void setPlayerListener(FFMPEGPlayer.FFMPEGPlayerListener listener) {
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.setPlayerListener(listener);
        }
    }

    @Override
    public void clear() {
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.setPlayerListener(null);
            mFFMPEGPlayer.destroy();
            mFFMPEGPlayer = null;
        }
    }

    @Override
    public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        if (mCameraTextureView != null) {
            mCameraTextureView.setSurfaceTextureListener(listener);
        }
    }

    @Override
    public TextureView getTextureView() {
        return mCameraTextureView;
    }

    @Override
    public ViewGroup getUploadPanelParent() {
        return mUploadWrap;
    }

    @Override
    public int getUploadPanelParentWidth() {
        if (mUploadWrap == null) return 0;
        return mUploadWrap.getWidth();
    }

    @Override
    public int getUploadPanelParentHeight() {
        if (mUploadWrap == null) return 0;
        return mUploadWrap.getHeight();
    }

    @Override
    public String getUploadUrl() {
        return mEditText.getText().toString();
    }
}
