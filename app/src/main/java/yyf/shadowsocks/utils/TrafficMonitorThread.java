package yyf.shadowsocks.utils;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.support.annotation.NonNull;
import android.util.Log;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jim on 16/5/3.
 */
public class TrafficMonitorThread extends Thread {
    private static final String TAG = "TrafficMonitorThread";
    private static final String PATH = Constants.Path.BASE + "stat_path";
    private volatile boolean isRunning;
    private volatile LocalServerSocket serverSocket;
    private VpnService mVpnService;

    public TrafficMonitorThread(@NonNull VpnService vpnService){
        super(TrafficMonitorThread.class.getSimpleName());
        isRunning = true;
        mVpnService = vpnService;
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
                ShadowsocksApplication.handleException(e);
            }
            serverSocket = null;
        }
    }

}
