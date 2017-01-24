package yyf.shadowsocks.broadcast;

import com.androapplite.vpn3.BuildConfig;

/**
 * Created by jim on 16/5/10.
 */
public final class Action {
//    public static final String RESET_TOTAL = BuildConfig.APPLICATION_ID + ".RESET_TOTAL";
    public static final String INIT = BuildConfig.APPLICATION_ID + ".INIT";
    public static final String CONNECTING = BuildConfig.APPLICATION_ID + ".CONNECTING";
    public static final String CONNECTED = BuildConfig.APPLICATION_ID + ".CONNECTED";
    public static final String STOPPING = BuildConfig.APPLICATION_ID + ".STOPPING";
    public static final String STOPPED = BuildConfig.APPLICATION_ID + ".STOPPED";
    public static final String ERROR = BuildConfig.APPLICATION_ID + ".ERROR";
}
