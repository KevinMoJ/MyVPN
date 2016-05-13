package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RateUsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RateUsFragment extends Fragment {
    private Button mRateUsButton;
    private OnFragmentInteractionListener mListener;

    public RateUsFragment() {
        // Required empty public constructor
    }

    public static final RateUsFragment newInstance(){
        return new RateUsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_rate_us, container, false);
        mRateUsButton = (Button)rootView.findViewById(R.id.rate_us_btn);
        initRateUsButton();
        return rootView;
    }


    private void initRateUsButton(){
        mRateUsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRateUs();
                }
            }
        });
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onCloseRateUs();
        void onRateUs();
    }


}
