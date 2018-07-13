package com.androapplite.shadowsocks.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
}
