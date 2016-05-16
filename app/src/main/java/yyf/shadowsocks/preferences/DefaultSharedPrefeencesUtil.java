package yyf.shadowsocks.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;


/**
 * Created by jim on 16/4/27.
 */
public class DefaultSharedPrefeencesUtil {
    private static final String PREFERENCE_NAME = "VPN_PREFERENCE";
    public static final SharedPreferences getDefaultSharedPreferences(@NonNull Context context){
        return  context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
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

/*    public static final long getLastResetMonth(@NonNull Context context){
        return getDefaultSharedPreferences(context).getInt(SharedPreferenceKey.LAST_RESET_MONTH, 0);
    }

    public static final void setLastResetMonth(@NonNull Context context, long date){
        getDefaultSharedPreferencesEditor(context).putLong(SharedPreferenceKey.LAST_RESET_MONTH, date).apply();
//        getDefaultSharedPreferencesEditor(context).putInt(SharedPreferenceKey.LAST_RESET_MONTH, month).apply();
    }*/

    public static final Calendar getLastResetMonth(@NonNull Context context){
        long date = getDefaultSharedPreferences(context).getLong(SharedPreferenceKey.LAST_RESET_MONTH, 0);
        Calendar calendar = Calendar.getInstance();
        if(date != 0) {
            calendar.setTimeInMillis(date);
        }
        return calendar;
    }

    public static final void setLastResetMonth(@NonNull Context context, @NonNull Calendar calendar){
        getDefaultSharedPreferencesEditor(context).putLong(SharedPreferenceKey.LAST_RESET_MONTH, calendar.getTimeInMillis()).apply();
    }

}
