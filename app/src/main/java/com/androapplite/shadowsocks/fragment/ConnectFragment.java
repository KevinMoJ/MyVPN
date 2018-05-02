package com.androapplite.shadowsocks.fragment;


import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.view.VPN3AdDialog;
import com.vm.shadowsocks.core.LocalVpnService;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
    private OnConnectActionListener mListener;
    private TextView mMessageTextView;

    private ImageButton mConnectButton;
    private SharedPreferences mSharedPreference;
    private boolean mIsAnimating;
    private MyReceiver mMyReceiver;
    private VpnState mVpnState;
    private ImageView mJaguarImageView;
    private ImageView mJaguarAnimationImageView;
    private ProgressBar mProgressBar;
    private TextView mElapseTextView;


    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
        mConnectButton = (ImageButton)view.findViewById(R.id.connect_button);
        mJaguarImageView = (ImageView)view.findViewById(R.id.jaguar_image_view);
        mJaguarAnimationImageView = (ImageView)view.findViewById(R.id.jaguar_animation_image_view);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_bar);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
        mElapseTextView = (TextView)view.findViewById(R.id.elapse);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(getContext());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConnectButton.setOnClickListener(this);
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

        VPN3AdDialog dialog = new VPN3AdDialog();
        dialog.show(getFragmentManager(), "ConnectFragment");
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
    }

    public void animateConnecting(){
        startAnimation();
        mMessageTextView.setText(R.string.connecting);
    }

    private void startAnimation(){
        if(!mIsAnimating) {
            mJaguarImageView.setVisibility(View.INVISIBLE);
            mJaguarAnimationImageView.setVisibility(View.VISIBLE);
            AnimationDrawable animationDrawable = (AnimationDrawable)mJaguarAnimationImageView.getDrawable();
            animationDrawable.start();
            mConnectButton.setVisibility(View.INVISIBLE);

            mProgressBar.setVisibility(View.VISIBLE);
            int max = 60000;
            mProgressBar.setMax(max);
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(mProgressBar, "progress", 0, mProgressBar.getMax());
            progressAnimator.setDuration(max);
            progressAnimator.start();
            mProgressBar.setTag(progressAnimator);
            mMessageTextView.setText(R.string.connecting);
            mElapseTextView.setVisibility(View.INVISIBLE);
            mIsAnimating = true;
        }
    }

    private void stopAnimation(){
        clearAnimation();
    }

    private void clearAnimation(){
        mIsAnimating = false;
        AnimationDrawable animationDrawable = (AnimationDrawable)mJaguarAnimationImageView.getDrawable();
        animationDrawable.stop();
        ObjectAnimator progressAnimator = (ObjectAnimator)mProgressBar.getTag();
        if (progressAnimator != null) {
            progressAnimator.end();
        }
    }

    public void animateStopping(){
        startAnimation();
        mMessageTextView.setText(R.string.stopping);
    }

    private void connectFinish(){
        stopAnimation();
        mJaguarImageView.setImageLevel(1);
        mJaguarImageView.requestLayout();
        mJaguarImageView.setVisibility(View.VISIBLE);
        mConnectButton.setImageLevel(1);
        mConnectButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mMessageTextView.setText(R.string.connected);
        mElapseTextView.setVisibility(View.VISIBLE);
        mJaguarAnimationImageView.setVisibility(View.INVISIBLE);
    }

    private void stopFinish(){
        stopAnimation();
        mJaguarImageView.setImageLevel(0);
        mJaguarImageView.requestLayout();
        mJaguarImageView.setVisibility(View.VISIBLE);
        mConnectButton.setImageLevel(0);
        mMessageTextView.setText(R.string.tap_to_connect);
        mElapseTextView.setVisibility(View.INVISIBLE);
        mConnectButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mJaguarAnimationImageView.setVisibility(View.INVISIBLE);
    }

    private void error(){
        stopAnimation();
        mJaguarImageView.setImageLevel(0);
        mJaguarImageView.requestLayout();
        mJaguarImageView.setVisibility(View.VISIBLE);
        mConnectButton.setImageLevel(0);
        mMessageTextView.setText(R.string.retry);
        mElapseTextView.setVisibility(View.INVISIBLE);
        mConnectButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mJaguarAnimationImageView.setVisibility(View.INVISIBLE);
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
                fragment.updateFreeUseTime();
            }
        }
    }
    public void setConnectResult(VpnState state) {
        mVpnState = state;
        if (getUserVisibleHint() || isVisible()) {
            updateUI();
        }
    }

    private void updateFreeUseTime() {
        final long countDown = mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
        final String elapsedTime = DateUtils.formatElapsedTime(countDown);
        String freeUseTime = String.format(getString(R.string.free_used_time), elapsedTime);
        mElapseTextView.setText(freeUseTime);
    }
}
