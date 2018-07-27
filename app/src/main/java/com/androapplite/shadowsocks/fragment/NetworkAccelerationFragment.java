package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class NetworkAccelerationFragment extends Fragment implements View.OnClickListener,
        Animator.AnimatorListener {
    private AnimatorSet mRocketShake;
    private NetworkAccelerationFragmentListener mListener;
    public boolean mNeedToShake = true; // 小火箭是否需要抖动
    private FrameLayout mAdContent;
    private boolean isNativeAddSuccess;
    private Button mRocketBt;

    public NetworkAccelerationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_network_acceleration, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mRocketBt = (Button) view.findViewById(R.id.acc_btn);
        mRocketBt.setOnClickListener(this);
        mAdContent = (FrameLayout) view.findViewById(R.id.net_speed_ad_content);
        if (!RuntimeSettings.isVIP())
            addBottomAd();
        else
            isNativeAddSuccess = false;
        if (!isNativeAddSuccess) {
            RelativeLayout.LayoutParams rocketBtLayoutParams = (RelativeLayout.LayoutParams) mRocketBt.getLayoutParams();
            rocketBtLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.banner_ad_loading_icon_margin_top);
            mRocketBt.setLayoutParams(rocketBtLayoutParams);
        }
//        if(mNeedToShake) {
//            rocketShake();
//        }
    }

    private void addBottomAd() {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(getContext());
        try {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            isNativeAddSuccess = adAppHelper.getNative(2, mAdContent, params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.acc_btn:
                if (mNeedToShake) {
                    rocketShake();
                    if (mListener != null) {
                        mListener.onAccelerateImmediately();
                    }
                } else {
                    Toast.makeText(getContext(), R.string.updating_please_try_it_later, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void rocketShake() {
        View rootView = getView();
        if (rootView != null) {
            mNeedToShake = false;
            View rocketIV = rootView.findViewById(R.id.rocket_iv);
            ObjectAnimator objectAnimatorX = ObjectAnimator.ofFloat(rocketIV, "translationX", -5, 5);
            ObjectAnimator objectAnimatorY = ObjectAnimator.ofFloat(rocketIV, "translationY", -5, 5);
            objectAnimatorX.setRepeatCount(-1);
            objectAnimatorX.setRepeatMode(ValueAnimator.REVERSE);
            objectAnimatorY.setRepeatCount(-1);
            objectAnimatorY.setRepeatMode(ValueAnimator.REVERSE);
            mRocketShake = new AnimatorSet();
            mRocketShake.playTogether(objectAnimatorX, objectAnimatorY);
            mRocketShake.setDuration(100);
            mRocketShake.setInterpolator(new LinearInterpolator());
            mRocketShake.start();
        } else {
            mNeedToShake = true;
        }
    }

    @Override
    public void onDestroy() {
        if (mRocketShake != null) {
            mRocketShake.cancel();
        }
        super.onDestroy();
    }

    public void rocketFly() {
        mNeedToShake = true;
        if (mRocketShake != null) {
            mRocketShake.cancel();
        }
        View rootView = getView();
        if (rootView != null) {
            View rocketIV = rootView.findViewById(R.id.rocket_iv);
            ObjectAnimator objectAnimatorY = ObjectAnimator.ofFloat(rocketIV, "translationY", 0, -getResources().getDisplayMetrics().heightPixels);
            objectAnimatorY.setDuration(500);
            objectAnimatorY.setInterpolator(new AccelerateInterpolator());
            objectAnimatorY.addListener(this);
            objectAnimatorY.start();
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mListener != null) {
            mListener.onAnimationFinish();
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    public interface NetworkAccelerationFragmentListener {
        void onAccelerateImmediately();

        void onAnimationFinish();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NetworkAccelerationFragmentListener) {
            mListener = (NetworkAccelerationFragmentListener) context;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof NetworkAccelerationFragmentListener) {
            mListener = (NetworkAccelerationFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void stopShake() {
        if (mRocketShake != null) {
            mRocketShake.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRocketShake != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mRocketShake.resume();
            }
        }

        if (!RuntimeSettings.isVIP())
            addBottomAd();
        else
            isNativeAddSuccess = false;
        if (!isNativeAddSuccess) {
            RelativeLayout.LayoutParams rocketBtLayoutParams = (RelativeLayout.LayoutParams) mRocketBt.getLayoutParams();
            rocketBtLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.banner_ad_loading_icon_margin_top);
            mRocketBt.setLayoutParams(rocketBtLayoutParams);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRocketShake != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mRocketShake.pause();
            }
        }
    }
}
