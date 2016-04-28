package com.androapplite.shadowsocks.fragment;


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
    private FrameLayout mConnectionButtonFrameLayout;
    private TextView mConnectionMessageTextView;

    public ConnectionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connection, container, false);
        mConnectionButtonFrameLayout = (FrameLayout)rootView.findViewById(R.id.connection_button_bg);
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
                if (mWindMillImageView.getVisibility() != View.VISIBLE) {
                    connecting();
                } else {
                    stop();
                }
            }
        });
    }

    private void connecting(){
        mConnectionButton.setText(R.string.connecting);
        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_nomal);
        mConnectionMessageTextView.setText(R.string.please_wait);

        Animation rotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        mWindMillImageView.startAnimation(rotate);
        mWindMillImageView.setVisibility(View.VISIBLE);
    }

    private void stop(){
        mConnectionButton.setText(R.string.tap_to_connect);
        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_nomal);
        mConnectionMessageTextView.setText(R.string.tap_to_connect_explain);
        mWindMillImageView.clearAnimation();
        mWindMillImageView.setVisibility(View.GONE);
    }

    private void connected(){
        mConnectionButton.setText(R.string.tap_to_disconnect);
        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_conneced);
        mConnectionMessageTextView.setText(R.string.connect_success);
        mWindMillImageView.clearAnimation();
        mWindMillImageView.setVisibility(View.GONE);
    }

    protected void error(){
        mConnectionButton.setText("");
        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_error);
        mConnectionMessageTextView.setText("");
        mWindMillImageView.clearAnimation();
        mWindMillImageView.setVisibility(View.GONE);
    }
}
