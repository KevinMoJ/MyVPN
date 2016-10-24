package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectivityFragment extends Fragment {


    private ProgressBar mProgressBar;
    private AnimatorSet mAnimatorSet;
    private ImageButton mConnectButton;
    private View mMessageView;
    private TextView mMessageTextView;
    private ImageView mMessageArrowView;
    private ImageView mConnectedImageView;
    private OnFragmentInteractionListener mListener;
    private static final int[] LOADING_PHASE_DRAWABLE_RESOURCE = {
            R.drawable.connecting_phase_1,
            R.drawable.connecting_phase_2,
            R.drawable.connecting_phase_3,
            R.drawable.connecting_phase_4,
            R.drawable.connecting_phase_5,
            R.drawable.connecting_phase_6,
            R.drawable.connecting_phase_7,
            R.drawable.connecting_phase_8,
    };
    private static final int[] LOADING_PHASE_DRAWABLE_RESOURCE_END = {
            R.drawable.connecting_phase_1_end,
            R.drawable.connecting_phase_2_end,
            R.drawable.connecting_phase_3_end,
            R.drawable.connecting_phase_4_end,
            R.drawable.connecting_phase_5_end,
            R.drawable.connecting_phase_6_end,
            R.drawable.connecting_phase_7_end,
            R.drawable.connecting_phase_8_end,
    };
    private boolean mIsReverseAnimation;
    private int colorGreen;
    private int colorRed;
    private int colorYellow;

    public ConnectivityFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connectivity, container, false);
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);
        initProgressBar();
        mConnectButton = (ImageButton)rootView.findViewById(R.id.connection_button);
        initConnectButton();

        mMessageView = rootView.findViewById(R.id.message);
        mMessageTextView = (TextView)rootView.findViewById(R.id.message_body);
        mMessageArrowView = (ImageView)rootView.findViewById(R.id.message_arrow);
        mConnectedImageView = (ImageView)rootView.findViewById(R.id.connected);
        Resources resources = getResources();
        colorGreen = resources.getColor(R.color.message_green_color);
        colorRed = resources.getColor(R.color.message_red_color);
        colorYellow = resources.getColor(R.color.stop_yellow);
        return rootView;
    }

    private void initProgressBar() {
        mAnimatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.connecting);
        mAnimatorSet.getChildAnimations().get(0).addListener(new AnimatorListenerAdapter() {
            private int mIndex;

            @Override
            public void onAnimationStart(Animator animation) {
//                ShadowsocksApplication.debug("animation", "start");
                if(isAdded()){
                    if(mIsReverseAnimation){
                        mIndex = LOADING_PHASE_DRAWABLE_RESOURCE_END.length - 1;
                        mProgressBar.setProgressDrawable(getResources().getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE[0]));
                        mProgressBar.setBackground(getResources().getDrawable(R.drawable.connecting_phase_8_end));
                    }else{
                        mIndex = 0;
                        mProgressBar.setProgressDrawable(getResources().getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE[0]));
                        mProgressBar.setBackground(null);
                    }
                }

            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
//                ShadowsocksApplication.debug("animation", mIndex + "");
                try{
                    Resources resources = getResources();
                    if (mIsReverseAnimation) {
                        mProgressBar.setProgressDrawable(resources.getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE[mIndex]));
                        mIndex = (--mIndex + LOADING_PHASE_DRAWABLE_RESOURCE_END.length) % LOADING_PHASE_DRAWABLE_RESOURCE.length;
                        mProgressBar.setBackground(resources.getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE_END[mIndex]));

                    } else {
                        mProgressBar.setBackground(resources.getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE_END[mIndex]));
                        mIndex = (++mIndex) % LOADING_PHASE_DRAWABLE_RESOURCE.length;
                        mProgressBar.setProgressDrawable(resources.getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE[mIndex]));
                    }
                }catch (Exception e){
                    ShadowsocksApplication.handleException(e);
                }
            }


        });
        mAnimatorSet.setTarget(mProgressBar);
        mProgressBar.clearAnimation();
    }

    public void connecting(){
        if(!mAnimatorSet.isRunning()) {
            mIsReverseAnimation = false;
            mAnimatorSet.start();
        }
    }

    public void stopping(){

        if(mAnimatorSet.isRunning() && !mIsReverseAnimation){
            mAnimatorSet.cancel();
        }
        if(!mAnimatorSet.isRunning()){
            //准备倒转
            mIsReverseAnimation = true;
            ((ObjectAnimator) mAnimatorSet.getChildAnimations().get(0)).reverse();
        }
    }

    public void connected(){
        mAnimatorSet.cancel();
        mProgressBar.setProgress(0);
        mProgressBar.setBackgroundResource(R.drawable.connect_success);
    }

    public void error(){
        mAnimatorSet.cancel();
        mProgressBar.setProgress(0);
        mProgressBar.setBackgroundResource(R.drawable.connect_error);
    }

    private void initConnectButton(){
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                stopping();
//                connecting();
                if (mListener != null) {
                    mListener.onClickConnectionButton();
                }
            }
        });
    }

    private void stopped() {
        ((ObjectAnimator)mAnimatorSet.getChildAnimations().get(0)).cancel();
        mProgressBar.setProgress(0);
        mProgressBar.setBackground(null);
    }

    private void showConnectingMessage(){
        if(isAdded()){
            mMessageTextView.setText(R.string.connecting);
            mMessageTextView.setBackgroundResource(R.drawable.message_frame);
            mMessageArrowView.setColorFilter(colorGreen);
            startMessageViewAnimation();
        }
    }

    private void showConnectMessage(){
        if(isAdded()){
            mMessageTextView.setText(R.string.connected);
            mMessageTextView.setBackgroundResource(R.drawable.message_frame);
            mMessageArrowView.setColorFilter(colorGreen);
            mConnectedImageView.setVisibility(View.VISIBLE);
            startMessageViewAnimation();
        }
    }

    private void showStoppingMessage(){
        if(isAdded()){
            mMessageTextView.setText(R.string.stopping);
            mMessageTextView.setBackgroundResource(R.drawable.message_stop_frame);
            mMessageArrowView.setColorFilter(colorYellow);
            startMessageViewAnimation();
        }
    }

    private void showStoppedMessage(){
        if(isAdded()){
            mMessageTextView.setText(R.string.stoped);
            mMessageTextView.setBackgroundResource(R.drawable.message_stop_frame);
            mMessageArrowView.setColorFilter(colorYellow);
            startMessageViewAnimation();
        }
    }

    private void showErrorMessage(){
        if(isAdded()){
            mMessageTextView.setText(R.string.retry);
            mMessageTextView.setBackgroundResource(R.drawable.message_error_frame);
            mMessageArrowView.setColorFilter(colorRed);
            startMessageViewAnimation();
        }
    }

    private void startMessageViewAnimation() {
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.clearAnimation();
        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.center_enlarge);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }


            @Override
            public void onAnimationEnd(Animation animation) {
                mMessageView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mMessageView.setVisibility(View.INVISIBLE);
                        //这条代码只对连接成功状态起作用
                        mConnectedImageView.setVisibility(View.INVISIBLE);
                    }
                }, 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mMessageView.startAnimation(animation);
    }

    public interface OnFragmentInteractionListener {
        void onClickConnectionButton();
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        initListener(context);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        initListener(context);
    }

    private void initListener(Context context) {
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
        mAnimatorSet.end();
    }

    public void updateConnectionState(int state){
        if(state == Constants.State.CONNECTING.ordinal()){
            connecting();
            showConnectingMessage();
        }else if(state == Constants.State.CONNECTED.ordinal()){
            connected();
            showConnectMessage();
        }else if(state == Constants.State.ERROR.ordinal()){
            error();
            showErrorMessage();
        }else if(state == Constants.State.INIT.ordinal()){
        }else if(state == Constants.State.STOPPING.ordinal()){
            stopping();
            showStoppingMessage();
        }else if(state == Constants.State.STOPPED.ordinal()){
            showStoppedMessage();
            stopped();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            if(mAnimatorSet.isPaused()){
                mAnimatorSet.resume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            if(mAnimatorSet.isRunning()){
                mAnimatorSet.pause();
            }
        }
    }

}
