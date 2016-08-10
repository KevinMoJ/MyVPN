package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionIndicatorFragment extends Fragment {

    private TextView mIndicatorMsgTextView;
    private ImageView mIndicatorIconImageView;
    public ConnectionIndicatorFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.fragment_connection_indicator, container, false);
        mIndicatorIconImageView = (ImageView)v.findViewById(R.id.indicator_icon);
        mIndicatorMsgTextView = (TextView)v.findViewById(R.id.indicator_msg);
        return v;
    }

    public void updateConnectionState(int state){
        View rootView = getView();
        if(state == Constants.State.CONNECTING.ordinal()){
        }else if(state == Constants.State.CONNECTED.ordinal()){
            mIndicatorIconImageView.setEnabled(true);
            mIndicatorMsgTextView.setText(R.string.connected);
            rootView.setVisibility(View.VISIBLE);
        }else if(state == Constants.State.ERROR.ordinal()){
            mIndicatorIconImageView.setEnabled(false);
            mIndicatorMsgTextView.setText(R.string.disconnected);
            rootView.setVisibility(View.VISIBLE);
        }else if(state == Constants.State.INIT.ordinal()){
        }else if(state == Constants.State.STOPPING.ordinal()){
        }else if(state == Constants.State.STOPPED.ordinal()){
            mIndicatorIconImageView.setEnabled(false);
            mIndicatorMsgTextView.setText(R.string.disconnected);
            rootView.setVisibility(View.VISIBLE);
        }
    }

}
