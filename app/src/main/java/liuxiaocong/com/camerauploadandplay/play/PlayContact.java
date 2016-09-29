package liuxiaocong.com.camerauploadandplay.play;

import mozat.rings.libffmpeg.FFMPEGPlayer;

/**
 * Created by LiuXiaocong on 9/29/2016.
 */
public interface PlayContact {
    interface View {
        FFMPEGPlayer getFFMPEGPlayer();
        void setPlayerListener(FFMPEGPlayer.FFMPEGPlayerListener listener);
        void clear();
    }

    interface Presenter {
        void setView(View view);

        void startPlay();

        void clear();
    }
}
