package yyf.shadowsocks.utils;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import yyf.shadowsocks.service.BaseService;

/**
 * Created by jim on 16/5/3.
 */
public class TrafficMonitorThread extends Thread {
    private static final String TAG = "TrafficMonitorThread";
    private static final String PATH = Constants.Path.BASE + "protect_path";
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
                e.printStackTrace();
            }
            serverSocket = null;
        }
    }

    public void run() {
        try {
            new File(PATH).delete();
        } catch (Exception ex) {
        }

        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(localSocket.getFileDescriptor());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);

        while (isRunning) {
            try {
                final LocalSocket socket = serverSocket.accept();
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream input = socket.getInputStream();
                            OutputStream output = socket.getOutputStream();

                            input.read();

                            FileDescriptor[] fds = socket.getAncillaryFileDescriptors();

                            if (fds.length > 0) {
                                Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
                                int fd = (int) getInt.invoke(fds[0]);
                                boolean ret = mVpnService.protect(fd);

                                // Trick to close file decriptor
                                yyf.shadowsocks.jni.System.jniclose(fd);

                                if (ret) {
                                    output.write(0);
                                } else {
                                    output.write(1);
                                }
                            }
                            input.close();
                            output.close();

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        // close socket
                        try {
                            socket.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (IOException ex) {
                Log.e(TAG, "Error when accept socket", ex);
                return;
            }
        }
    }
}
