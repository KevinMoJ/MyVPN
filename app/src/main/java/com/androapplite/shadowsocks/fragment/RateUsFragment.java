package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RateUsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RateUsFragment extends Fragment {

    public RateUsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rate_us, container, false);
    }

}
