package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionFragment extends Fragment {
    private TextView mConnectionButtonTextView;
    private ImageButton mConnectionButton;
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
//        mConnectionMessageTextView = (TextView)rootView.findViewById(R.id.connection_message);
        initConnectionButton();
        return rootView;
    }

    private void initConnectionButton(){
        mConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mConnectionButton.setClickable(false);
                    mListener.onClickConnectionButton();
                }
            }
        });
    }

    public void connecting(){
        mConnectionButtonTextView.setText(R.string.connecting);
        mConnectionButtonTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
        Snackbar.make(mConnectionButtonTextView, R.string.please_wait,Snackbar.LENGTH_SHORT);
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_CONNECTING);
        mConnectionButton.clearAnimation();
        Animation rotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        mConnectionButton.startAnimation(rotate);
    }

    public void stop(){
        mConnectionButton.setClickable(true);
        mConnectionButtonTextView.setText(R.string.tap_to_connect);
        mConnectionButtonTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
        Snackbar.make(mConnectionButtonTextView, R.string.tap_to_connect_explain,Snackbar.LENGTH_SHORT).show();
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_STOP);
        mConnectionButton.clearAnimation();
        getView().invalidate();
    }

    public void connected(){
        mConnectionButton.setClickable(true);
        mConnectionButtonTextView.setText(R.string.connected);
        mConnectionButtonTextView.setTextColor(getResources().getColor(R.color.button_green));
        Snackbar.make(mConnectionButtonTextView, R.string.connect_success,Snackbar.LENGTH_SHORT).show();
        mConnectionButton.clearAnimation();
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_CONNECTED);
        getView().invalidate();
    }

    public void error(){
        mConnectionButton.setClickable(true);
        mConnectionButtonTextView.setText(R.string.connection_failed);
        mConnectionButtonTextView.setTextColor(getResources().getColor(R.color.connect_button_error));
        Snackbar.make(mConnectionButtonTextView, R.string.connection_failed_explain,Snackbar.LENGTH_SHORT).show();
        mConnectionButton.clearAnimation();
        mConnectionButton.setImageLevel(CONNECTION_BUTTON_STATE_ERROR);
        getView().invalidate();
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
