package liuxiaocong.com.camerauploadandplay.common;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public abstract class CameraUtil {
    private static final String TAG = "CameraUtil";

    public static String translatePreviewFormat(int supportedPreviewFormat) {
        switch (supportedPreviewFormat) {
            case ImageFormat.JPEG:
                return "ImageFormat.JPEG";
            case ImageFormat.NV16:
                return "ImageFormat.NV16";
            case ImageFormat.NV21:
                return "ImageFormat.NV21";
            case ImageFormat.RAW10:
                return "ImageFormat.RAW10";
            case ImageFormat.RAW_SENSOR:
                return "ImageFormat.RAW_SENSOR";
            case ImageFormat.RGB_565:
                return "ImageFormat.RGB_565";
            case ImageFormat.UNKNOWN:
                return "ImageFormat.UNKNOWN";
            case ImageFormat.YUV_420_888:
                return "ImageFormat.YUV_420_888";
            case ImageFormat.YUY2:
                return "ImageFormat.YUY2";
            case ImageFormat.YV12:
                return "ImageFormat.YV12";
            default:
                return "xxxxxxxxdefault";
        }
    }


    public static int getCameraDisplayOrientation(Activity activity, @SuppressWarnings("deprecation") Camera.CameraInfo cameraInfo) {
        int degrees = getCameraDisplayOrientation_1(activity, cameraInfo);

        String MANUFACTURER = android.os.Build.MANUFACTURER;
        int GINGERBREAD_MR1 = android.os.Build.VERSION_CODES.GINGERBREAD_MR1;
        String MODEL = android.os.Build.MODEL;
        String RELEASE = Build.VERSION.RELEASE;

        Log.d(TAG, "MANUFACTURER = " + MANUFACTURER + "; GINGERBREAD_MR1 = " + GINGERBREAD_MR1 + "; MODEL = " + MODEL + "; RELEASE = " + RELEASE + "; Degrees =" + degrees);

        //noinspection deprecation
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

            if ((MANUFACTURER.toLowerCase().equals("HTC".toLowerCase()) && MODEL.toLowerCase().equals("HTC Salsa C510e".toLowerCase())
                    // &&
                    // Build.VERSION.RELEASE.toLowerCase().equals("2.3.3".toLowerCase())
            )
                    || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("Galaxy Y Duos".toLowerCase())
                    // &&
                    // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
            )

                    || (MANUFACTURER.toLowerCase().equals("LGE".toLowerCase()) && MODEL.toLowerCase().equals("LG-P500".toLowerCase())
                    // &&
                    // Build.VERSION.RELEASE.toLowerCase().equals("2.3.3".toLowerCase())
            )) {
                degrees = (degrees + 90 + 360) % 360;
            }
//
//            || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("SM-G920I".toLowerCase())
//                    // &&
//                    // Build.VERSION.RELEASE.toLowerCase().equals("2.3.3".toLowerCase())
//            )
        } else //noinspection deprecation
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if ((MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("Galaxy Y".toLowerCase())
                        // &&
                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
                )
                        || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("Galaxy Y Duos".toLowerCase())
                        // &&
                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
                ) || (MANUFACTURER.toLowerCase().equals("LGE".toLowerCase()) && MODEL.toLowerCase().equals("LG-P500".toLowerCase())
                        // &&
                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.3".toLowerCase())
                ) || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("Galaxy Young Pro".toLowerCase())
                        // &&
                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
                ) || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("GT-S5360".toLowerCase())
                        // &&
                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
                ) || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("Galaxy Ace 2".toLowerCase())
                        // &&
                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
                )) {
                    degrees = (degrees + 90 + 360) % 360;
                }

//                || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) && MODEL.toLowerCase().equals("SM-G920I".toLowerCase())
//                        // &&
//                        // Build.VERSION.RELEASE.toLowerCase().equals("2.3.3".toLowerCase())
//                )
                // || (MANUFACTURER.toLowerCase().equals("samsung".toLowerCase()) &&
                // MODEL.toLowerCase().equals("GT-S6102".toLowerCase())
                // // &&
                // //
                // Build.VERSION.RELEASE.toLowerCase().equals("2.3.6".toLowerCase())
                // )
            }

        Log.d(TAG, "After Degrees =" + degrees);
        return degrees;
    }

    private static int getCameraDisplayOrientation_1(Activity activity, @SuppressWarnings("deprecation") Camera.CameraInfo cameraInfo) {
        // android.hardware.Camera.CameraInfo info =
        // new android.hardware.Camera.CameraInfo();
        // android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default: {
                Log.e(TAG, "un deal with handler task");
            }
            break;
        }

        int result;
        //noinspection deprecation
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        return result;
    }
}
