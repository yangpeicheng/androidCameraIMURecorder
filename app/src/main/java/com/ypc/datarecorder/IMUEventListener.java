package com.ypc.datarecorder;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

/**
 * Created by yangpc on 2018/7/2.
 */
public class IMUEventListener implements SensorEventListener {
    public enum SensorType{
        G,      //gyroscope
        A,      //accelerometer
        M,       //magn
        Gravity
    }


    public class SensorData{
        private float x,y,z;
        private long timestamp;
        public SensorData(float[] v,long timestamp){
            x=v[0];
            y=v[1];
            z=v[2];
            this.timestamp=timestamp;
        }
        public SensorData(float x,float y,float z,long timestamp){
            this.x=x;
            this.y=y;
            this.z=z;
            this.timestamp=timestamp;
        }
        @Override
        public String toString() {
            return String.format("%08.6f %08.6f %08.6f %d \n\r", x, y, z, timestamp);
        }
    }

    private SensorData mCurrentData;        //当前数据
    private LinkedList<SensorData> mBufferData;        //缓存数据列表
    private int BUFFERLEN=500;             //缓存列表长度
    private int mSerializedNum=0;           //文件序列号
    private SensorType mSensorType;             //传感器类型
    private final String TAG = "TAG/CameraIMU";

    public IMUEventListener(SensorType type){
        mCurrentData=new SensorData(0,0,0,0);
        mBufferData=null;
        mSensorType=type;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(Config.isCapturing()){
            mCurrentData.x=event.values[0];
            mCurrentData.y=event.values[1];
            mCurrentData.z=event.values[2];
            mCurrentData.timestamp=event.timestamp;
            Log.i("imu timestamp",String.valueOf(event.timestamp));
            record(event.values,event.timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void record(float[] values,long timestamp){
        if(mBufferData==null){
            mBufferData=new LinkedList<SensorData>();
        }
        mBufferData.add(new SensorData(values,timestamp));
        if(mBufferData.size()==BUFFERLEN){
            LinkedList<SensorData> asyncSequence=mBufferData;
            mBufferData=new LinkedList<SensorData>();
            new OutputAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,asyncSequence);
        }
    }

    private class OutputAsyncTask extends AsyncTask<LinkedList<?>,Void,Void>{
        @Override
        protected Void doInBackground(LinkedList<?>... params) {
            LinkedList<SensorData> data=(LinkedList<SensorData>)params[0];
            String filename=String.format(mSensorType + "%08d.txt", mSerializedNum++);
//            File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+
//                    Config.getStorageDir()+File.separator+filename);

            File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    Config.getStorageDir()+File.separator+filename);

            //写缓存数据
            try{
                FileOutputStream fos=new FileOutputStream(file);
                BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(fos));
                for(SensorData sensorData:data)
                    writer.write(sensorData.toString());
                writer.flush();
                writer.close();
            }catch (IOException e){
                Log.e(TAG, "doInBackground: " + e.getMessage());
            }
            return null;
        }
    }



}
