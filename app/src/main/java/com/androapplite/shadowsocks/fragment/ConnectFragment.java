package com.androapplite.shadowsocks.fragment;


import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;
import com.vm.shadowsocks.core.LocalVpnService;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
    private OnConnectActionListener mListener;
    private TextView mMessageTextView;

    private Button mConnectButton;
    private SharedPreferences mSharedPreference;
    private ImageView mLoadingView;
    private ImageView mConnectStatus;
    private TextView mFreeUsedTimeTextView;
    private boolean mIsAnimating;
    private MyReceiver mMyReceiver;
    private VpnState mVpnState;
    private int[] mMessageTextStrings = {R.string.building_tls, R.string.waiting_server_reply, R.string.requesting_connecting_configure
            , R.string.verifying, R.string.taking_a_coffee_break, R.string.building_configuration};

    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
        mConnectButton = (Button)view.findViewById(R.id.connect_button);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(getContext());
        mLoadingView = (ImageView)view.findViewById(R.id.loading);
        mConnectStatus = (ImageView)view.findViewById(R.id.connect_status);
        mFreeUsedTimeTextView = (TextView)view.findViewById(R.id.free_used_time);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConnectButton.setOnClickListener(this);
        updateFreeUsedTime();
        if (LocalVpnService.IsRunning) {
            mVpnState = VpnState.Connected;
            connectFinish();
        } else {
            mVpnState = VpnState.Init;
            stopFinish();
        }

        IntentFilter intentFilter = new IntentFilter(Action.ACTION_TIME_USE);
        mMyReceiver = new MyReceiver(this);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mMyReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        clearAnimation();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            switch (v.getId()){
                case R.id.connect_button:
                    mListener.onConnectButtonClick();
                    break;
            }
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
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMyReceiver);
        super.onDetach();
    }

    private void init(){
        updateFreeUsedTime();
    }

    private void updateFreeUsedTime() {
        final long countDown = mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
        final String elapsedTime = DateUtils.formatElapsedTime(countDown);
        String freeUseTime = String.format(getString(R.string.free_used_time), elapsedTime);
        SpannableString spannableString = new SpannableString(freeUseTime);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.primary_white_text)),
                freeUseTime.length() - elapsedTime.length(), freeUseTime.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mFreeUsedTimeTextView.setText(spannableString);
    }

    public void animateConnecting(){
        startAnimation();
//        mConnectButton.setText(R.string.connecting);
        mConnectStatus.setImageResource(R.drawable.connect_normal_icon);
        mMessageTextView.setVisibility(View.VISIBLE);
//        mMessageTextView.setText(R.string.connecting);
        mFreeUsedTimeTextView.setVisibility(View.GONE);
        mLoadingView.setImageLevel(0);
        setConnectingText();
    }

    private int count = -1;

    private void setConnectingText() {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, mMessageTextStrings.length - 1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int currentValue = (int) animation.getAnimatedValue();
                if (currentValue != count) {
                    count = currentValue;
                    mMessageTextView.setText(mMessageTextStrings[count]);
                }

            }
        });
        valueAnimator.setDuration(mMessageTextStrings.length * 400).start();
    }

    private void startAnimation(){
        if(!mIsAnimating) {
            Animation currentAnimation = mLoadingView.getAnimation();
            if(currentAnimation == null) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
                mLoadingView.startAnimation(animation);
            }
            mIsAnimating = true;
        }
    }

    private void stopAnimation(){
        clearAnimation();
    }

    private void clearAnimation(){
        mIsAnimating = false;
        mLoadingView.clearAnimation();
    }

    public void animateStopping(){
        startAnimation();
//        mConnectButton.setText(R.string.connect);
        mConnectStatus.setImageResource(R.drawable.connect_normal_icon);
        mMessageTextView.setVisibility(View.VISIBLE);
        mMessageTextView.setText(R.string.stopping);
        mFreeUsedTimeTextView.setVisibility(View.GONE);
        mLoadingView.setImageLevel(0);
    }

    private void connectFinish(){
        stopAnimation();
//        mConnectButton.setText(R.string.disconnect);
        mConnectStatus.setImageResource(R.drawable.connect_success_icon);
        mMessageTextView.setVisibility(View.GONE);
        mFreeUsedTimeTextView.setVisibility(View.VISIBLE);
    }

    private void stopFinish(){
        stopAnimation();
//        mConnectButton.setText(R.string.connect);
        mConnectStatus.setImageResource(R.drawable.connect_normal_icon);
        mMessageTextView.setVisibility(View.GONE);
        mFreeUsedTimeTextView.setVisibility(View.VISIBLE);
        updateFreeUsedTime();
    }

    private void error(){
        stopAnimation();
        mFreeUsedTimeTextView.setVisibility(View.GONE);
        mMessageTextView.setVisibility(View.VISIBLE);
        final long countDown = mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
        mMessageTextView.setText(DateUtils.formatElapsedTime(countDown));
        mLoadingView.setImageLevel(1);
//        mConnectButton.setText(R.string.connect);
        mConnectStatus.setImageResource(R.drawable.connect_normal_icon);
    }

    @Override
    public void onResume() {
        super.onResume();
        int state = mSharedPreference.getInt(SharedPreferenceKey.VPN_STATE, VpnState.Init.ordinal());
        mVpnState = VpnState.values()[state];
        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void updateUI(){
        if(isVisible()) {
            switch (mVpnState) {
                case Init:
                    init();
                    break;
                case Connecting:
                    animateConnecting();
                    break;
                case Connected:
                    connectFinish();
                    break;
                case Stopping:
                    animateStopping();
                    break;
                case Stopped:
                    stopFinish();
                    break;
                case Error:
                    error();
                    break;
            }
        }
    }

    private static class MyReceiver extends BroadcastReceiver {
        private WeakReference<ConnectFragment> mReference;

        MyReceiver(ConnectFragment fragment) {
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectFragment fragment = mReference.get();
            if (fragment != null && fragment.isVisible()) {
                fragment.updateFreeUsedTime();
            }
        }
    }
    public void setConnectResult(VpnState state) {
        mVpnState = state;
        if (isVisible()) {
            updateUI();
        }
    }

}
