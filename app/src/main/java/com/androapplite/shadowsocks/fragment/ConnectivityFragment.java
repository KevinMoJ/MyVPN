package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
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
    private TextView mStatusTextView;
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
        mStatusTextView = (TextView)rootView.findViewById(R.id.status);
        return rootView;
    }

    private void showStatusViewConnected(){

        mStatusTextView.setVisibility(View.VISIBLE);
        mStatusTextView.setEnabled(true);
        mStatusTextView.setText(R.string.connected);
    }

    private void showStatusViewDisconnected(){
        mStatusTextView.setVisibility(View.VISIBLE);
        mStatusTextView.setEnabled(false);
        mStatusTextView.setText(R.string.disconnected);
    }

    private void initProgressBar() {
        mAnimatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.connecting);
        mAnimatorSet.getChildAnimations().get(0).addListener(new AnimatorListenerAdapter() {
            private int mIndex;

            @Override
            public void onAnimationStart(Animator animation) {
                mIndex = 0;
                initProgressBarStartState();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                error();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                mProgressBar.setProgress(mProgressBar.getMax());
                mProgressBar.setBackground(mProgressBar.getProgressDrawable());
                mIndex = (++mIndex) % LOADING_PHASE_DRAWABLE_RESOURCE.length;
                mProgressBar.setProgressDrawable(getResources().getDrawable(LOADING_PHASE_DRAWABLE_RESOURCE[mIndex]));
            }


        });
        mAnimatorSet.setTarget(mProgressBar);
        mProgressBar.clearAnimation();
    }



    private void initProgressBarStartState() {
        mProgressBar.setProgress(0);
        mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.connecting_phase_1));
        mProgressBar.setBackground(null);
    }

    public void connecting(){
        if(!mAnimatorSet.isRunning()) {
            ((ObjectAnimator) mAnimatorSet.getChildAnimations().get(0)).setRepeatCount(ValueAnimator.INFINITE);
            mAnimatorSet.start();
        }
    }

    public void stopping(){
        if(!mAnimatorSet.isRunning()) {
            ((ObjectAnimator) mAnimatorSet.getChildAnimations().get(0)).setRepeatCount(0);
            ((ObjectAnimator) mAnimatorSet.getChildAnimations().get(0)).reverse();
            initProgressBarStartState();
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
                connecting();
//                if (mListener != null) {
//                    mListener.onClickConnectionButton();
//                }
            }
        });
    }

    private void stopped() {
        ((ObjectAnimator)mAnimatorSet.getChildAnimations().get(0)).cancel();
        initProgressBarStartState();
    }

    private void showConnectingMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.connecting);
        mMessageTextView.setBackgroundResource(R.drawable.message_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.message_green_color));
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
                    }
                }, 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mMessageView.startAnimation(animation);
    }

    private void showConnectMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.connected);
        mMessageTextView.setBackgroundResource(R.drawable.message_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.message_green_color));
        mConnectedImageView.setVisibility(View.VISIBLE);

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

    private void showStoppingMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.stopping);
        mMessageTextView.setBackgroundResource(R.drawable.message_stop_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.stop_yellow));

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
                    }
                }, 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mMessageView.startAnimation(animation);
    }

    private void showStoppedMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.stoped);
        mMessageTextView.setBackgroundResource(R.drawable.message_stop_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.stop_yellow));

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
                    }
                }, 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mMessageView.startAnimation(animation);
    }

    private void showErrorMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.retry);
        mMessageTextView.setBackgroundResource(R.drawable.message_error_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.message_red_color));
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
                    }
                }, 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
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
        mAnimatorSet.end();

    }

    public void updateConnectionState(int state){
        if(state == Constants.State.CONNECTING.ordinal()){
            connecting();
            showConnectingMessage();
        }else if(state == Constants.State.CONNECTED.ordinal()){
            connected();
            showConnectMessage();
            showStatusViewConnected();
        }else if(state == Constants.State.ERROR.ordinal()){
            error();
            showErrorMessage();
            showStatusViewDisconnected();
        }else if(state == Constants.State.INIT.ordinal()){
        }else if(state == Constants.State.STOPPING.ordinal()){
            stopping();
            showStoppingMessage();
            showStatusViewDisconnected();
        }else if(state == Constants.State.STOPPED.ordinal()){
            showStoppedMessage();
            stopped();
            showStatusViewDisconnected();
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
