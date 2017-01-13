package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.animation.AnimatorListenerCompat;
import android.support.v4.animation.ValueAnimatorCompat;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RunnableFuture;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
//    private ImageView mJaguarImageView;
//    private ImageView mJaguarAnimationImageView;
    private OnConnectActionListener mListener;
//    private ImageButton mConnectButton;
//    private ProgressBar mProgressBar;
//    private boolean mIsSuccess;
    private TextView mMessageTextView;
//    private TextView mElapseTextView;
    private Button mConnectButton;
    private ImageView mLoadingView;


    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
//        mConnectButton = (ImageButton) view.findViewById(R.id.connect_button);
//        mConnectButton.setOnClickListener(this);
//        mJaguarImageView = (ImageView)view.findViewById(R.id.jaguar_image_view);
//        mJaguarAnimationImageView = (ImageView)view.findViewById(R.id.jaguar_animation_image_view);
//        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_bar);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
//        mElapseTextView = (TextView)view.findViewById(R.id.elapse);
        mConnectButton = (Button)view.findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(this);
        mLoadingView = (ImageView)view.findViewById(R.id.loading);
        return view;
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            mListener.onConnectButtonClick();
        }


//        switch (v.getId()){
//            case R.id.connect_button:
//                if(rotateAnimation == null) {
//                    rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
//                    mLoadingView.startAnimation(rotateAnimation);
//                }else{
//                    mLoadingView.clearAnimation();
//                    rotateAnimation = null;
//                }
//                break;
//        }

    }

    public interface OnConnectActionListener{
        void onConnectButtonClick();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnConnectActionListener){
            mListener = (OnConnectActionListener) context;
        }else{
            throw new ClassCastException(context.getClass().getSimpleName() + " must implement " + OnConnectActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof OnConnectActionListener){
            mListener = (OnConnectActionListener) activity;
        }else{
            throw new ClassCastException(activity.getClass().getSimpleName() + " must implement " + OnConnectActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoadingView.clearAnimation();
//        Timer timer = (Timer) mElapseTextView.getTag();
//        if(timer != null){
//            timer.cancel();
//            timer.purge();
//            mElapseTextView.setTag(null);
//        }

    }

    public void animateConnecting(){
        startAnimation();
        mConnectButton.setText(R.string.disconnect);
        mMessageTextView.setText(R.string.connecting);
        mLoadingView.setColorFilter(getResources().getColor(R.color.animation_color));
//        Runnable showDisconnectDelayRunnable = (Runnable)mConnectButton.getTag();
//        if(showDisconnectDelayRunnable == null){
//            showDisconnectDelayRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    mConnectButton.setVisibility(View.VISIBLE);
//                    mConnectButton.setImageLevel(1);
//                }
//            };
//            mConnectButton.setTag(showDisconnectDelayRunnable);
//        }
//        mConnectButton.postDelayed(showDisconnectDelayRunnable, 20000);

    }

    private void startAnimation(){
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        mLoadingView.startAnimation(animation);
    }


    public void animateStopping(){
        startAnimation();
        mConnectButton.setText(R.string.connect);
        mMessageTextView.setText(R.string.stopping);
    }

    public void setConnectResult(final Constants.State state){
        switch (state){
            case INIT:
                break;
            case CONNECTING:
                animateConnecting();
                break;
            case CONNECTED:
                connectFinish();
                break;
            case STOPPING:
                animateStopping();
                break;
            case STOPPED:
                stopFinish();
                break;
            case ERROR:
                error();
                break;
        }

    }

    private void connectFinish(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.connect_color));
    }

    private void stopFinish(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.connect_color));
    }

    private void error(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.error_color));

    }

    public void addProgress(int millisecond){
//        if(mProgressBar != null) {
//            mProgressBar.setProgress(mProgressBar.getProgress() + millisecond);
//        }
    }
}
