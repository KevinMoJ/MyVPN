package yyf.shadowsocks.service;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
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

import yyf.shadowsocks.utils.Constants;

public class ShadowsocksVpnThread extends Thread {
    private static final String TAG = "ShadowsocksVpnService";
    private static final String PATH = Constants.Path.BASE + "protect_path";

    private volatile boolean isRunning = true;
    private volatile LocalServerSocket serverSocket;
    private VpnService vpnService;

    public ShadowsocksVpnThread(VpnService vpnService) {
        this.vpnService = vpnService;
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ex) {
                ShadowsocksApplication.handleException(ex);
            }
            serverSocket = null;
        }
    }

    public void stopThread() {
        isRunning = false;
        closeServerSocket();
    }

    public void run() {
        try {
            new File(PATH).delete();
        } catch (Exception ex) {
            ShadowsocksApplication.handleException(ex);
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
                                boolean ret = vpnService.protect(fd);

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
                            ShadowsocksApplication.handleException(ex);
                        }

                        // close socket
                        try {
                            socket.close();
                        } catch (Exception ex) {
                            ShadowsocksApplication.handleException(ex);
                        }
                    }
                });
            } catch (IOException ex) {
                ShadowsocksApplication.handleException(ex);
                return;
            }
        }
    }
}
