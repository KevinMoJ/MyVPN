package yyf.shadowsocks.utils;

import android.content.Context;
import android.net.LocalServerSocket;
import android.support.annotation.NonNull;

/**
 * Created by jim on 16/5/3.
 */
public class TrafficMonitorThread extends Thread {
    private static final String TAG = "TrafficMonitorThread";
    private static final String  STAT_PATH = "/stat_path";
    private volatile LocalServerSocket serverSocket;
    private volatile Boolean isRunning;

    public TrafficMonitorThread(@NonNull Context context){
        super(TrafficMonitorThread.class.getSimpleName());
    }
}
