package liuxiaocong.com.camerauploadandplay.common;

public class YuvHelper {
    public static void rotateNV21Degree90(
            byte[] src, byte[] dst,
            int srcWidth, int srcHeight) {
        int fs = srcWidth * srcHeight;
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < srcWidth; x++) {
            for (int y = srcHeight - 1; y >= 0; y--) {
                dst[i] = src[y * srcWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = dst.length - 1;
        for (int x = srcWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < srcHeight / 2; y++) {
                dst[i] = src[fs + (y * srcWidth) + x];
                i--;
                dst[i] = src[fs + (y * srcWidth) + x - 1];
                i--;
            }
        }
    }

    public static void rotateNV21Degree180(
            byte[] src, byte[] dst,
            int srcWidth, int srcHeight) {
        int fs = srcWidth * srcHeight;
        int i;
        int count = 0;
        for (i = fs - 1; i >= 0; i--) {
            dst[count] = src[i];
            count++;
        }

        for (i = dst.length - 1; i >= fs; i -= 2) {
            dst[count++] = src[i - 1];
            dst[count++] = src[i];
        }
    }

    public static void rotateNV21Degree270(
            byte[] src, byte[] dst,
            int srcWidth, int srcHeight) {
        int fs = srcWidth * srcHeight;
        // Rotate the Y luma
        int i = 0;
        for (int x = srcWidth - 1; x >= 0; x--) {
            for (int y = 0; y < srcHeight; y++) {
                dst[i] = src[y * srcWidth + x];
                i++;
            }

        }
        // Rotate the U and V color components
        i = fs;
        for (int x = srcWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < srcHeight / 2; y++) {
                dst[i] = src[fs + (y * srcWidth) + (x - 1)];
                i++;
                dst[i] = src[fs + (y * srcWidth) + x];
                i++;
            }
        }
    }

    public static void nv21ToYUV420p(byte[] input, byte[] output, int width, int height, boolean horizontalMirror) {

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xi = x, yi = y;
                int xo = x, yo = y;
                int w = width, h = height;

                if (horizontalMirror) {
                    // horizontal flip
                    xi = w - xi - 1;
                }
                // vertical flip
                // yi = h - yi - 1;
                //

                output[w * yo + xo] = input[w * yi + xi];

                int fs = w * h;
                int qs = (fs >> 2);
                xi = (xi >> 1);
                yi = (yi >> 1);

                xo = (xo >> 1);
                yo = (yo >> 1);

                w = (w >> 1);
                h = (h >> 1);
                // adjust for interleave here
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + w * yo + xo;
                // and here
                int vi = ui + 1;
                int vo = qs + uo;
                output[uo] = input[vi];
                output[vo] = input[ui];
            }
        }
    }

    public static void nv21HorizontalMirrorChange(byte[] input, byte[] output, int width, int height) {

        int k = 0;

        for (int r = 0; r < height; r++) {

            for(int l = 0; l < width ; l++)
            {

                output[k++] = input[(width-l) + r * width - 1];

            }


        }

        for(int uR = height;uR< height + height/2 ; uR++)
        {
            for(int uL = 0; uL < width; uL = uL + 2)
            {

                output[k++] = input[(width-uL - 2) + uR * width];
                output[k++] = input[(width-uL - 1) + uR * width];

            }
        }



    }
}