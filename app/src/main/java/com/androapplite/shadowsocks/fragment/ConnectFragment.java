package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.animation.AnimatorListenerCompat;
import android.support.v4.animation.ValueAnimatorCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener, Animator.AnimatorListener{
    private ImageView mJaguarImageView;
    private ImageView mJaguarAnimationImageView;
    private OnConnectActionListener mListener;
    private ImageButton mConnectButton;
    private ProgressBar mProgressBar;
    private boolean mIsSuccess;
    private TextView mMessageTextView;
    private TextView mElapseTextView;


    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        mConnectButton = (ImageButton) view.findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(this);
        mJaguarImageView = (ImageView)view.findViewById(R.id.jaguar_image_view);
        mJaguarAnimationImageView = (ImageView)view.findViewById(R.id.jaguar_animation_image_view);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_bar);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
        mElapseTextView = (TextView)view.findViewById(R.id.elapse);
        return view;
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            mListener.onConnectButtonClick();
        }

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
        ObjectAnimator progressAnimator = (ObjectAnimator) mProgressBar.getTag();
        if(progressAnimator != null) {
            progressAnimator.end();
        }
    }

    public void startAnimation(){
        mJaguarImageView.setVisibility(View.INVISIBLE);
        mJaguarAnimationImageView.setVisibility(View.VISIBLE);
        AnimationDrawable animationDrawable = (AnimationDrawable)mJaguarAnimationImageView.getDrawable();
        animationDrawable.start();
        mConnectButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", 0, mProgressBar.getMax());
        progressAnimator.setDuration(15000);
        progressAnimator.start();
        mProgressBar.setTag(progressAnimator);
        mMessageTextView.setText(R.string.connecting);

    }

    public void setConnectResult(boolean success){
        ObjectAnimator progressAnimator = (ObjectAnimator) mProgressBar.getTag();
        if(progressAnimator != null) {
            progressAnimator.end();
        }
        progressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", mProgressBar.getProgress(), mProgressBar.getMax());
        progressAnimator.setDuration(200);
        progressAnimator.addListener(this);
        progressAnimator.start();
        mProgressBar.setTag(progressAnimator);
        mIsSuccess = success;
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mJaguarImageView.setVisibility(View.VISIBLE);
        mJaguarAnimationImageView.setVisibility(View.INVISIBLE);
        AnimationDrawable animationDrawable = (AnimationDrawable)mJaguarAnimationImageView.getDrawable();
        animationDrawable.stop();
        mConnectButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mProgressBar.setTag(null);
        if(mIsSuccess){
            mJaguarImageView.setImageLevel(1);
            mConnectButton.setImageLevel(1);
            mMessageTextView.setText(R.string.connected);
            mElapseTextView.setVisibility(View.VISIBLE);
            Timer
        }else{
            mJaguarImageView.setImageLevel(0);
            mConnectButton.setImageLevel(0);
            mMessageTextView.setText(R.string.retry);
            mElapseTextView.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
