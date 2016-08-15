package com.sky;

import android.app.Application;

/**
 * Created by Sky on 2016/8/15.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(getApplicationContext());
//        CrashHandler.getInstance().sendPreviousReportsToServer();//上传之前日志到服务器
    }
}
