package com.ypc.datarecorder;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import static com.ypc.datarecorder.CamPreview.DEFAULT_CAPTURE_H;
import static com.ypc.datarecorder.CamPreview.DEFAULT_CAPTURE_W;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "TAG/CameraIMU";
    private SensorManager mSensorManager;
    private Sensor mAcc;
    private Sensor mGyro;
//    private Sensor mMagn;
//    private Sensor mGravity;

    private IMUEventListener mAccListener;
    private IMUEventListener mGyroListener;
    private MultipleSensorEventListener mMultipleSensorEventListener;
//    private IMUEventListener mMagnListener;
//    private IMUEventListener mGravityListener;

    private Camera mCamera;
    private CamCallbacks.PreviewCallback mPreviewCallback;


    private Button mButtonCapture;
    private boolean mButtonState=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        mAcc=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro=mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//        mMagn=mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        mGravity=mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

//        mAccListener=new IMUEventListener(IMUEventListener.SensorType.A);
//        mGyroListener=new IMUEventListener(IMUEventListener.SensorType.G);
//        mMagnListener=new IMUEventListener(IMUEventListener.SensorType.M);
//        mGravityListener=new IMUEventListener(IMUEventListener.SensorType.Gravity);
        mMultipleSensorEventListener=new MultipleSensorEventListener();


        mButtonCapture=(Button) findViewById(R.id.isCapture);
        Config.verifyPermissions(this);
        if (!isExternalStorageWritable()) {
            Toast toast = Toast.makeText(MainActivity.this, R.string.failed_to_access_external_storage, Toast.LENGTH_SHORT);
            toast.show();
            finish();
            return;
        }

        if (!checkCamera()) {
            Toast toast = Toast.makeText(MainActivity.this, R.string.failed_to_access_camere, Toast.LENGTH_SHORT);
            toast.show();
            finish();
            return;
        }
        getCameraInstance();
        ((FrameLayout)findViewById(R.id.cam_layout)).addView(new CamPreview(this,mCamera));

        mPreviewCallback=new CamCallbacks.PreviewCallback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mMultipleSensorEventListener,mAcc,SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mMultipleSensorEventListener,mGyro,SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(mAccListener,mAcc, SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(mGyroListener,mGyro,SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(mMagnListener,mMagn,SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(mGravityListener,mGravity,SensorManager.SENSOR_DELAY_FASTEST);
        setCamFeatures();
        mButtonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,mButtonCapture.getText().toString());
                if(!mButtonState){
                    Config.setCapturing(true);
                    mButtonCapture.setText("on");
                    mButtonState=true;
                    File path=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                            Config.getStorageDir()+File.separator+"IMG");
                    if (!path.mkdirs()) {
                        Context context = getApplicationContext();
                        Toast toast = Toast.makeText(context, R.string.failed_to_access_external_storage, Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    mCamera.setPreviewCallback(mPreviewCallback);
                }
                else{
                    Config.setCapturing(false);
                    mButtonState=false;
                    mButtonCapture.setText("off");
                    mCamera.setPreviewCallback(null);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mMultipleSensorEventListener);
//        mSensorManager.unregisterListener(mMagnListener);
//        mSensorManager.unregisterListener(mGravityListener);
//        mSensorManager.unregisterListener(mGyroListener);
//        mSensorManager.unregisterListener(mAccListener);
        releaseCamera();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private boolean checkCamera() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return true;
        else
            return false;
    }

    private void getCameraInstance(){
        if(mCamera==null){
            try {
                mCamera= Camera.open();
            }
            catch (Exception e){
                Log.e(TAG, "getCameraInstance: " + e.getMessage());
            }
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }


    private void setCamFeatures() {
        Camera.Parameters params = mCamera.getParameters();

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//            mCamera.autoFocus(null);
        }
        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        List<String> whiteBalanceModes = params.getSupportedWhiteBalance();
        if (whiteBalanceModes.contains(Camera.Parameters.WHITE_BALANCE_DAYLIGHT))
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);

        params.setPreviewSize(DEFAULT_CAPTURE_W, DEFAULT_CAPTURE_H);
        params.setPictureSize(DEFAULT_CAPTURE_W, DEFAULT_CAPTURE_H);

        mCamera.setParameters(params);
    }
}
