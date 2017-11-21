package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.Rotate3dAnimation;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.vm.shadowsocks.core.LocalVpnService;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
    private OnConnectActionListener mListener;
    private TextView mMessageTextView;

    private Button mConnectButton;
    private TextView mSuccessConnectTextView;
    private TextView mFailedConnectTextView;
    private SharedPreferences mSharedPreference;
    private ImageView mBigCircleImageView;
    private ImageView mMiddleCircleImageView;
    private boolean mIsAnimating;
    private MyReceiver mMyReceiver;
    private VpnState mVpnState;


    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_conn, container, false);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
        mConnectButton = (Button)view.findViewById(R.id.connect_button);
        mSuccessConnectTextView = (TextView)view.findViewById(R.id.success_connect);
        mFailedConnectTextView = (TextView)view.findViewById(R.id.failed_connect);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(getContext());
        mBigCircleImageView = (ImageView)view.findViewById(R.id.circle_big_image_view);
        mMiddleCircleImageView = (ImageView)view.findViewById(R.id.circle_mid_image_view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConnectButton.setOnClickListener(this);
        updateFreeUsedTime();
        updateSuccessTimes();
        updateFailedTimes();
        if (LocalVpnService.IsRunning) {
            mVpnState = VpnState.Connected;
            connectFinish();
        } else {
            mVpnState = VpnState.Init;
            stopFinish();
        }

        addBottomAd();

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
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.connect_green)),
                freeUseTime.length() - elapsedTime.length(), freeUseTime.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mMessageTextView.setText(spannableString);
        mConnectButton.setBackgroundResource(R.drawable.connect_btn);

    }

    public void animateConnecting(){
        startAnimation();
        mConnectButton.setText(R.string.connecting);
        mConnectButton.setBackgroundResource(R.drawable.connect_btn);

    }

    private void startAnimation(){
        if(!mIsAnimating) {
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.zoom);
            animation.setDuration(500);
            mBigCircleImageView.startAnimation(animation);
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.zoom);
            animation.setDuration(450);
            mMiddleCircleImageView.startAnimation(animation);
            mIsAnimating = true;
        }
    }

    private void stopAnimation(){
        clearAnimation();
    }

    private void clearAnimation(){
        mIsAnimating = false;
        mBigCircleImageView.clearAnimation();
        mMiddleCircleImageView.clearAnimation();
    }

    public void animateStopping(){
        startAnimation();
        mConnectButton.setText(R.string.stopping);
    }

    private void connectFinish(){
        stopAnimation();
        mConnectButton.setBackgroundResource(R.drawable.connect_btn);
        mConnectButton.setText(R.string.disconnect);
    }

    private void stopFinish(){
        stopAnimation();
        mConnectButton.setText(R.string.touch_to_connect);
    }

    private void error(){
        stopAnimation();
        updateFailedTimes();
        mConnectButton.setBackgroundResource(R.drawable.connect_error);
    }

    @Override
    public void onResume() {
        super.onResume();
        int state = mSharedPreference.getInt(SharedPreferenceKey.VPN_STATE, VpnState.Init.ordinal());
        mVpnState = VpnState.values()[state];
        updateUI();
        addBottomAd();
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
            updateSuccessTimes();
            updateFailedTimes();
        }
    }

    private void updateSuccessTimes() {
        long success = mSharedPreference.getLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, 0);
        String successTimeString = String.valueOf(success);
        String successString = getString(R.string.success_connect, success);
        SpannableString spannableString = new SpannableString(successString);
        int start = successString.indexOf(successTimeString);
        int end = start + successTimeString.length();
        if(start >= 0 && end < successString.length()) {
            spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.connect_green)),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mSuccessConnectTextView.setText(spannableString);

    }

    private void updateFailedTimes(){
        long failed = mSharedPreference.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0);
        String failedTimeString = String.valueOf(failed);
        String failedString = getString(R.string.failed_connect, failed);
        SpannableString spannableString = new SpannableString(failedString);
        int start = failedString.indexOf(failedTimeString);
        int end = start + failedTimeString.length();
        if(start >= 0 && end < failedString.length()) {
            spannableString.setSpan(new ForegroundColorSpan(Color.GRAY),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mFailedConnectTextView.setText(spannableString);

    }

    public void addBottomAd() {
        final View rootView = getView();
        if(getView() != null) {
            FrameLayout container = (FrameLayout) rootView.findViewById(R.id.ad_view_container);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
            try {
                AdAppHelper adAppHelper = AdAppHelper.getInstance(getContext());
                container.addView(adAppHelper.getNative(), params);
                Firebase.getInstance(rootView.getContext()).logEvent("NATIVE广告", "显示成功", "首页底部");
            } catch (Exception ex) {
                ShadowsocksApplication.handleException(ex);
                Firebase.getInstance(rootView.getContext()).logEvent("NATIVE广告", "显示失败", "首页底部");
            }
        }
    }

    public void rotateAd(){
        final View rootView = getView();
        if(rootView != null) {
            FrameLayout view = (FrameLayout) rootView.findViewById(R.id.ad_view_container);
            float centerX = view.getWidth() / 2.0f;
            float centerY = view.getHeight() / 2.0f;
            Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(getContext(), 0, 360, centerX, centerY, 0f, false, true);
            rotate3dAnimation.setDuration(1000);
            rotate3dAnimation.setFillAfter(false);
            view.startAnimation(rotate3dAnimation);
        }
    }

    private static class MyReceiver extends BroadcastReceiver {
        private WeakReference<ConnectFragment> mReference;

        MyReceiver(ConnectFragment fragment) {
            mReference = new WeakReference<ConnectFragment>(fragment);
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
        if (getUserVisibleHint() || isVisible()) {
            updateUI();
        }
    }

}
