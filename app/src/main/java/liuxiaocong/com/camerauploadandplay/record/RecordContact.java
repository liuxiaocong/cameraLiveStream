package liuxiaocong.com.camerauploadandplay.record;

import mozat.rings.libffmpeg.FFMPEGPlayer;

/**
 * Created by LiuXiaocong on 9/29/2016.
 */
public interface RecordContact {
    interface View {
        void clear();
    }

    interface Presenter {
        void setView(View view);

        void startPreview();

        void stopUpload();

        void startUpload();

        void clear();
    }
}
