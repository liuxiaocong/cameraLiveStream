package liuxiaocong.com.camerauploadandplay.record;

import android.view.TextureView;
import android.view.ViewGroup;

import mozat.rings.libffmpeg.FFMPEGPlayer;

/**
 * Created by LiuXiaocong on 9/29/2016.
 */
public interface RecordContact {
    interface View {
        void clear();

        void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener);

        TextureView getTextureView();

        ViewGroup getUploadPanelParent();

        int getUploadPanelParentWidth();

        int getUploadPanelParentHeight();

        String getUploadUrl();
    }

    interface Presenter {
        void setView(View view);

        void startPreview();

        void stopUpload();

        void startUpload();

        void clear();
    }
}
