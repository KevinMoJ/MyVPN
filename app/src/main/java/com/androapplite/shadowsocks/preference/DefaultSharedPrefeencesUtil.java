package com.androapplite.shadowsocks.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * Created by jim on 16/4/27.
 */
public class DefaultSharedPrefeencesUtil {
    public static final SharedPreferences getDefaultSharedPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static final SharedPreferences.Editor getDefaultSharedPreferencesEditor(Context context){
        return getDefaultSharedPreferences(context).edit();
    }

    public static final boolean isNewUser(Context context){
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.IS_NEW_USER, true);
    }

    public static final void markAsOldUser(Context context){
        final SharedPreferences.Editor editor = getDefaultSharedPreferencesEditor(context);
        editor.putBoolean(SharedPreferenceKey.IS_NEW_USER, false).apply();
    }

    public static final boolean doesNeedRateUsFragmentShow(Context context){
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, false);
    }

    public static final void markRateUsFragmentNotNeedToShow(Context context){
        SharedPreferences.Editor editor = getDefaultSharedPreferencesEditor(context);
        editor.putBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, true).apply();
    }

/*    public static final void resetRxTotal(@NonNull Context context){
        getDefaultSharedPreferences(context).edit().putLong(SharedPreferenceKey.RX_TOTAL, 0).apply();
    }

    public static final void resetTxTotal(@NonNull Context context){
        getDefaultSharedPreferences(context).edit().putLong(SharedPreferenceKey.TX_TOTAL, 0).apply();
    }
    public static final long accumulateRxTotal(@NonNull Context context, long increment){

    }
*/

    public static final void resetTxTotalAndRxTotal(@NonNull Context context){
        SharedPreferences.Editor editor = getDefaultSharedPreferencesEditor(context);
        editor.putLong(SharedPreferenceKey.RX_TOTAL, 0).putLong(SharedPreferenceKey.TX_TOTAL, 0).apply();
    }

    public static final void accumulateTxTotalAndRxToal(@NonNull Context context,
                                                        long txIncrement, long rxIncrement){
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        long rxTotal = sharedPreferences.getLong(SharedPreferenceKey.RX_TOTAL, 0) + rxIncrement;
        long txTotal = sharedPreferences.getLong(SharedPreferenceKey.TX_TOTAL, 0) + txIncrement;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(SharedPreferenceKey.RX_TOTAL, rxTotal).putLong(SharedPreferenceKey.TX_TOTAL, txTotal).apply();
    }

    public static final long getRxTotal(@NonNull Context context){
        return getDefaultSharedPreferences(context).getLong(SharedPreferenceKey.RX_TOTAL, 0);
    }

    public static final long getTxTotal(@NonNull Context context){
        return getDefaultSharedPreferences(context).getLong(SharedPreferenceKey.TX_TOTAL, 0);
    }

}
