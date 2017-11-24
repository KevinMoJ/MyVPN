package com.vm.shadowsocks.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.service.ConnectionTestService;
import com.androapplite.shadowsocks.service.FindProxyService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjian on 2017/10/19.
 */

public class StatusGuard implements Runnable{
    private long mSentByteCountLast;
    private long mReceivedByteCountLast;
    private long mProxySentByteCountLast;
    private long mProxyReceivedByteCountLast;
    private long mProxyPayloadSentByteCountLast;
    private long mProxyPayloadReceivedByteCountLast;
    private long mPayloadSentByteCountLast;
    private long mPayloadReceivedByteCountLast;
    private int mScheduleCount;
    private int mNoReceivedCount;
    private int mNoProxyReceivedCount;
    private int mNetworkGoodCount;
    private int mNetworkBadCount;
    private int mProxyGoodCount;
    private int mProxyBadCount;
    private ConcurrentHashMap<String, Integer> mErrors = new ConcurrentHashMap<>();
    private PrintWriter mPrintWriter;
    private Calendar mCalendar;
    private Context mContext;
    private long mSentByteCountHourly;
    private long mReceivedByteCountHourly;
    private long mProxySentByteCountHourly;
    private long mProxyReceivedByteCountHourly;
    private long mProxyPayloadSentByteCountHourly;
    private long mProxyPayloadReceivedByteCountHourly;
    private long mPayloadSentByteCountHourly;
    private long mPayloadReceivedByteCountHourly;
    private long mSentCountHourly;
    private long mReceivedCountHourly;
    private long mProxySentCountHourly;
    private long mProxyReceivedCountHourly;
    private int mProxyBadCountSequence;

