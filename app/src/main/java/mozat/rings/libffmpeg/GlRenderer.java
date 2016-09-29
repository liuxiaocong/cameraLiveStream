package mozat.rings.libffmpeg;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ken on 3/24/2016.
 */
public class GlRenderer implements GLSurfaceView.Renderer {
    private final static String LOG_TAG = "GlRenderer";

//    /** Tilt the spheres a little. */
//    private static final int AXIAL_TILT_DEGREES = 0;

    /** Clear colour, alpha component. */
    private static final float CLEAR_RED = 0.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_GREEN = 0.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_BLUE = 0.0f;

    /** Clear colour, alpha component. */
    private static final float CLEAR_ALPHA = 1.0f;

//    /** Perspective setup, field of view component. */
//    private static final float FIELD_OF_VIEW_Y = 55.0f;

    /** Perspective setup, near component. */
    private static final float Z_NEAR = 0.1f;

    /** Perspective setup, far component. */
    private static final float Z_FAR = 100.0f;

//    /** Object distance on the screen. move it back a bit so we can see it! */
//    private static final float OBJECT_DISTANCE = 0.0f;

//    private final float RADIUS = 80.0f;

//    private final boolean TOUCH_TO_MOVE = true;

    /** The earth's sphere. */
    private final IGlObject mGlObject;

    private boolean mSensorSupported = false;

    private final float[] mRelocation = new float[3];

    private final float[] mRotationXLimit = new float[2];

    /** The context. */
    private Context mContext;

    /** The rotation angle, with Z axis, azimuth*/
    private float mRotationAngleZ;
    /** The rotation angle, with X axis, pitch */
    private float mRotationAngleX;

    private float mDownX = 0.0f;
    private float mDownY = 0.0f;
    private float mDownRotationX;
    private float mDownRotationZ;
    private float mPerimeter;

