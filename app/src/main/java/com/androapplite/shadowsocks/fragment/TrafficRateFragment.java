package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class TrafficRateFragment extends Fragment {

    private TextView mTxRateValueText;
    private TextView mTxRateUnitText;
    private TextView mRxRateValueText;
    private TextView mRxRateUnitText;
    public TrafficRateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_traffic_rate, container, false);
        mRxRateValueText = (TextView)rootView.findViewById(R.id.incomming_rate);
        mRxRateUnitText = (TextView)rootView.findViewById(R.id.incomming_unite);
        mTxRateValueText = (TextView)rootView.findViewById(R.id.outgoing_rate);
        mTxRateUnitText = (TextView)rootView.findViewById(R.id.outgoing_unite);
        return rootView;
    }

    public void updateRate(String txRate, String rxRate){
        String[] tx = txRate.split(" ");
        mTxRateValueText.setText(tx[0]);
        mTxRateUnitText.setText(tx[1]);
        String[] rx = rxRate.split(" ");
        mRxRateValueText.setText(rx[0]);
        mRxRateUnitText.setText(rx[1]);
    }
}
