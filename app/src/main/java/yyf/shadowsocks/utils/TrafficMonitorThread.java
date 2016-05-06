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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import yyf.shadowsocks.service.BaseService;

/**
 * Created by jim on 16/5/3.
 */
public class TrafficMonitorThread extends Thread {
    private static final String TAG = "TrafficMonitorThread";
    private static final String PATH = Constants.Path.BASE + "stat_path";
    private volatile boolean isRunning;
    private volatile LocalServerSocket serverSocket;
    private BaseService mBaseService;

    public TrafficMonitorThread(@NonNull BaseService baseService){
        super(TrafficMonitorThread.class.getSimpleName());
        isRunning = true;
        mBaseService = baseService;
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

    @Override
    public void run() {
        try{
            new File(PATH).delete();
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }

        LocalSocket localSocket = new LocalSocket();
        try {
            localSocket.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(localSocket.getFileDescriptor());

            ExecutorService pool = Executors.newSingleThreadExecutor();
            while (isRunning){
                final LocalSocket socket = serverSocket.accept();
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream input = socket.getInputStream();
                            OutputStream output = socket.getOutputStream();
                            byte[] buffer = new byte[16];
                            if(input.read(buffer) != 16) throw new IOException("Unexpected traffic stat length");
                            ByteBuffer stat = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                            mBaseService.getTrafficMonitor().update(stat.getLong(0), stat.getLong(8));
                            output.write(0);
                            input.close();
                            output.close();
                        } catch (IOException e) {
                            ShadowsocksApplication.handleException(e);
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            ShadowsocksApplication.handleException(e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            ShadowsocksApplication.handleException(e);
        }

    }
}
