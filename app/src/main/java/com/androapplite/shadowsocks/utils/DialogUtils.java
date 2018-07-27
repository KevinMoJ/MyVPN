package com.androapplite.shadowsocks.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
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
    public static Dialog showGameGetTimeDialog(Context context, String freeTime, DialogInterface.OnDismissListener dismissListener) {
        final Dialog dialog = new Dialog(context, R.style.transpanrent_theme);
        dialog.setContentView(R.layout.dialog_game_get_money_dialog);
        if (dismissListener != null)
            dialog.setOnDismissListener(dismissListener);
        ImageView bigIcon = dialog.findViewById(R.id.dialog_free_icon);
        TextView title = dialog.findViewById(R.id.dialog_free_title);
        TextView makePersistentE = dialog.findViewById(R.id.dialog_free_make);
        TextView message = dialog.findViewById(R.id.dialog_free_message);
        TextView tryAgain = dialog.findViewById(R.id.dialog_free_bt);
        boolean isWin = !freeTime.equals("thanks");
        long TotalFreeTime = RuntimeSettings.getLuckPanGetRecord();
        if (isWin) {
            title.setText(context.getResources().getString(R.string.add_minutes, freeTime));
            makePersistentE.setText(context.getResources().getString(R.string.congratulations));
        } else {
            title.setText(context.getResources().getString(R.string.add_minutes, "0"));
            makePersistentE.setText(context.getResources().getString(R.string.make_persistent_efforts));
        }

        bigIcon.setImageResource(R.drawable.luck_pan_price_icon);
        message.setText(getColorText(context.getResources().getString(R.string.cumulative_use_duration, String.valueOf(TotalFreeTime)), String.valueOf(TotalFreeTime), Color.YELLOW));

        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                    return true;
                return false;
            }
        });
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }

    private static SpannableString getColorText(String text, String colorText, int color) {
        SpannableString spannableString = new SpannableString(text);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        spannableString.setSpan(colorSpan, text.indexOf(colorText), text.indexOf(colorText) + colorText.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static Dialog showVIPWelcomeDialog(Context context, DialogInterface.OnDismissListener dismissListener) {
        final Dialog dialog = new Dialog(context, R.style.transpanrent_theme);
        dialog.setContentView(R.layout.dialog_vip_welcome);
        if (dismissListener != null)
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
