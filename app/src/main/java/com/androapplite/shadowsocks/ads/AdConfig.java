package com.androapplite.shadowsocks.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.umeng.analytics.game.UMGameAgent;

import java.util.HashMap;

public class AdConfig {
    public static class AdIds {
        public String admob = "";
        public String admob_full = "";
        public String admob_n = "";
        public String fb = "";
        public String fb_f = "";
        public String fb_n = "";
        public String fbns_banner = "";
        public String fbns_full = "";
    }

    public static class NgsCtrl {
        public int exe;
        public int admob;
        public int facebook;
        public int fbn;
    }

    public static class AdCtrl {
        public int ad_delay;
        public static class NgsOrder {
            public int before;
            public int adt;
            public int adt_type;
        }
        public NgsOrder ngsorder = new NgsOrder();
        public NgsOrder ngsorder_admob = new NgsOrder();
    }

    public static class BannerCtrl {
        public int exe;
        public int admob;
        public int facebook;
        public int fbn;
    }

    public static class NativeCtrl {
        public int exe;
        public int admob;
        public int facebook;
    }

    public static class AdCountCtrl {
        public int exe;
        public int full_interval;
        public int full_count;
        public int banner_interval;
        public int banner_count;
        public int native_interval;
        public int native_count;

        public long last_full_show_time;
        public int last_full_show_count;
        public long last_banner_show_time;
        public int last_banner_show_count;
        public long last_native_show_time;
        public int last_native_show_count;
        public int last_day;
    }

    public AdIds ad_ids = new AdIds();
    public NgsCtrl ngs_ctrl = new NgsCtrl();
    public BannerCtrl banner_ctrl = new BannerCtrl();
    public NativeCtrl native_ctrl = new NativeCtrl();
    public AdCountCtrl ad_count_ctrl = new AdCountCtrl();
    public AdCtrl ad_ctrl = new AdCtrl();

    public static int getInt(HashMap map, String key, int defaultValue) {
        try {
            Integer i = Integer.parseInt((String)map.get(key));
            return i;
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static String getString(HashMap map, String key, String defaultValue) {
        try {
            String value = (String)map.get(key);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static HashMap<String, String> getValues(Context context, String key) {
        String value = UMGameAgent.getConfigParams(context, key);
        if (TextUtils.isEmpty(value)) {
            SharedPreferences sp = context.getSharedPreferences("ourdefault_game_config", 0);
            return convertToMap(sp.getString(key, ""));
        } else {
            return convertToMap(value);
        }
    }


    private static HashMap convertToMap(String value) {
        HashMap ret = new HashMap();
        if(value != null && !"".equals(value.trim())) {
            value = value.trim();

            int start;
            int end;
            for(boolean b = true; (start = value.indexOf('{')) >= 0 && (end = value.indexOf('}')) > 0; value = value.substring(end + 1)) {
                String kv = value.substring(start + 1, end);
                if(kv != null && kv.indexOf("=") > 0) {
                    int equal = kv.indexOf("=");
                    ret.put(kv.substring(0, equal).trim(), kv.substring(equal + 1).trim());
                }
            }
        }
        return ret;
    }
}
