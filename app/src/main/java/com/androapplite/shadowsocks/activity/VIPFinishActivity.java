package com.androapplite.shadowsocks.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.utils.DialogUtils;
import com.androapplite.vpn3.R;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class VIPFinishActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "VIPFinishActivity";
    public static final String TYPE_VIP_PAY_FINISH = "TYPE_VIP_PAY_FINISH"; // 是VIP界面购买完成的，用来显示一次弹窗

    private TextView mVipFinishServerMessageText;
    private TextView mVipFinishPayAutomaticRenewal;
    private TextView mVipFinishPayType;
    private TextView mVipFinishPayRenewedTime;

    private ImageView mCloseImage;

    private SharedPreferences mSharedPreferences;
    private boolean isShowVIPDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vipfinsh);
        initData();
        initView();
        initUI();
    }

    private void initData() {
        mSharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        isShowVIPDialog = getIntent().getBooleanExtra(TYPE_VIP_PAY_FINISH, false);
    }

    private void initView() {
        mCloseImage = (ImageView) findViewById(R.id.vip_finish_close);

        mVipFinishServerMessageText = (TextView) findViewById(R.id.vip_finish_server_message_text);
        mVipFinishPayAutomaticRenewal = (TextView) findViewById(R.id.vip_finish_pay_automatic_renewal);
        mVipFinishPayType = (TextView) findViewById(R.id.vip_finish_pay_type);
        mVipFinishPayRenewedTime = (TextView) findViewById(R.id.vip_finish_pay_renewed_time);

        mCloseImage.setOnClickListener(this);
    }

    private void initUI() {
        mVipFinishServerMessageText.setText(getResources().getString(R.string.more_than_countries, String.valueOf(3)));

        boolean isPayOneMonth = mSharedPreferences.getBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, true);
        mVipFinishPayType.setText(getResources().getString(R.string.months_plan, isPayOneMonth ? "1" : "6"));

        long payTime = 0;
        payTime = mSharedPreferences.getLong(SharedPreferenceKey.VIP_PAY_TIME, 0);
        try {
            if (payTime != 0) {
                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
                String dateStr = dateformat.format(payTime + (TimeUnit.DAYS.toMillis(isPayOneMonth ? 30 : 180)));
                //2018/08/10
                String year = dateStr.substring(0, dateStr.indexOf("/"));
                String month = dateStr.substring(dateStr.indexOf("/") + 1, dateStr.lastIndexOf("/"));
                String day = dateStr.substring(dateStr.lastIndexOf("/") + 1, dateStr.length());
                String renewedTimeString = getEnglishMonth(month) + " " + day + "," + year;
                mVipFinishPayRenewedTime.setText(renewedTimeString);
            }
        } catch (Exception e) {
        }

        boolean isAuto = mSharedPreferences.getBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, true);
        mVipFinishPayAutomaticRenewal.setText(isAuto ? "ON" : "OFF");

        if (isShowVIPDialog) {
            Dialog dialog = DialogUtils.showVIPWelcomeDialog(this, new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {

                }
            });

            if (dialog.isShowing()) {
                Firebase.getInstance(this).logEvent("VIPDialog界面", "显示");
            }
        }

        Firebase.getInstance(this).logEvent("VIP购买成功弹窗", "显示");
    }

    private String getEnglishMonth(String month) {
        String EnglishMonth = "";
        if (month.equals("01"))
            EnglishMonth = "January";
        else if (month.equals("02"))
            EnglishMonth = "February";
        else if (month.equals("03"))
            EnglishMonth = "March";
        else if (month.equals("04"))
            EnglishMonth = "April";
        else if (month.equals("05"))
            EnglishMonth = "May";
        else if (month.equals("06"))
            EnglishMonth = "June";
        else if (month.equals("07"))
            EnglishMonth = "July";
        else if (month.equals("08"))
            EnglishMonth = "August";
        else if (month.equals("09"))
            EnglishMonth = "September";
        else if (month.equals("10"))
            EnglishMonth = "October";
        else if (month.equals("11"))
            EnglishMonth = "November";
        else if (month.equals("12"))
            EnglishMonth = "December";
        return EnglishMonth;
    }

    public static void startVIPFinishActivity(Context context, boolean showDialog) {
        Intent intent = new Intent(context, VIPFinishActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TYPE_VIP_PAY_FINISH, showDialog);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vip_finish_close:
                finish();
                break;
        }
    }
}
