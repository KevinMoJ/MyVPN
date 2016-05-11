package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionFragment extends Fragment {
    private TextView mConnectionButtonTextView;
    private ImageButton mConnectionButton;
    private TextView mConnectionMessageTextView;
    private OnFragmentInteractionListener mListener;
    private int count = 0;

    private static final int CONNECTION_BUTTON_STATE_STOP = 0;
    private static final int CONNECTION_BUTTON_STATE_CONNECTING = 1;
    private static final int CONNECTION_BUTTON_STATE_CONNECTED = 2;
    private static final int CONNECTION_BUTTON_STATE_ERROR = 3;
    public ConnectionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connection, container, false);
        mConnectionButton = (ImageButton)rootView.findViewById(R.id.connection_button);
        mConnectionButtonTextView = (TextView)rootView.findViewById(R.id.connection_button_text);
        mConnectionMessageTextView = (TextView)rootView.findViewById(R.id.connection_message);
        initConnectionButton();
        return rootView;
    }

    private void initConnectionButton(){
        mConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onClickConnectionButton();
                }
            }
        });
    }

    public void connecting(){
        mConnectionButtonTextView.setText(R.string.connecting);
        mConnectionMessageTextView.setText(R.string.please_wait);
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_CONNECTING);
        mConnectionButton.clearAnimation();
        Animation rotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        mConnectionButton.startAnimation(rotate);
    }

    public void stop(){
        mConnectionButtonTextView.setText(R.string.tap_to_connect);
        mConnectionMessageTextView.setText(R.string.tap_to_connect_explain);
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_STOP);
        mConnectionButton.clearAnimation();
    }

    public void connected(){
        mConnectionButtonTextView.setText(R.string.tap_to_disconnect);
        mConnectionMessageTextView.setText(R.string.connect_success);
        mConnectionButton.clearAnimation();
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_CONNECTED);
    }

    public void error(){
        mConnectionButtonTextView.setText(R.string.connection_failed);
        mConnectionMessageTextView.setText("");
        mConnectionButton.clearAnimation();
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_ERROR);
    }

    public interface OnFragmentInteractionListener {
        void onClickConnectionButton();
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
}
