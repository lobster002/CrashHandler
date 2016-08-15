package com.sky;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by Sky on 2016/8/15.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final String TAG = "CrashHandler";

    private boolean isDebug = true;//是否打印日志 关闭可以提高性能

    private boolean isDealHere = true;

    private static CrashHandler mCrashHandler;

    private Context mContext;

    private Thread.UncaughtExceptionHandler mDefaultHandler;//系统默认的UncaughtException处理类

    private String mDeviceCrashInfo = "";

    private static final String CRASH_REPORTER_EXTENSION = ".crash";//错误报告文件的扩展名

    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例 ,单例
     */
    public static CrashHandler getInstance() {
        if (null == mCrashHandler) {
            synchronized (CrashHandler.class) {
                if (null == mCrashHandler) {
                    mCrashHandler = new CrashHandler();
                }
            }
        }
        return mCrashHandler;
    }

    /**
     * 初始化,注册Context对象, 获取系统默认的UncaughtException处理器, 设置该CrashHandler为程序的默认处理器
     */
    public void init(Context ctx) {
        mContext = ctx;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 分发处理异常
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!isDealHere && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                handleException(ex);
            } catch (Exception e) {
                e.printStackTrace();
            }
            SystemClock.sleep(3000);// 来让线程停止一会是为了显示Toast信息给用户，然后Kill程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    /**
     * 自定义异常处理,收集错误信息 发送错误报告等操作均在此完成. 开发者可以根据自己的情况来自定义异常处理逻辑
     */
    private void handleException(Throwable ex) {
        if (null == ex) {
            return;
        }
        showToast("程序出错啦:" + ex.toString()); // 使用Toast来显示异常信息
        saveCrashInfoToFile(ex);// 保存错误报告文件  返回值为文件名
        sendCrashReportsToServer(mContext); // 发送错误报告到服务器
    }


    /**
     * 保存错误信息到文件中
     */
    private String saveCrashInfoToFile(Throwable ex) {

        //收集设备信息
        if (TextUtils.isEmpty(mDeviceCrashInfo)) {
            StringBuffer sb = new StringBuffer();
            sb.append("品牌:").append(Build.MANUFACTURER)
                    .append("\n型号： ").append(Build.MODEL)
                    .append("\nCPU： ").append(Build.HARDWARE)
                    .append("\nSDK版本： ").append(Build.VERSION.SDK_INT)
                    .append("<---------------------------------------------------------------------------->");
            mDeviceCrashInfo = sb.toString();
        }

        Writer info = new StringWriter();
        PrintWriter printWriter = new PrintWriter(info);
        ex.printStackTrace(printWriter);

        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }

        String result = info.toString();
        printWriter.close();

        String msg = result + "\n设备信息: \n" + mDeviceCrashInfo;
        String fileName = getFilePath() + "crash_" + String.valueOf(System.currentTimeMillis()) + CRASH_REPORTER_EXTENSION;
        try {
            File f = new File(fileName);
            if (f.exists()) {
                f.delete();//通常不会走这一步
            }
            f.createNewFile();
            FileOutputStream stream = new FileOutputStream(f);
            byte[] buf = msg.getBytes();
            stream.write(buf);
            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There be something error while write to file");
            return null;
        }
        return fileName;
    }

    /**
     * 把错误报告发送给服务器,包含新产生的和以前没发送的.
     */
    private void sendCrashReportsToServer(Context ctx) {
        ArrayList<String> crFiles = getCrashReportFiles(ctx);
        if (crFiles.size() > 0) {
            askApi(crFiles);
        }
    }

    private void askApi(final ArrayList<String> sortedFiles) {
        //上传错误日志
        /**
         * Do someThing here
         * */
//        for (String fileName : sortedFiles) {
//            new File(getFilePath(), fileName).delete();
//        }
    }

    private String FILE_PATH = null;

    private String getFilePath() {
        if (!TextUtils.isEmpty(FILE_PATH)) {
            return FILE_PATH;
        }
        if (Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashHandler/";
        } else {
            FILE_PATH = mContext.getCacheDir().getPath();
        }
        File f = new File(FILE_PATH);
        if (!f.exists()) {
            f.mkdirs();
        }
        return FILE_PATH;
    }

    /**
     * 获取错误报告文件名
     */
    private ArrayList<String> getCrashReportFiles(Context ctx) {
        File f = new File(getFilePath());
        File[] filesDir = f.listFiles();
        ArrayList<String> result = new ArrayList<String>();
        for (File file : filesDir) {
            if (file.getName().endsWith(CRASH_REPORTER_EXTENSION)) {
                result.add(file.getAbsolutePath());
            }
        }
        return result;
    }

    public boolean isDealHere() {
        return isDealHere;
    }

    public void setDealHere(boolean dealHere) {
        isDealHere = dealHere;
    }

    /**
     * 在程序启动时候, 可以调用该函数来发送以前没有发送的报告
     */
    public void sendPreviousReportsToServer() {
        sendCrashReportsToServer(mContext);
    }

    private void showToast(final String text) {
        new Thread() {
            @Override
            public void run() {
                // Toast 显示需要出现在一个线程的消息队列中
                Looper.prepare();
                Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
    }
}
