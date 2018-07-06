package com.ypc.datarecorder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by yangpc on 2018/7/3.
 */
public class Config {
    private static boolean isCapturing=false;    //是否监听数据
    private static String storageDir=new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(Calendar.getInstance().getTime());;
    public static boolean isCapturing() {
        return isCapturing;
    }
    public static void setCapturing(boolean capturing) {
        isCapturing = capturing;
        if(isCapturing){
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        }
    }
    public static String getStorageDir(){
        return storageDir;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    public static void verifyPermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_EXTERNAL_STORAGE);
            }
            if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CAMERA},1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
