package com.androapplite.shadowsocks.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.vpn3.R;

/**
 * Created by KevinMo.J on 2018/7/10.
 */

public class VIPWelcomeDialog extends DialogFragment {
    private Button mVipDialogBt;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Translucent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_vip_welcome, container, false);

        mVipDialogBt = (Button) view.findViewById(R.id.vip_dialog_bt);
        mVipDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        Firebase.getInstance(getContext()).logEvent("VIP购买成功弹窗", "显示");
        return view;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
