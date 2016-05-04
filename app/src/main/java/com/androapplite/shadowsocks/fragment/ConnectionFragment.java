package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionFragment extends Fragment {
    private Button mConnectionButton;
    private ImageView mWindMillImageView;
//    private FrameLayout mConnectionButtonFrameLayout;
    private TextView mConnectionMessageTextView;
    private OnFragmentInteractionListener mListener;

    public ConnectionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connection, container, false);
//        mConnectionButtonFrameLayout = (FrameLayout)rootView.findViewById(R.id.connection_button_bg);
        mWindMillImageView = (ImageView)rootView.findViewById(R.id.connection_button_windmill);
        mConnectionButton = (Button)rootView.findViewById(R.id.connection_button);
        mConnectionMessageTextView = (TextView)rootView.findViewById(R.id.connection_message);
        initConnectionButton();
        return rootView;
    }

    private void initConnectionButton(){
        mConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/*                if (mWindMillImageView.getVisibility() != View.VISIBLE) {
                    connecting();
                } else {
                    stop();
                }*/
                if(mListener != null){
                    mListener.onClickConnectionButton();
                }
            }
        });
    }

    public void connecting(){
        mConnectionButton.setText(R.string.connecting);
//        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_normal);
        mConnectionMessageTextView.setText(R.string.please_wait);
        mWindMillImageView.setBackgroundResource(R.drawable.connection_button_windmill);
        Animation rotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        mWindMillImageView.startAnimation(rotate);
        mWindMillImageView.setVisibility(View.VISIBLE);
    }

    public void stop(){
        mConnectionButton.setText(R.string.tap_to_connect);
//        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_normal);
        mConnectionMessageTextView.setText(R.string.tap_to_connect_explain);
        mWindMillImageView.clearAnimation();
        mWindMillImageView.setVisibility(View.GONE);
    }

    public void connected(){
        mConnectionButton.setText(R.string.tap_to_disconnect);
//        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_conneced);
        mConnectionMessageTextView.setText(R.string.connect_success);
        mWindMillImageView.clearAnimation();
        mWindMillImageView.setVisibility(View.VISIBLE);
        mWindMillImageView.setBackgroundResource(R.drawable.connection_button_success);
    }

    protected void error(){
        mConnectionButton.setText("");
//        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_error);
        mConnectionMessageTextView.setText("");
        mWindMillImageView.clearAnimation();
        mWindMillImageView.setVisibility(View.VISIBLE);
        mWindMillImageView.setBackgroundResource(R.drawable.connection_button_fail);
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
