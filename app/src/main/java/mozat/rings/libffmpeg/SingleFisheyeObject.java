package mozat.rings.libffmpeg;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ken on 3/24/2016.
 */
public class SingleFisheyeObject implements IGlObject {

    /** Each vertex is made up of 3 points, x, y, z. */
    private static final int AMOUNT_OF_NUMBERS_PER_VERTEX_POINT = 3;

    /** Each texture point is made up of 2 points, x, y (in reference to the texture being a 2D image). */
    private static final int AMOUNT_OF_NUMBERS_PER_TEXTURE_POINT = 2;

    private static final float FIELD_OF_VIEW_IN_Y = 80.0f;
    private static final float[] x_limits = new float[2];

    /** Buffer holding the vertices. */
    private final List<FloatBuffer> mVertexBuffer = new ArrayList<>();

    /** Buffer holding the texture coordinates. */
    private final List<FloatBuffer> mTextureBuffer = new ArrayList<>();

    /** The texture pointer. */
    private final int[] mTextures = new int[1];

    private SurfaceTexture mSurfaceTextture = null;
    private volatile boolean mFrameAvailable = false;

    private int mFieldOfView = 180;
    private boolean mUpsideDown = true;
//    private final static double PI_2 = Math.PI / 2d;
//    private final static double PI_3 = Math.PI * 1.5d;


    public SingleFisheyeObject(final int fieldOfView, final int stepAngle, final boolean upsideDown) {
        final float radius = 80.0f;
        mFieldOfView = fieldOfView;
        mUpsideDown = upsideDown;
        if (upsideDown) {
            // clockwise is positive
            x_limits[0] = -mFieldOfView/2.0f + FIELD_OF_VIEW_IN_Y/2.0f;
            x_limits[1] = 0.0f;
        } else {
            // clockwise is positive
            x_limits[0] = -180.0f;
            x_limits[1] = -180.0f + mFieldOfView/2.0f - FIELD_OF_VIEW_IN_Y/2.0f;
        }

        int vStart = upsideDown?-90:(90-mFieldOfView/2);
        float[] verticesLower = getLatitudeVertices(radius, vStart, stepAngle);
        float[] textureVerticesLower = getTextureVertices(vStart, vStart, stepAngle);
        int vEnd = upsideDown?(mFieldOfView/2-90) : 90;
        for (int vAngle=vStart+stepAngle; vAngle<=vEnd; vAngle+=stepAngle) {
            float[] verticesUpper = getLatitudeVertices(radius, vAngle, stepAngle);
            float[] textureVerticesUpper = getTextureVertices(vStart, vAngle, stepAngle);
//            int vPos = 0;
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(verticesLower.length * 2 * Float.SIZE);
            byteBuffer.order(ByteOrder.nativeOrder());
            FloatBuffer fb = byteBuffer.asFloatBuffer();

            ByteBuffer txtByteBuffer = ByteBuffer.allocateDirect(textureVerticesLower.length * 2 * Float.SIZE);
            txtByteBuffer.order(ByteOrder.nativeOrder());
            FloatBuffer tb = txtByteBuffer.asFloatBuffer();

            for (int i=0;i<verticesUpper.length;i+=AMOUNT_OF_NUMBERS_PER_VERTEX_POINT) {
//                verticesBuffer[vPos++] = verticesUpper[i];
//                verticesBuffer[vPos++] = verticesLower[i];
                fb.put(verticesUpper[i]);
                fb.put(verticesUpper[i+1]);
                fb.put(verticesUpper[i+2]);
                fb.put(verticesLower[i]);
                fb.put(verticesLower[i+1]);
                fb.put(verticesLower[i+2]);

//                Log.i("fisheye", "Vertices starts from VAngle "
//                        + vAngle + ": ("
//                        + verticesUpper[i] + ", "
//                        + verticesUpper[i+1] + ", "
//                        + verticesUpper[i+2] + "), ("
//                        + verticesLower[i] + ", "
//                        + verticesLower[i+1] + ", "
//                        + verticesLower[i+2] + ")"
//                );
            }
            fb.position(0);
            mVertexBuffer.add(fb);

            for (int i=0; i<textureVerticesUpper.length; i+=AMOUNT_OF_NUMBERS_PER_TEXTURE_POINT) {
                tb.put(textureVerticesUpper[i+0]);
                tb.put(textureVerticesUpper[i+1]);
                tb.put(textureVerticesLower[i+0]);
                tb.put(textureVerticesLower[i+1]);

//                Log.i("fisheye", "Textures starts from VAngle "
//                        + vAngle + ", hAngle " + i
//                        + " : ("
//                        + textureVerticesUpper[i+0] + ", "
//                        + textureVerticesUpper[i+1] + "), ("
//                        + textureVerticesLower[i+0] + ", "
//                        + textureVerticesLower[i+1] + ")"
//                );
            }
//            Log.i("Sphere3", "===================================================");
            tb.position(0);
            mTextureBuffer.add(tb);

            verticesLower = verticesUpper;
            textureVerticesLower = textureVerticesUpper;
        }
    }

    private float[] getLatitudeVertices(float radius, int vAngle, int stepAngle) {
        float[] buffer = new float[(360/stepAngle+1)*AMOUNT_OF_NUMBERS_PER_VERTEX_POINT];
        int index = 0;
        for (int hAngle=0; hAngle<360; hAngle+=stepAngle) {
            buffer[index++]= (float) (radius* Math.cos(Math.toRadians(vAngle))* Math.cos(Math.toRadians(hAngle)));
            buffer[index++]= (float) (radius* Math.cos(Math.toRadians(vAngle))* Math.sin(Math.toRadians(hAngle)));
            buffer[index++]= (float) (radius* Math.sin(Math.toRadians(vAngle)));
        }
        buffer[index++]=buffer[0];
        buffer[index++]=buffer[1];
        buffer[index++]=buffer[2];
        return buffer;
    }

