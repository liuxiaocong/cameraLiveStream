package liuxiaocong.com.camerauploadandplay.record;

import android.media.AudioManager;

import liuxiaocong.com.camerauploadandplay.play.PlayContact;
import mozat.rings.libffmpeg.FFMPEGPlayer;

/**
 * Created by LiuXiaocong on 9/29/2016.
 */
public class RecordPresenterImpl implements RecordContact.Presenter {

    private RecordContact.View mView;

    @Override
    public void setView(RecordContact.View view) {
        mView = view;
    }

    @Override
    public void startPreview() {

    }

    @Override
    public void stopUpload() {

    }

    @Override
    public void startUpload() {

    }

    @Override
    public void clear() {

    }
}
