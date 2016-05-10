package yyf.shadowsocks.preferences;

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
