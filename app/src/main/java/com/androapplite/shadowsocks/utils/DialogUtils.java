package com.androapplite.shadowsocks.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.activity.LuckRotateActivity;
import com.androapplite.shadowsocks.activity.VIPActivity;
import com.androapplite.shadowsocks.connect.ConnectVpnHelper;
import com.androapplite.vpn3.R;


public class DialogUtils {
    /*游戏获得金币的Dialog*/
    public static Dialog showGameGetMoneyDialog(Context context, String get, String todayReward, DialogInterface.OnDismissListener dismissListener) {
        final Dialog dialog = new Dialog(context, R.style.transpanrent_theme);
        dialog.setContentView(R.layout.dialog_game_get_money_dialog);
        dialog.setOnDismissListener(dismissListener);
        TextView tvGet = dialog.findViewById(R.id.tv_get_money);
        TextView tvEarnToday = dialog.findViewById(R.id.tv_earn_today);
        TextView tvTryAgain = dialog.findViewById(R.id.tv_try_again);
        TextView tvNoThanks = dialog.findViewById(R.id.tv_no_thanks);
        ImageView imageView = dialog.findViewById(R.id.iv_money_icon);

        tvTryAgain.setBackground(context.getResources().getDrawable(R.drawable.luck_pan_bt_bg));
        imageView.setImageResource(R.mipmap.ic_launcher);
        tvGet.setText(get);
        tvEarnToday.setText(todayReward);
        tvTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        tvNoThanks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }

    public static Dialog showVIPWelcomeDialog(Context context, DialogInterface.OnDismissListener dismissListener) {
        final Dialog dialog = new Dialog(context, R.style.transpanrent_theme);
        dialog.setContentView(R.layout.dialog_vip_welcome);
        dialog.setOnDismissListener(dismissListener);
        Button vipDialogBt = (Button) dialog.findViewById(R.id.vip_dialog_bt);
        vipDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        vipDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }

    public static Dialog showFreeUseOverDialog(final Context context, int type) {
        final Dialog dialog = new Dialog(context, R.style.transpanrent_theme);
        final Firebase firebase = Firebase.getInstance(context);
        dialog.setContentView(R.layout.dialog_free_time_over);
        Button vipDialogBt = (Button) dialog.findViewById(R.id.free_time_over_join_vip);
        Button cancelDialogBt = (Button) dialog.findViewById(R.id.free_time_over_cancel);
        vipDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VIPActivity.startVIPActivity(context, VIPActivity.TYPE_FREE_TIME_OVER);
                firebase.logEvent("免费使用弹窗", "点击跳转vip购买界面");
                dialog.dismiss();
            }
        });
        cancelDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LuckRotateActivity.startLuckActivity(context);
                firebase.logEvent("免费使用弹窗", "跳转到转盘界面");
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        if (type == ConnectVpnHelper.FREE_OVER_DIALOG_NET_SPEED)
            firebase.logEvent("免费使用弹窗", "显示", "加速页");
        else if (type == ConnectVpnHelper.FREE_OVER_DIALOG_MAIN)
            firebase.logEvent("免费使用弹窗", "显示", "主界面链接");
        else if (type == ConnectVpnHelper.FREE_OVER_DIALOG_SERVER_LIST)
            firebase.logEvent("免费使用弹窗", "显示", "免费服务器列表页");
        else if (type == ConnectVpnHelper.FREE_OVER_DIALOG_AUTO)
            firebase.logEvent("免费使用弹窗", "显示", "自动断开显示");
        return dialog;
    }
}
