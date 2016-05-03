package yyf.shadowsocks.service;

import android.app.Notification;
import android.content.Context;
import android.net.VpnService;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.utils.Constants;
import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.TrafficMonitor;
import yyf.shadowsocks.utils.TrafficMonitorThread;

/**
 * Created by yyf on 2015/6/18.
 */
public abstract class BaseService extends VpnService {
    volatile private Constants.State state = Constants.State.INIT;
    volatile private int callbackCount = 0;
    volatile protected TrafficMonitorThread trafficMonitorThread;

    final RemoteCallbackList<IShadowsocksServiceCallback> callbacks = new RemoteCallbackList<IShadowsocksServiceCallback>();
    IShadowsocksService.Stub binder = new IShadowsocksService.Stub(){
        public int getMode(){
            return getServiceMode().ordinal();
        }

        public int getState(){
            return state.ordinal();
        }

        @Override
        public void registerCallback(IShadowsocksServiceCallback cb) throws RemoteException {
            BaseService.this.registerCallback(cb);
        }

        @Override
        public void unregisterCallback(IShadowsocksServiceCallback cb) throws RemoteException {
            BaseService.this.unregisterCallback(cb);
        }

        public void stop() {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                stopRunner();
            }
        }

        public void start(Config config) {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                startRunner(config);
            }
        }
    };

    public abstract void stopBackgroundService();
    public void startRunner(Config config){
        TrafficMonitor.reset();
        trafficMonitorThread = new TrafficMonitorThread(this);
        trafficMonitorThread.start();
    }
    public abstract void stopRunner();
    public abstract Constants.Mode getServiceMode();
    public abstract String getTag();
    public abstract Context getContext();

    public int getCallbackCount(){
        //        return callbackCount;
        return -1;
    }
    public Constants.State getState(){
        return state;
    }
    public void changeState(Constants.State s) {
        changeState(s, null);
    }

    protected void changeState(final Constants.State s,final String msg) {
        Handler handler = new Handler(getContext().getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                if (state != s) {
                    if (callbackCount > 0) {
                        int n = callbacks.beginBroadcast();
                        for (int i = 0; i < n; i++) {
                            try {
                                callbacks.getBroadcastItem(i).stateChanged(s.ordinal(), msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        callbacks.finishBroadcast();
                    }
                    state = s;
                }
            }
        });

    }

    void initSoundVibrateLights(Notification notification) {
        notification.sound = null;
    }

    public void registerCallback(IShadowsocksServiceCallback callback){
        if(callback != null) {
            callbacks.register(callback);
            callbackCount += 1;
        }
    }

    public void unregisterCallback(IShadowsocksServiceCallback callback){
        if(callback != null) {
            callbacks.unregister(callback);
            callbackCount -= 1;
        }
    }

}
