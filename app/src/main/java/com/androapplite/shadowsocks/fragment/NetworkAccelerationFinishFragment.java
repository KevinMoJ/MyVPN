package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.androapplite.shadowsocks.utils.Rotate3dAnimation;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.animation.AbstractAnimator;

/**
 * A simple {@link Fragment} subclass.
 */
public class NetworkAccelerationFinishFragment extends Fragment implements AbstractAnimator, Handler.Callback {
    private Handler mHandler;
    private Animation mAnimation;
    private static final int MSG_ROTATE_TICK = 1;
    private boolean mIsInterstitalClose;
    private boolean mIsNativeAdShow;
    private boolean mIsNeedRotateTick;
    private NetworkAccelerationFinishFragmentListener mListener;
    private ImageView mVIPRecommendImage;

    public NetworkAccelerationFinishFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_network_acceleration_finish, container, false);
        mVIPRecommendImage =  view.findViewById(R.id.net_speed_recommend_image);
        mVIPRecommendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.onVIPImageClick();
            }
        });
        mVIPRecommendImage.setVisibility(RuntimeSettings.isVIP() ? View.VISIBLE : View.GONE);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mVIPRecommendImage.setVisibility(!RuntimeSettings.isVIP() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NetworkAccelerationFinishFragmentListener) {
            mListener = (NetworkAccelerationFinishFragmentListener) context;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof NetworkAccelerationFinishFragmentListener) {
            mListener = (NetworkAccelerationFinishFragmentListener) activity;
        }
    }

    public interface NetworkAccelerationFinishFragmentListener {
        void onVIPImageClick();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (!RuntimeSettings.isVIP()) {
            FrameLayout container = (FrameLayout) view.findViewById(R.id.ad_view_fl);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
            Context context = view.getContext();
            if (container != null) {
                AdAppHelper.getInstance(context).getNative(2, container, params);
            }
        }
    }

    @Override
    public void run(View view) {
        mIsNativeAdShow = true;
        if (mIsInterstitalClose) {
            Context context = view.getContext();
            if (context != null) {
                mAnimation = AnimationUtils.loadAnimation(context, R.anim.bottom_up);
                view.startAnimation(mAnimation);

                mHandler = new Handler(this);
                mHandler.sendEmptyMessageDelayed(MSG_ROTATE_TICK, 2000);
            }
        }
    }

    public void animate() {
        mIsInterstitalClose = true;
        if (mIsNativeAdShow) {
            View view = getView();
            if (view != null) {
                Context context = view.getContext();
                if (context != null) {
                    mAnimation = AnimationUtils.loadAnimation(context, R.anim.bottom_up);
                    FrameLayout container = (FrameLayout)view.findViewById(R.id.ad_view_fl);
                    container.startAnimation(mAnimation);

                    mHandler = new Handler(this);
                    mHandler.sendEmptyMessageDelayed(MSG_ROTATE_TICK, 2000);
                }
            }
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ROTATE_TICK:
                View view = getView();
                if (view != null) {
                    View tickV = view.findViewById(R.id.tick_iv);
                    rotatedBottomAd(tickV);
                }
                break;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mAnimation != null) {
            mAnimation.cancel();
        }
        View view = getView();
        if (view != null) {
            View tickV = view.findViewById(R.id.tick_iv);
            tickV.clearAnimation();
        }
        super.onDestroy();
    }

    private void rotatedBottomAd(View view){
        if (view != null) {
            Context context = view.getContext();
            if (context != null) {
                mIsNeedRotateTick = false;
                rotateTick(view);
            } else {
                mIsNeedRotateTick = true;
            }
        } else {
            mIsNeedRotateTick = true;
        }
    }

    private void rotateTick(View view) {
        Context context = view.getContext();
        if (context != null) {
            float centerX = view.getWidth() / 2.0f;
            float centerY = view.getHeight() / 2.0f;
            Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(context, 0, 360, centerX, centerY, 0f, false, true);
            rotate3dAnimation.setDuration(1000);
            rotate3dAnimation.setFillAfter(false);
            view.startAnimation(rotate3dAnimation);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsNeedRotateTick) {
            mIsNeedRotateTick = false;
            View view = getView();
            if (view != null) {
                View tickV = view.findViewById(R.id.tick_iv);
                rotateTick(tickV);
            }
        }
    }
}
