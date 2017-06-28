package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.bestgo.adsplugin.ads.AdAppHelper;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisconnectFragment extends DialogFragment implements View.OnClickListener{
    private OnDisconnectActionListener mListener;
    private FrameLayout mAdLayout;

    public DisconnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_disconnect, container, false);
        v.findViewById(R.id.cancel_disconnect).setOnClickListener(this);
        v.findViewById(R.id.disconnect).setOnClickListener(this);
        mAdLayout = (FrameLayout)v.findViewById(R.id.adContainer);
        mAdLayout.setVisibility(View.GONE);
        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getContext());
        Firebase firebase = Firebase.getInstance(getContext());
        if(adAppHelper.isNativeLoaded()){
            if(shouldShowOrLoadAds()){
                try {
                    mAdLayout.addView(adAppHelper.getNative());
                    mAdLayout.setVisibility(View.VISIBLE);
                } catch (Exception ex) {
                    ShadowsocksApplication.handleException(ex);
                }
                firebase.logEvent("广告", "native加载成功", "断开连接");
            }else {
                firebase.logEvent("广告", "native加载成功但不显示", "断开连接");
            }
        }else{
            firebase.logEvent("广告", "native没有加载成功", "断开连接");
        }

        return  v;
    }

    private boolean shouldShowOrLoadAds(){
        boolean shouldShow = true;
        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getContext());
        String defaultChange = adAppHelper.getCustomCtrlValue("default", "1");
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(getContext());
        String city = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
        if(city != null){
            String chanceString = adAppHelper.getCustomCtrlValue(city, defaultChange);
            float chance = 1;
            try {
                chance = Float.parseFloat(chanceString);
                if(chance < 0){
                    chance = 0;
                }else if(chance > 1){
                    chance = 1;
                }
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
            }

            float random = (float) Math.random();
            shouldShow = random < chance;
        }
        return  shouldShow;

    }

    public interface OnDisconnectActionListener{
        void onCancel(DisconnectFragment disconnectFragment);
        void onDisconnect(DisconnectFragment disconnectFragment);
        void onDismiss(DisconnectFragment disconnectFragment);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDisconnectActionListener) {
            mListener = (OnDisconnectActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnDisconnectActionListener) {
            mListener = (OnDisconnectActionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            dismissAllowingStateLoss();
            switch (v.getId()){
                case R.id.cancel_disconnect:
                    mListener.onCancel(this);
                    break;
                case R.id.disconnect:
                    mListener.onDisconnect(this);
                    break;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(STYLE_NO_TITLE);
        return dialog;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(mListener != null){
            mListener.onDismiss(this);
        }
        super.onDismiss(dialog);
    }
}
