package mozat.rings.libffmpeg;

import android.view.Surface;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ken on 3/24/2016.
 */
public interface IGlObject {

    public static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    void loadGLTexture(GL10 gl);

    void destroy();

    /**
     * gets the surface. MUST be called afte {@link #loadGLTexture(GL10)}
     * @return
     */
    Surface getSurface();

    void updateFrameIfAvailable();

    void draw(final GL10 gl);

    /**
     * gets the relocate position where this object demands.
     */
    void getRelocation(float[] pos);

    /**
     * gets the limitation of the rotation around x axis.
     * @param xLimit [0] is the up side, [1] is the down side.
     */
    void getAxisXRotationLimit(float[] xLimit);

    float getInitiateXRotation();

    float getFieldOfViewInYDirection();

    /**
     * indicates whether sensor rotation is supported.
     * @return
     */
    boolean isSensorSupported();
}
