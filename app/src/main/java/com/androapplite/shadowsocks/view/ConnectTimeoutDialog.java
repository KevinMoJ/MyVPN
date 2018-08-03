package com.androapplite.shadowsocks.view;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.vpn3.R;

public class ConnectTimeoutDialog extends DialogFragment {
    private static final String TAG = "ConnectTimeoutDialog";

    private TextView mChangeBt, mAgainBt, mCancelBt;
    private OnDialogBtClickListener mListener;

    public interface OnDialogBtClickListener {
        void onChangeServer();

        void onTryAgain();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Translucent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_connect_timeout, container, false);
        initView(view);
        initData();
        return view;
    }

    private void initView(View view) {
        mChangeBt = (TextView) view.findViewById(R.id.dialog_connect_timeout_change);
        mAgainBt = (TextView) view.findViewById(R.id.dialog_connect_timeout_again);
        mCancelBt = (TextView) view.findViewById(R.id.dialog_connect_timeout_cancel);

        mChangeBt.setOnClickListener(mOnClickListener);
        mAgainBt.setOnClickListener(mOnClickListener);
        mCancelBt.setOnClickListener(mOnClickListener);
    }

    private void initData() {
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDialogBtClickListener) {
            mListener = (OnDialogBtClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnDialogBtClickListener) {
            mListener = (OnDialogBtClickListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                dismissAllowingStateLoss();
                switch (v.getId()) {
                    case R.id.dialog_connect_timeout_change:
                        mListener.onChangeServer();
                        break;
                    case R.id.dialog_connect_timeout_again:
                        mListener.onTryAgain();
                        break;
                    case R.id.dialog_connect_timeout_cancel:
                        break;
                }
            }
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
        if (mListener != null)
            mListener = null;
    }
}