    public StatusGuard(Context context, ScheduledExecutorService scheduledExecutorService) {
//        String state = Environment.getExternalStorageState();
//        if (state.equals(Environment.MEDIA_MOUNTED)){
//            File root = Environment.getExternalStorageDirectory();
//            File folder = new File(root, context.getPackageName());
//            try {
//                if (!folder.exists()) {
//                    folder.mkdir();
//                }
//                File file = new File(folder, "status_guard.log");
//                FileOutputStream fos = new FileOutputStream(file, true);
//                mPrintWriter = new PrintWriter(fos, true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        mCalendar = Calendar.getInstance();
        try {
            scheduledExecutorService.scheduleWithFixedDelay(this, 1, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        mContext = context;
    }


    @Override
    public void run() {
        LocalVpnService localVpnService = LocalVpnService.Instance;
        if (localVpnService.IsRunning) {
            mScheduleCount++;
//            if (mScheduleCount == 1 || mScheduleCount == 20 || mScheduleCount % 720 == 0) {
//                mCalendar.setTimeInMillis(System.currentTimeMillis());
//                if (mPrintWriter != null) {
//                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
//                    mPrintWriter.println(simpleDateFormat.format(mCalendar.getTime()));
//                }
//            }
            //检查线程
            Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
            println("thread exist? VPNService %s, Dns proxy %s, Tcp proxy %s, Udp proxy %s",
                    threads.containsKey(localVpnService.getVpnThread()),
                    threads.containsKey(localVpnService.getDnsProxy().getThread()),
                    threads.containsKey(localVpnService.getTcpProxyServer().getThread()),
                    threads.containsKey(localVpnService.getUdpProxy().getThread()));


            //检查DNS端口
            boolean isDnsProxyPortExist = false;
            int dnsProxyPort = 0;
            DatagramSocket datagramSocket = null;
            try {
                dnsProxyPort = localVpnService.getDnsProxy().Port;
                datagramSocket = new DatagramSocket(dnsProxyPort);
            } catch (BindException e) {
                isDnsProxyPortExist = true;
            } catch (SocketException e) {
                e.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }

            //检查TCP端口
            boolean isTcpProxyPortExist = false;
            int tcpProxyPort = 0;
            ServerSocket serverSocket = null;
            try {
                tcpProxyPort = localVpnService.getTcpProxyServer().Port;
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(tcpProxyPort));
            } catch (BindException e) {
                isTcpProxyPortExist = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //检查UDP端口
            boolean isUdpProxyPortExist = false;
            int udpProxyPort = 0;
            datagramSocket = null;
            try {
                udpProxyPort = localVpnService.getUdpProxy().Port;
                datagramSocket = new DatagramSocket(udpProxyPort);
            } catch (BindException e) {
                isDnsProxyPortExist = true;
            } catch (SocketException e) {
                e.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }

            println("port exist? Dns proxy %d %s, Tcp proxy %d %s, Udp proxy %d %s",
                    dnsProxyPort, isDnsProxyPortExist, tcpProxyPort, isTcpProxyPortExist,
                    udpProxyPort, isUdpProxyPortExist);

            boolean needCheckNetwork = false;
            boolean needCheckProxy = false;
            boolean noTrafficData = false;
            boolean noProxyTrafficData = false;

            TcpTrafficMonitor trafficMonitor = localVpnService.getTcpTrafficMonitor();
            if (trafficMonitor != null) {
                //发送和接收数据
                println("count and byte Total: %d/%d, %d/%d; Proxy: %d/%d, %d/%d",
                        trafficMonitor.pSentCount - mSentCountHourly, trafficMonitor.pReceivedCount - mReceivedCountHourly,
                        trafficMonitor.pSentByteCount - mSentByteCountHourly, trafficMonitor.pReceivedByteCount - mReceivedByteCountHourly,
                        trafficMonitor.pProxySentCount - mProxySentCountHourly, trafficMonitor.pProxyReceivedCount - mProxyReceivedCountHourly,
                        trafficMonitor.pProxySentByteCount - mProxySentByteCountHourly, trafficMonitor.pProxyReceivedByteCount - mProxyReceivedByteCountHourly);

                //检查连接数
                if (mPayloadReceivedByteCountLast == trafficMonitor.pPayloadReceivedByteCount) {
                    needCheckNetwork = true;
                    if (mPayloadSentByteCountLast != trafficMonitor.pPayloadSentByteCount) {
                        ++mNoReceivedCount;
                        noTrafficData = true;
                    }
                }

                if (mProxyPayloadReceivedByteCountLast == trafficMonitor.pProxyPayloadReceivedByteCount) {
                    needCheckProxy = true;
                    if (mProxyPayloadSentByteCountLast != trafficMonitor.pProxyPayloadSentByteCount) {
                        ++mNoProxyReceivedCount;
                        noProxyTrafficData = true;
                    }
                }

                println("no byte count ALL: %d, Proxy: %d; Proxy payload: %d/%d",
                        mNoReceivedCount, mNoProxyReceivedCount, trafficMonitor.pProxyPayloadSentByteCount - mProxyPayloadSentByteCountHourly,
                        trafficMonitor.pProxyPayloadReceivedByteCount - mProxyPayloadReceivedByteCountHourly);

                mSentByteCountLast = trafficMonitor.pSentByteCount;
                mReceivedByteCountLast = trafficMonitor.pReceivedByteCount;
                mProxySentByteCountLast = trafficMonitor.pProxySentByteCount;
                mProxyReceivedByteCountLast = trafficMonitor.pProxyReceivedByteCount;
                mProxyPayloadSentByteCountLast = trafficMonitor.pProxyPayloadSentByteCount;
                mProxyPayloadReceivedByteCountLast = trafficMonitor.pProxyPayloadReceivedByteCount;
                mPayloadSentByteCountLast = trafficMonitor.pPayloadSentByteCount;
                mPayloadReceivedByteCountLast = trafficMonitor.pPayloadReceivedByteCount;
            }

            if (needCheckNetwork || needCheckProxy) {
                if (checkActiveNetwork()){
                    ++mNetworkGoodCount;
                } else  {
                    ++mNetworkBadCount;
                }
            }

            if (!noTrafficData && noProxyTrafficData) {
                ++mProxyBadCount;
                ++mProxyBadCountSequence;
            } else if (!noProxyTrafficData){
                ++mProxyGoodCount;
            }

            if (!needCheckProxy) {
                mProxyBadCountSequence = 0;
            }

            if (mProxyBadCountSequence > 2 && LocalVpnService.IsRunning) {
                Log.d("[heart beat]", "switchProxy");
                FindProxyService.switchProxy(mContext);
                mProxyBadCountSequence = 0;
            }

            println("connectivity status Network: %d/%d; Proxy: %d/%d",
                    mNetworkGoodCount, mNetworkBadCount, mProxyGoodCount, mProxyBadCount);

            //检测错误数量
            if (!mErrors.isEmpty()) {
                for (Map.Entry<String, Integer> entry : mErrors.entrySet()) {
                    println("error %d, %s", entry.getValue(), entry.getKey());
                }
            }

            if (mScheduleCount % 720 == 0) {
                mErrors.clear();
                mNoReceivedCount = 0;
                mNoProxyReceivedCount = 0;
                mNetworkGoodCount = 0;
                mNetworkBadCount = 0;
                mProxyGoodCount = 0;
                mProxyBadCount = 0;
                mSentByteCountHourly = trafficMonitor.pSentByteCount;
                mReceivedByteCountHourly = trafficMonitor.pReceivedByteCount;
                mProxySentByteCountHourly = trafficMonitor.pProxySentByteCount;
                mProxyReceivedByteCountHourly = trafficMonitor.pProxyReceivedByteCount;
                mProxyPayloadSentByteCountHourly = trafficMonitor.pProxyPayloadSentByteCount;
                mProxyPayloadReceivedByteCountHourly = trafficMonitor.pProxyPayloadReceivedByteCount;
                mPayloadSentByteCountHourly = trafficMonitor.pPayloadSentByteCount;
                mPayloadReceivedByteCountHourly = trafficMonitor.pPayloadReceivedByteCount;
                mSentCountHourly = trafficMonitor.pSentCount;
                mReceivedCountHourly = trafficMonitor.pReceivedCount;
                mProxySentCountHourly = trafficMonitor.pProxySentCount;
                mProxyReceivedCountHourly = trafficMonitor.pProxyReceivedCount;
            }
        }
    }

    private boolean checkActiveNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) LocalVpnService.Instance.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Integer> getErrors() {
        return mErrors;
    }

    private void println(String format, Object... args){
        String logString = String.format(format, args);
        System.out.printf("[heart beat] %s\n", logString);
        if (mScheduleCount == 1 || mScheduleCount == 20 || mScheduleCount % 720 == 0) {
            if (mPrintWriter != null) {
                mPrintWriter.println(logString);
            }
            Firebase.getInstance(mContext).logEvent("StatusGuard", String.valueOf(mScheduleCount), logString);
        }
    }
}
