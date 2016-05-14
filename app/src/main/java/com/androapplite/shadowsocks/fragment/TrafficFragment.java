package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

import yyf.shadowsocks.utils.TrafficMonitor;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrafficFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrafficFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String USAGE = "USAGE";
    private static final String AVAILABLE = "AVAILABLE";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView mTrafficUseTextView;


    public TrafficFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param usage Parameter 1.
     * @param available Parameter 2.
     * @return A new instance of fragment TrafficFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TrafficFragment newInstance(String usage, String available) {
        TrafficFragment fragment = new TrafficFragment();
        Bundle args = new Bundle();
        args.putString(USAGE, usage);
        args.putString(AVAILABLE, available);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(USAGE);
            mParam2 = getArguments().getString(AVAILABLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_traffic, container, false);
        mTrafficUseTextView = (TextView)rootView.findViewById(R.id.traffic_use);
        return rootView;
    }

    public void updateTrafficUse(long dailyTxUsed, long dailyRxUsed, long monthlyTxUsed, long monthlyRxUsed){
        if(mTrafficUseTextView != null) {
            long total = dailyTxUsed + dailyRxUsed + monthlyTxUsed + monthlyRxUsed;
            String formatTotal = TrafficMonitor.formatTraffic(getActivity(), total);
            mTrafficUseTextView.setText(formatTotal);
        }

    }

}
