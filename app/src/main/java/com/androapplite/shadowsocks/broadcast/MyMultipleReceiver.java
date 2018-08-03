package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;

/**
 * Created by jikai on 11/15/17.
 */

public class MyMultipleReceiver extends BroadcastReceiver {
    protected static final String REFERRER_PREF = "referrer";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(new Intent("com.android.vending.INSTALL_REFERRER"), 0);
        for (ResolveInfo resolveInfo : receivers){
            if(resolveInfo.activityInfo.packageName.equals(context.getPackageName())
                    &&  "com.android.vending.INSTALL_REFERRER".equals(action)
                    && !this.getClass().getName().equals(resolveInfo.activityInfo.name)){
                try {
                    BroadcastReceiver broadcastReceiver = (BroadcastReceiver) Class.forName(resolveInfo.activityInfo.name).newInstance();
                    broadcastReceiver.onReceive(context,intent);
                } catch (Throwable e) {
                }
            }
        }

        if ("com.android.vending.INSTALL_REFERRER".equals(action)) {
            String referrer = intent.getStringExtra(REFERRER_PREF);
            if (referrer != null && !referrer.isEmpty()) {
                if ("utm_source=(not%20set)&utm_medium=(not%20set)".equals(referrer)) {
                } else {
                    HashMap<String, String> maps = new HashMap<>();
                    String[] pairs = referrer.split("&");
                    if (pairs != null && pairs.length > 0) {
                        for (int i = 0; i < pairs.length; i++) {
                            String one = pairs[i];
                            String[] kv = one.split("=");
                            if (kv != null && kv.length == 2) {
                                String key = kv[0];
                                String value = kv[1];
                                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                                    maps.put(key, value);
                                }
                            }
                        }
                    }
                    String campaignType = maps.get("campaigntype");
                    String network = maps.get("network");
                    String campaignId = maps.get("campaignid");
                    if ("a".equals(campaignType) && "g".equals(network) && !TextUtils.isEmpty(campaignId)) {
                    }
                }
            }
        }
    }
}
