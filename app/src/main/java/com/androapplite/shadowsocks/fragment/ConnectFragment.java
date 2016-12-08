package com.androapplite.shadowsocks.fragment;


import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
    ImageView mJaguarImageView;
    ImageView mJaguarAnimationImageView;

    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        view.findViewById(R.id.connect_button).setOnClickListener(this);
        mJaguarImageView = (ImageView)view.findViewById(R.id.jaguar_image_view);
        mJaguarAnimationImageView = (ImageView)view.findViewById(R.id.jaguar_animation_image_view);
        return view;
    }

    @Override
    public void onClick(View v) {
        AnimationDrawable animationDrawable = (AnimationDrawable)mJaguarAnimationImageView.getDrawable();
        animationDrawable.start();
    }
}
