package com.ypc.datarecorder;

/**
 * Created by yangpc on 2018/7/3.
 */
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class CamCallbacks {

    private static long mLastTimestampNanos;
    public static final int INFO_VIEW_UPDATE_RATE = 10;

    static { mLastTimestampNanos = -1; }

    public static class ShutterCallback implements Camera.ShutterCallback {
        @Override
        public void onShutter() {
            mLastTimestampNanos = System.nanoTime();
        }
    }

    public static class PictureCallback implements Camera.PictureCallback {

        private final String TAG = "TAG/CameraIMU";

        private ShutterCallback mShutterCallback=new ShutterCallback();
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            long timestampMillis = mLastTimestampNanos;
            if ( Config.isCapturing())
                recordPicture(data, camera, timestampMillis);

            // Take pictures continuously
            camera.startPreview();
            if (Config.isCapturing())
                camera.takePicture(mShutterCallback, null, this);
        }

        private void recordPicture(byte[] data, Camera camera, long timestampMillis) {
            String filename = String.format(Locale.US, "%013d.jpg", timestampMillis);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    Config.getStorageDir() + File.separator + "IMG" + File.separator + filename);

            // Write file
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                Log.e(TAG, "recordPicture: " + e.getMessage());
            }
        }
    }

    public static class PreviewCallback implements Camera.PreviewCallback {
        private float mCurrentFPS = 0f;
        private int mLocalFrameCount = 0;

        private final String TAG = "TAG/CameraIMU";


        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            final long timestampNanos = System.nanoTime();
            final long timestampNanos= SystemClock.elapsedRealtimeNanos();
            final int frameW = camera.getParameters().getPreviewSize().width;
            final int frameH = camera.getParameters().getPreviewSize().height;
            if (Config.isCapturing()) {
                // Instantiate an AsyncTask to do the compressing and saving jobs
                // in order to prevent blocking
                new AsyncTask<byte[], Void, Void>() {
                    @Override
                    protected Void doInBackground(byte[]... params) {
                        byte[] data = params[0];
                        compressAndSaveAsJPEG(data, frameW, frameH, timestampNanos);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, data);
            }

            if (mLastTimestampNanos == -1) {
                mCurrentFPS = 0f;
                mLastTimestampNanos = timestampNanos;
            } else {
                ++mLocalFrameCount;
                if (mLocalFrameCount == INFO_VIEW_UPDATE_RATE) {
                    mCurrentFPS = INFO_VIEW_UPDATE_RATE * 1e9f / (timestampNanos - mLastTimestampNanos);
                    mLastTimestampNanos = timestampNanos;
                    mLocalFrameCount = 0;
                }
            }
        }

        private void compressAndSaveAsJPEG(byte[] data, int w, int h, long timestampMillis) {
            // Note: The default original data is in NV21 format
//            Log.i("cam timestamp",String.valueOf(timestampMillis));
            YuvImage img = new YuvImage(data, ImageFormat.NV21, w, h, null);
            String filename = String.format(Locale.US, "%013d.jpg", timestampMillis);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    Config.getStorageDir() + File.separator + "IMG" + File.separator + filename);

            try {
                FileOutputStream fos = new FileOutputStream(file);
                if (!img.compressToJpeg(new Rect(0, 0, w, h), 95, fos))
                    Log.e(TAG, "compressAndSaveAsJPEG: Failed to compress image!");
                fos.close();
            } catch (IOException e) {
                Log.e(TAG, "recordPicture: " + e.getMessage());
            }
        }

        public float getCurrentFPS() { return mCurrentFPS; }
    }
}