    private float[] getTextureVertices(int vStartAngle, int vAngle, int stepAngle) {
        float[] buffer = new float[(360/stepAngle+1)*AMOUNT_OF_NUMBERS_PER_TEXTURE_POINT];
        // for texture, radius = 0.5
//        double shadowLen = 0.5*Math.cos(Math.toRadians(vAngle));
//        double shadowLen = (90.0 - Math.abs(vAngle)) / 90.0 * 0.5;
        double shadowLen;
        if (mUpsideDown) {
            shadowLen = (vAngle - vStartAngle) / (mFieldOfView / 2.0) * 0.5;
        } else {
            shadowLen = (1.0f - (vAngle - vStartAngle) / (mFieldOfView / 2.0)) * 0.5;
        }
        int index = 0;
        for (int hAngle=0; hAngle<360; hAngle+=stepAngle) {
            buffer[index++] = 0.5f - (float)(Math.cos(Math.toRadians(hAngle)) * shadowLen);
            buffer[index++] = 0.5f + (mUpsideDown?1:-1) * (float)(Math.sin(Math.toRadians(hAngle)) * shadowLen);
        }
        buffer[index++] = buffer[0];
        buffer[index++] = buffer[1];

        return buffer;
    }

//    private void getTextureVertex(float vAgnle, float hAngle, float[] result) {
//        // for texture, radius = 0.5
//        double shadowLen = 0.5*Math.cos(Math.toRadians(vAgnle));
//        double hRadians;
//        if (hAngle>=0 & hAngle <90) {
//            hRadians = Math.toRadians(hAngle);
//            result[0] = 0.5f + (float)(Math.sin(hRadians) * shadowLen);
//            result[1] = 0.5f + (float)(Math.cos(hRadians) * shadowLen);
//        }
//
//    }


    @Override
    public void destroy() {
        if (mSurfaceTextture!=null) {
            mSurfaceTextture.setOnFrameAvailableListener(null);
            mSurfaceTextture.release();
            mSurfaceTextture = null;
        }
    }

    @Override
    public void loadGLTexture(GL10 gl) {
// Generate one texture pointer, and bind it to the texture array.
        gl.glGenTextures(1, this.mTextures, 0);
//        gl.glBindTexture(GL10.GL_TEXTURE_2D, this.mTextures[0]);
        gl.glBindTexture(GL_TEXTURE_EXTERNAL_OES, this.mTextures[0]);
        Log.e("Sphere", "====== loadGLTexture threadId=" + Thread.currentThread().getId());

        // Create nearest filtered texture.
//        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
//        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        //
        // original
        //
        // // Use Android GLUtils to specify a two-dimensional texture image from our bitmap.
//        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), texture);
//        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
//        bitmap.recycle();
        //
        // end of original
        //

//        gl.glTexParameterx(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
//        gl.glTexParameterx(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
        mSurfaceTextture = new SurfaceTexture(this.mTextures[0]);
        mSurfaceTextture.setOnFrameAvailableListener(mOnFrameAvailable);
    }

    private SurfaceTexture.OnFrameAvailableListener mOnFrameAvailable = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mFrameAvailable = true;
        }
    };

    @Override
    public Surface getSurface() {
        return new Surface(mSurfaceTextture);
    }

    @Override
    public void updateFrameIfAvailable() {
        if (mFrameAvailable) {
            mFrameAvailable = false;
//            Log.e("Sphere", "====== updateTextImage threadId=" + Thread.currentThread().getId());
            mSurfaceTextture.updateTexImage();
        }
    }

    @Override
    public void draw(GL10 gl) {
        // bind the previously generated texture.
//        gl.glBindTexture(GL10.GL_TEXTURE_2D, this.mTextures[0]);
        gl.glBindTexture(GL_TEXTURE_EXTERNAL_OES, this.mTextures[0]);

        // Point to our buffers.
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        // Set the face rotation, clockwise in this case.
        gl.glFrontFace(GL10.GL_CW);

        // Point to our vertex buffer.
        int cnt = mVertexBuffer.size();
        for (int i = 0; i < cnt; i++) {
            gl.glVertexPointer(AMOUNT_OF_NUMBERS_PER_VERTEX_POINT, GL10.GL_FLOAT, 0, this.mVertexBuffer.get(i));
            gl.glTexCoordPointer(AMOUNT_OF_NUMBERS_PER_TEXTURE_POINT, GL10.GL_FLOAT, 0, this.mTextureBuffer.get(i));

            // Draw the vertices as triangle strip.
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, this.mTextureBuffer.get(i).capacity() / AMOUNT_OF_NUMBERS_PER_VERTEX_POINT);
        }

        // Disable the client state before leaving.
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    @Override
    public void getRelocation(float[] pos) {
        pos[0] = 0;
        pos[1] = 0;
        pos[2] = 0;
    }

    @Override
    public void getAxisXRotationLimit(float[] xLimit) {
        xLimit[0] = x_limits[0];
        xLimit[1] = x_limits[1];
    }

    @Override
    public float getInitiateXRotation() {
        return mUpsideDown? x_limits[0] : x_limits[1];
    }

    @Override
    public boolean isSensorSupported() {
        return false;
    }

    @Override
    public float getFieldOfViewInYDirection() {
        return FIELD_OF_VIEW_IN_Y;
    }
}
