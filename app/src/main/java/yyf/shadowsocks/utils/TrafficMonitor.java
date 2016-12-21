package yyf.shadowsocks.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import com.androapplite.shadowsocks.R;

import java.text.DecimalFormat;
import java.util.Formatter;

/**
 * Created by jim on 16/5/3.
 */
public final class TrafficMonitor {
    private volatile boolean dirty;
    private static final DecimalFormat numberFormat = new DecimalFormat("@@@");
    // Bytes per second
    public long txRate;
    public long rxRate;
    // Bytes for the current session
    public long txTotal;
    public long rxTotal;
    // Bytes for the last query
    private long txLast;
    private long rxLast;
    private long timestampLast;
    private static final String[] units = { "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB", "NB", "DB", "CB" };
    private static final String[] RATE_UNITS = {"KB/s", "MB/s", "GB/s", "TB/s", "PB/s"};

/*    public TrafficMonitor(){
        units = new String[] { "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB", "NB", "DB", "CB" };
        numberFormat = new DecimalFormat("@@@");
    }*/

    @NonNull
    public static final String formatTrafficRate(@NonNull Context context, long size){
        double n = size;
        int i = -1;
        while (n >= 1000) {
            n /= 1024;
            i = i + 1;
        }
        StringBuilder formatString = new StringBuilder();
        if(i < 0){
            formatString.append(size).append(" B/s");
        }else{
            formatString.append(numberFormat.format(n)).append(" ").append(RATE_UNITS[i]);
        }

        return formatString.toString();
    }

//    @NonNull
//    public static final String formatTraffic(@NonNull Context context, long size){
//        double n = size;
//        int i = -1;
//        while (n >= 1000) {
//            n /= 1024;
//            i = i + 1;
//        }
//        StringBuilder formatString = new StringBuilder();
//        if(i < 0){
//            formatString.append(size).append(" ").append(context.getResources().getQuantityString(R.plurals.bytes, (int) size));
//        }else{
//            formatString.append(numberFormat.format(n)).append(" ").append(units[i]);
//        }
//        return formatString.toString();
//    }

    public boolean updateRate(){
        long now = System.currentTimeMillis();
        long delta = now - timestampLast;
        boolean updated = false;
        if(delta != 0){
            if(dirty){
                txRate = (txTotal - txLast) * 1000 / delta;
                rxRate = (rxTotal - rxLast) * 1000 / delta;
                txLast = txTotal;
                rxLast = rxTotal;
                dirty = false;
                updated = true;
            }else{
                if (txRate != 0) {
                    txRate = 0;
                    updated = true;
                }
                if (rxRate != 0) {
                    rxRate = 0;
                    updated = true;
                }
            }
            timestampLast = now;
        }
        return updated;
    }

    public void update(long tx, long rx){
        if (txTotal != tx) {
            txTotal = tx;
            dirty = true;
        }
        if (rxTotal != rx) {
            rxTotal = rx;
            dirty = true;
        }
    }

    public void reset() {
        rxLast = 0;
        rxRate = 0;
        rxTotal = 0;
        timestampLast = 0;
        txLast = 0;
        txRate = 0;
        txTotal = 0;
        dirty = true;
    }
}
