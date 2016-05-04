package yyf.shadowsocks.utils;

import android.content.Context;
import android.net.LocalServerSocket;
import android.support.annotation.NonNull;

import java.io.IOException;

import yyf.shadowsocks.service.BaseService;

/**
 * Created by jim on 16/5/3.
 */
public class TrafficMonitorThread extends Thread {
    private static final String TAG = "TrafficMonitorThread";
    private static final String  STAT_PATH = "/stat_path";
    private volatile LocalServerSocket serverSocket;
    private volatile Boolean isRunning;

    public TrafficMonitorThread(@NonNull BaseService baseService){
        super(TrafficMonitorThread.class.getSimpleName());
        isRunning = true;
    }

    public void stopThread() {
        isRunning = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        if(serverSocket != null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
    }
}
