package jp.anzx.stsclient;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.Date;

public class ScreenshotTaker {

    public static final String TAG = "ScreenshotTaker";

    private int resultCode;
    private Intent data;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    int mWidth;
    int mHeight;
    int mDensity;

    private ImageReader imageReader;
    private DisplayMetrics metrics;
    private Handler imageHandler;

    private Context context;

    private static int navigationBarHeight = 0;
    private static int statusBarHeight = 0;

    private ScreenshotTakerListener screenshotTakerListener;

    interface ScreenshotTakerListener{
        public void onImageReady(Bitmap image);
    }

    public void setScreenshotTakerListener(ScreenshotTakerListener screenshotTakerListener) {
        this.screenshotTakerListener = screenshotTakerListener;
    }

    public void setNavigationBarHeight(int navigationBarHeight) {
        this.navigationBarHeight = navigationBarHeight;
    }

    public void setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
    }

    public ScreenshotTaker(Context context, int mediaResultCode, Intent mediaData, int screenWidth, int screenHeight){

        this.context = context;

        resultCode = mediaResultCode;
        data = mediaData;

        //screen
        metrics = context.getResources().getDisplayMetrics();

        mWidth = screenWidth;
        mHeight = screenHeight;
        mDensity = metrics.densityDpi;
    }

    public void takeScreenshot(){


        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

        //screenshot
        imageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 5);
        //handler = new Handler();
        imageHandler = new Handler(Looper.getMainLooper());

        mediaProjection.createVirtualDisplay("screen-mirror",
                mWidth,
                mHeight,
                mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                        | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                        | DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                //DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                imageHandler);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                reader.setOnImageAvailableListener(null, imageHandler);

                Image image = reader.acquireLatestImage();

                final Image.Plane[] planes = image.getPlanes();
                final ByteBuffer buffer = planes[0].getBuffer();

                //ByteBuffer buffer = ByteBuffer.wrap(os.toByteArray());
                //buffer.rewind();

                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * metrics.widthPixels;

                // create bitmap
                Bitmap bmp = Bitmap.createBitmap(reader.getWidth(), reader.getHeight(), Bitmap.Config.ARGB_8888);

                //так надо
                try {
                    int a = 1/0;
                    bmp.copyPixelsFromBuffer(buffer);
                }
                catch (Exception e){
                    Log.e(TAG, e.getMessage());

                    int offset = 0;

                    for (int i = 0; i < mHeight; ++i) {
                        for (int j = 0; j < mWidth; ++j) {
                            int pixel = 0;
                            pixel |= (buffer.get(offset) & 0xff) << 16;     // R
                            pixel |= (buffer.get(offset + 1) & 0xff) << 8;  // G
                            pixel |= (buffer.get(offset + 2) & 0xff);       // B
                            pixel |= (buffer.get(offset + 3) & 0xff) << 24; // A
                            bmp.setPixel(j, i, pixel);
                            offset += pixelStride;
                        }
                        offset += rowPadding;
                    }

                }

                //bmp = Bitmap.createScaledBitmap(bmp, metrics.widthPixels + (int) ((float) rowPadding / (float) pixelStride), metrics.heightPixels, false);

                //Rect rect = image.getCropRect();
                //bmp = Bitmap.createBitmap(bmp, rect.left, rect.top, rect.width(), rect.height());

                Log.i(TAG, bmp.getHeight() + " " + bmp.getWidth() + " " + bmp.getHeight() * bmp.getWidth() * 3);
                //Log.i(TAG, buffer.toString());

                image.close();
                reader.close();

                //Bitmap realSizeBitmap = Bitmap.createBitmap(bmp, 0, 0, metrics.widthPixels, metrics.heightPixels/*bmp.getHeight()*/);
                //bmp.recycle();

                /* do something with [realSizeBitmap] */
                //saveBitmap(cropBitmap(realSizeBitmap));
                //saveBitmap(realSizeBitmap);

                //cut statusbar
                bmp = Bitmap.createBitmap(bmp, 0, statusBarHeight, bmp.getWidth(), bmp.getHeight() - statusBarHeight);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight() - navigationBarHeight);

                if(screenshotTakerListener != null)
                    screenshotTakerListener.onImageReady(bmp);

                //Translator.init() -> MainAct//
                //String recognised_text = Translator.extractText(result);

                //Toast.makeText(getApplicationContext(), recognised_text, Toast.LENGTH_LONG)
                //        .show();

                //Translator.translate(recognised_text, context);

                mediaProjection.stop();
            }
        }, imageHandler);


    }

    public static Bitmap cropBitmap(Bitmap bitmap, int startX, int startY, int endX, int endY){

        int width = 0, height = 0;
        int x = 0, y = 0;

        if(startX > endX){
            width = startX - endX;
            x = endX;
        }

        if(startY > endY){
            height = startY - endY;
            y = endY;
        }

        if(startX < endX){
            width = endX - startX;
            x = startX;
        }

        if(startY < endY){
            height = endY - startY;
            y = startY;
        }


        //y += statusBarHeight;
        //y += navigationBarHeight;

        return Bitmap.createBitmap(bitmap, x, y, width, height);
    }

    public void saveBitmap(Bitmap bitmap){
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        //save path
        String path = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

        try {

            File imageFile = new File(path);
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}