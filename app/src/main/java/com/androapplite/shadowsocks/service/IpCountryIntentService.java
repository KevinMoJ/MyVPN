package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class IpCountryIntentService extends IntentService {


    public IpCountryIntentService() {
        super("IpCountryIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
//            String countryCode = getCountryCodeByIp();
//            if (countryCode == null) {
//                countryCode = getCountryByTelephonyManager();
//                if (countryCode == null) {
//                    SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
//                    countryCode = sharedPreferences.getString(SharedPreferenceKey.COUNTRY_CODE, null);
//                    if (countryCode == null) {
//                        countryCode = getResources().getConfiguration().locale.getCountry();
//                    }
//                }
//            }
//            if (countryCode != null) {
//                SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
//                sharedPreferences.edit().putString(SharedPreferenceKey.COUNTRY_CODE, countryCode.toUpperCase()).apply();
//            }
            SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
            JSONObject jsonObject = getCountryCodeAndIp();
            String countryCode = null;
            if (jsonObject != null) {
                countryCode = getCountryCodeByIp(jsonObject);
                if (countryCode == null) {
                    countryCode = getCountryByTelephonyManager();
                    if (countryCode == null) {
                        countryCode = getResources().getConfiguration().locale.getCountry();
                    }
                }
                try {
                    String ipString = jsonObject.getString("query");
                    int ip = convertIpStringToLong(ipString);
                    sharedPreferences.edit().putInt(SharedPreferenceKey.IP, ip).apply();
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                }
            }

            if (countryCode != null) {
                sharedPreferences.edit().putString(SharedPreferenceKey.COUNTRY_CODE, countryCode.toUpperCase()).apply();
                Firebase.getInstance(this).logEvent("国家", "代码", countryCode.toUpperCase());
            } else {
                Firebase.getInstance(this).logEvent("国家", "代码", "未知");
            }
            Firebase.getInstance(this).logEvent("国家", "本机ip", sharedPreferences.getInt(SharedPreferenceKey.IP, 0));
        }
    }

    private String getCountryCodeByIp() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://ip-api.com/json").openConnection();
            connection.setConnectTimeout(1000 * 20);
            connection.setReadTimeout(1000 * 20);
            int code = connection.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                String line = reader.readLine();
                while (line != null) {
                    buffer.append(line);
                    line = reader.readLine();
                }

                try {
                    JSONObject json = new JSONObject(buffer.toString());
                    String countryCode = json.getString("countryCode");
                    if (countryCode != null && countryCode.length() == 2) {
                        return countryCode;
                    }
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                }
            } else {
                ShadowsocksApplication.handleException(new Exception(connection.getResponseMessage()));
            }
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private String getCountryCodeByIp(JSONObject jsonObject) {
        try {
            String countryCode = jsonObject.getString("countryCode");
            if (countryCode != null && countryCode.length() == 2) {
                return countryCode;
            }
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        return null;
    }

    private JSONObject getCountryCodeAndIp() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://ip-api.com/json").openConnection();
            connection.setConnectTimeout(1000 * 20);
            connection.setReadTimeout(1000 * 20);
            int code = connection.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                String line = reader.readLine();
                while (line != null) {
                    buffer.append(line);
                    line = reader.readLine();
                }

                try {
                    return new JSONObject(buffer.toString());
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                }
            } else {
                ShadowsocksApplication.handleException(new Exception(connection.getResponseMessage()));
            }
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public static void startService(Context context) {
        context.startService(new Intent(context, IpCountryIntentService.class));
    }

    private String getCountryByTelephonyManager() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String iosCountry = manager.getSimCountryIso();
        return iosCountry.isEmpty() ? null : iosCountry;
    }

    public static int convertIpStringToLong(String ipString) {
        String[] arrStrings = ipString.split("\\.");
        int r = (Integer.parseInt(arrStrings[0]) << 24)
                | (Integer.parseInt(arrStrings[1]) << 16)
                | (Integer.parseInt(arrStrings[2]) << 8)
                | Integer.parseInt(arrStrings[3]);
        return r;
    }

}
