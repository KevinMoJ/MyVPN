package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionFragment extends Fragment {
    private ImageButton mConnectionButton;
    private FrameLayout mConnectionButtonFrameLayout;

    public ConnectionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connection, container, false);
        mConnectionButtonFrameLayout = (FrameLayout)rootView.findViewById(R.id.connection_button_bg);
        mConnectionButton = (ImageButton)rootView.findViewById(R.id.connection_button_windmill);
        initConnectionButton();
        return rootView;
    }

    private void initConnectionButton(){
        mConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnectionButton.getAnimation() == null){
                    connect();
                }else{
                    stop();
                }
            }
        });
    }

    private void connect(){
        Animation rotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        mConnectionButton.startAnimation(rotate);
        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_connecting);

    }

    private void stop(){
        mConnectionButton.clearAnimation();
        mConnectionButtonFrameLayout.setBackgroundResource(R.drawable.connection_button_nomal);
    }

}