    private final float[] mOriginalRotationMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];
    private int mScreenOrientation = 0;

    private boolean mSensorEnabled = false;

    private SensorManager mSensorManager = null;
    private Sensor mRotationVectorSensor = null;

    private GlRendererCallback mRendererCallback = null;

    public interface GlRendererCallback {

        void onGlSurfaceCreated(Surface sf);

    }

    protected static final GlRenderer createSphereRenderer(Context context) {
        return new GlRenderer(context, new SphereObject(10));
    }

    protected static final GlRenderer createSingleFisheyeRenderer(Context context, boolean upsidedown, int fov) {
        return new GlRenderer(context, new SingleFisheyeObject(fov, 10, upsidedown));
    }

    /**
     * Constructor to set the handed over context.
     * @param context The context.
     */
    private GlRenderer(final Context context, IGlObject glObj) {
        mContext = context;
//        this.mGlObject = new Sphere(3, 2);
//        this.mGlObject = new SphereObject(10);
//        mGlObject = new SingleFisheyeObject(240, 10, true);
        mGlObject = glObj;
        mGlObject.getAxisXRotationLimit(mRotationXLimit);
        mRotationAngleZ = 0.0f;
        mRotationAngleX = mGlObject.getInitiateXRotation();
        mSensorSupported = mGlObject.isSensorSupported();

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // initialize the rotation matrix to identity
        mOriginalRotationMatrix[ 0] = 1;
        mOriginalRotationMatrix[ 4] = 1;
        mOriginalRotationMatrix[ 8] = 1;
        mOriginalRotationMatrix[12] = 1;
        System.arraycopy(mOriginalRotationMatrix, 0, mRotationMatrix, 0, mOriginalRotationMatrix.length);
    }

    public void destroy() {
        onStop();
        if (mGlObject!=null) {
            mGlObject.destroy();
        }
        mRendererCallback = null;
        mSensorManager = null;
        mRotationVectorSensor = null;
        mContext = null;
    }

    public void onResume() {
        if (mSensorSupported && mSensorEnabled) {
            mSensorManager.registerListener(mSensorEventListener, mRotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void onStop() {
        if (mSensorSupported && mSensorEnabled) {
            mSensorManager.unregisterListener(mSensorEventListener, mRotationVectorSensor);
        }
    }

    public void setSensorEnable(boolean b) {
        if (mSensorEnabled==b) {
            return;
        }

        mSensorEnabled = b;
        if (mSensorSupported && mSensorEnabled) {
            mSensorManager.registerListener(mSensorEventListener, mRotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            mSensorManager.unregisterListener(mSensorEventListener, mRotationVectorSensor);
        }
    }

    public boolean getSensorEnable() {
        return mSensorEnabled;
    }

    public boolean getSensorWorking() {
        return mSensorSupported && mSensorEnabled;
    }

    public void setGlRendererCallback(GlRendererCallback callback) {
        mRendererCallback = callback;
    }

    public GlRendererCallback getGlRendererCallback() {
        return mRendererCallback;
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        mGlObject.updateFrameIfAvailable();
        gl.glClearColor(CLEAR_RED, CLEAR_GREEN, CLEAR_BLUE, CLEAR_ALPHA);
        gl.glClearDepthf(1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        mGlObject.getRelocation(mRelocation);
        gl.glTranslatef(mRelocation[0], mRelocation[1], mRelocation[2]);
        if (mSensorSupported && mSensorEnabled) {
            gl.glMultMatrixf(mRotationMatrix, 0);
        } else {
            gl.glRotatef(mRotationAngleX, 1, 0, 0);
            gl.glRotatef(mRotationAngleZ, 0, 0, 1);
        }
        mGlObject.draw(gl);
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        final float aspectRatio = (float) width / (float) (height == 0 ? 1 : height);

//        float oriPerimeter = (float) (2.0f * Math.PI * RADIUS);
//        mPerimeter = height * 360.0f / FIELD_OF_VIEW_Y;
        double screenRadius = height / 2.0 / Math.sin(Math.toRadians(mGlObject.getFieldOfViewInYDirection() / 2.0));
        mPerimeter = (float) (2.0 * Math.PI * screenRadius);

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU.gluPerspective(gl, mGlObject.getFieldOfViewInYDirection(), aspectRatio, Z_NEAR, Z_FAR);
//        gl.glFrustumf(-aspectRatio, aspectRatio, -1, 1, 1, 10);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        mScreenOrientation = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        String strOri = "unknown";
        switch (mScreenOrientation) {
            case Surface.ROTATION_0 : {
                strOri = "rotation 0";
            }
            break;
            case Surface.ROTATION_90 : {
                strOri = "rotation 90";
            }
            break;
            case Surface.ROTATION_270 : {
                strOri = "rotation 270";
            }
            break;
            case Surface.ROTATION_180 : {
                strOri = "rotation 180";
            }
            break;
        }
        Log.i(LOG_TAG, "Screen Orientation: " + strOri);
    }

    @Override
    public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
        this.mGlObject.loadGLTexture(gl);//, this.mContext, R.drawable.e2);
        //
        // here we callback not notify the surface is ready.
        //
        if (mRendererCallback!=null) {
            mRendererCallback.onGlSurfaceCreated(mGlObject.getSurface());
        }
//        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(IGlObject.GL_TEXTURE_EXTERNAL_OES);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glClearColor(CLEAR_RED, CLEAR_GREEN, CLEAR_BLUE, CLEAR_ALPHA);
        gl.glClearDepthf(1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    public void onTouch(int event, float x, float y) {
        if (mSensorSupported && mSensorEnabled) {
            return;
        }

        switch (event) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = x;
                mDownY = y;
                mDownRotationX = mRotationAngleX;
                mDownRotationZ = mRotationAngleZ;
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                mGlObject.getAxisXRotationLimit(mRotationXLimit);
                float newX = mDownRotationX - (y-mDownY) * 360.0f / mPerimeter;
                if (newX>mRotationXLimit[1]) {
                    newX = mRotationXLimit[1];
                } else if (newX<mRotationXLimit[0]) {
                    newX = mRotationXLimit[0];
                }
                mRotationAngleZ = mDownRotationZ - (x-mDownX) * 360.0f / mPerimeter;
                mRotationAngleX = newX;
            }
            break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {

            }
            break;
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!mSensorEnabled) {
                return;
            }

            if (event.sensor.getType()== Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(mOriginalRotationMatrix, event.values);

                int axisX, axisY;
                switch (mScreenOrientation) {
                    default:
                    case Surface.ROTATION_0 : {
                        axisX = SensorManager.AXIS_X;
                        axisY = SensorManager.AXIS_Y;
                    }
                    break;
                    case Surface.ROTATION_90: {
                        axisX = SensorManager.AXIS_Y;
                        axisY = SensorManager.AXIS_MINUS_X;
                    }
                    break;
                    case Surface.ROTATION_180: {
                        axisX = SensorManager.AXIS_MINUS_X;
                        axisY = SensorManager.AXIS_MINUS_Y;
                    }
                    break;
                    case Surface.ROTATION_270: {
                        axisX = SensorManager.AXIS_MINUS_Y;
                        axisY = SensorManager.AXIS_X;
                    }
                    break;
                }
                SensorManager.remapCoordinateSystem(mOriginalRotationMatrix,
                        axisX, axisY,
                        mRotationMatrix);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
}
