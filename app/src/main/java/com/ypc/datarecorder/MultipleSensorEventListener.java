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
 * Created by yangpc on 2018/7/3.
 */
public class MultipleSensorEventListener implements SensorEventListener {
    public class IMUdata{
        private float ax,ay,az;
        private float gx,gy,gz;
        private long timestamp;
        private boolean flagAcc=false;
        private boolean flagGyro=false;
        public boolean setAcc(float[] value,long timestamp){
            ax=value[0];
            ay=value[1];
            az=value[2];
            flagAcc=true;
            if(flagAcc&&flagGyro){
                this.timestamp=timestamp;
                flagAcc=false;
                flagGyro=false;
                return true;
            }
            return false;
        }
        public boolean setGyro(float[] value,long timestamp){
            gx=value[0];
            gy=value[1];
            gz=value[2];
            flagGyro=true;
            if(flagAcc&&flagGyro){
                this.timestamp=timestamp;
                flagAcc=false;
                flagGyro=false;
                return true;
            }
            return false;
        }
        public void setTimestamp(long timestamp){
            this.timestamp=timestamp;
        }
        public String toString(){
            return String.format("%d,%08.6f,%08.6f,%08.6f,%08.6f,%08.6f,%08.6f \r\n"
                    ,timestamp,gx,gy,gz,ax,ay,az);
        }

        public IMUdata clone(){
            IMUdata clonedata=new IMUdata();
            clonedata.setAcc(new float[]{this.ax,this.ay,this.az},this.timestamp);
            clonedata.setGyro(new float[]{this.gx,this.gy,this.gz},this.timestamp);
            return clonedata;
        }
    }
    private IMUdata mCurrentIMUdata=new IMUdata();
    private LinkedList<IMUdata> mBuffer;
    private int BUFFERSIZE=500;
    private final String TAG = "TAG/CameraIMU";

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!Config.isCapturing())
            return;
        boolean update=false;
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            update=mCurrentIMUdata.setAcc(event.values,event.timestamp);
//            update=mCurrentIMUdata.setAcc(event.values,System.nanoTime());
        }else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
            update=mCurrentIMUdata.setGyro(event.values,event.timestamp);
//            update=mCurrentIMUdata.setGyro(event.values,System.nanoTime());
        }
        if(update){
//            mCurrentIMUdata.setTimestamp(System.nanoTime());
            record(mCurrentIMUdata);
            Log.i("imu timestamp",String.valueOf(System.nanoTime()));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void record(IMUdata data){
        if(mBuffer==null){
            mBuffer=new LinkedList<IMUdata>();
        }
        mBuffer.add(data.clone());
        if(mBuffer.size()==BUFFERSIZE){
            LinkedList<IMUdata> asyncSequence=mBuffer;
            mBuffer=new LinkedList<IMUdata>();
            new OutputAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,asyncSequence);
        }
    }

    private class OutputAsyncTask extends AsyncTask<LinkedList<?>,Void,Void> {
        @Override
        protected Void doInBackground(LinkedList<?>... params) {
            LinkedList<IMUdata> data=(LinkedList<IMUdata>)params[0];
            String filename="imu";
//            File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+
//                    Config.getStorageDir()+File.separator+filename);

            File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    Config.getStorageDir()+File.separator+filename);

            //写缓存数据
            try{
                FileOutputStream fos=new FileOutputStream(file,true);
                BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(fos));
                for(IMUdata sensorData:data)
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
