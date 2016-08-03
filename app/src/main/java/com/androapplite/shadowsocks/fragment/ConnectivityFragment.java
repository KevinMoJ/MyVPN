package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
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
public class ConnectivityFragment extends Fragment {


    private ProgressBar mProgressBar;
    private AnimatorSet mAnimatorSet;
    private ImageButton mConnectButton;
    private View mMessageView;
    private TextView mMessageTextView;
    private ImageView mMessageArrowView;
    private ImageView mConnectedImageView;
    private OnFragmentInteractionListener mListener;

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
        return rootView;
    }

    @NonNull
    private void initProgressBar() {
        mAnimatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.connecting);
        mAnimatorSet.getChildAnimations().get(0).addListener(new AnimatorListenerAdapter() {
            private int mCount = 0;

            @Override
            public void onAnimationEnd(Animator animation) {
                error();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                if (mCount == 0) {
                    mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.connecting_phase_2));
                    mProgressBar.setBackgroundResource(R.drawable.connecting_phase_1_end);
                } else if (mCount == 1) {
                    mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.connecting_phase_3));
                    mProgressBar.setBackgroundResource(R.drawable.connecting_phase_2_end);
                }
                mCount++;
            }
        });
        mAnimatorSet.setTarget(mProgressBar);
        mProgressBar.clearAnimation();
    }

    public void connecting(){
        mAnimatorSet.start();
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
                if(mListener != null){
                    mListener.onClickConnectionButton();
                }
            }
        });
    }

    private void showConnectMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.connect_success);
        mMessageTextView.setBackgroundResource(R.drawable.message_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.message_green_color));
        mConnectedImageView.setVisibility(View.VISIBLE);
        mMessageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMessageView.setVisibility(View.INVISIBLE);
                mConnectedImageView.setVisibility(View.GONE);
            }
        }, 1000);
    }

    private void showErrorMessage(){
        mMessageView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.retry);
        mMessageTextView.setBackgroundResource(R.drawable.message_error_frame);
        mMessageArrowView.setColorFilter(getResources().getColor(R.color.message_red_color));
        mMessageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMessageView.setVisibility(View.INVISIBLE);
            }
        }, 1000);
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
