package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.R;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionIndicatorFragment extends Fragment {


    public ConnectionIndicatorFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connection_indicator, container, false);
    }

    public void updateConnectionState(int state){
        View rootView = getView();
        if(state == Constants.State.CONNECTING.ordinal()){
        }else if(state == Constants.State.CONNECTED.ordinal()){
            rootView.setEnabled(true);
            rootView.setVisibility(View.VISIBLE);
        }else if(state == Constants.State.ERROR.ordinal()){
            rootView.setEnabled(false);
            rootView.setVisibility(View.VISIBLE);
        }else if(state == Constants.State.INIT.ordinal()){
        }else if(state == Constants.State.STOPPING.ordinal()){
            rootView.setEnabled(false);
            rootView.setVisibility(View.VISIBLE);
        }else if(state == Constants.State.STOPPED.ordinal()){
            rootView.setEnabled(false);
            rootView.setVisibility(View.VISIBLE);
        }
    }

}
