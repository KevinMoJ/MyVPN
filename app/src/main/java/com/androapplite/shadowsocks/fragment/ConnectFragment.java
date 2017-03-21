package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
    private OnConnectActionListener mListener;
    private TextView mMessageTextView;
    private Button mConnectButton;
    private ImageView mLoadingView;
    private Timer mCountDownTimer;
    private Handler mUpdateStateHandler;
    private Runnable mUpdateStateDelayedRunable;
    private TextView mSuccessConnectTextView;
    private TextView mFailedConnectTextView;
    private SharedPreferences mSharedPreference;

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
        mConnectButton.setOnClickListener(this);
        mLoadingView = (ImageView)view.findViewById(R.id.loading);
        mSuccessConnectTextView = (TextView)view.findViewById(R.id.success_connect);
        mFailedConnectTextView = (TextView)view.findViewById(R.id.failed_connect);

        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(getContext());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        init();
        super.onViewCreated(view, savedInstanceState);
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
        mLoadingView.clearAnimation();
        if(mCountDownTimer != null){
            mCountDownTimer.cancel();
            mCountDownTimer.purge();
            mCountDownTimer = null;
        }

    }

    public void animateConnecting(){
        startAnimation();
        mConnectButton.setText(R.string.disconnect);
        mMessageTextView.setText(R.string.connecting);
        mLoadingView.setImageLevel(0);
    }

    private void startAnimation(){
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        mLoadingView.startAnimation(animation);
    }


    public void animateStopping(){
        startAnimation();
        mConnectButton.setText(R.string.connect);
        mMessageTextView.setText(R.string.stopping);
        mLoadingView.setImageLevel(0);
    }

    public void setConnectResult(final Constants.State state){
        if(isVisible()) {
            switch (state) {
                case INIT:
                    init();
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
        }else if(mUpdateStateHandler == null){
            mUpdateStateHandler = new Handler();
            mUpdateStateDelayedRunable = new UpdateStateDelayedRunable(this, state);
            mUpdateStateHandler.postDelayed(mUpdateStateDelayedRunable, 100);
        }

    }

    private static class UpdateStateDelayedRunable implements Runnable{
        private WeakReference<ConnectFragment> mFragmentReference;
        private Constants.State mState;

        UpdateStateDelayedRunable(ConnectFragment fragment, Constants.State state){
            mFragmentReference = new WeakReference<ConnectFragment>(fragment);
            mState = state;
        }

        @Override
        public void run() {
            ConnectFragment fragment = mFragmentReference.get();
            if(fragment != null){
                fragment.setConnectResult(mState);
                fragment.mUpdateStateHandler.removeCallbacks(fragment.mUpdateStateDelayedRunable);
                fragment.mUpdateStateHandler = null;
                fragment.mUpdateStateDelayedRunable = null;
            }
        }


    }

    private void init(){
        final long countDown = mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
        mMessageTextView.setText(getString(R.string.free_used_time, DateUtils.formatElapsedTime(countDown)));

        long success = mSharedPreference.getLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, 0);
        mSuccessConnectTextView.setText(getString(R.string.success_connect, success));

        long error = mSharedPreference.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0);
        mFailedConnectTextView.setText(getString(R.string.failed_connect, error));
    }

    private void connectFinish(){
        mLoadingView.clearAnimation();
        mCountDownTimer = new Timer();
        mCountDownTimer.schedule(new CountDownTimerTask(this), 0, 1000);
        mConnectButton.setText(R.string.disconnect);
        long success = mSharedPreference.getLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, 0);
        mSuccessConnectTextView.setText(getString(R.string.success_connect, success));
    }

    private static class CountDownTimerTask extends TimerTask{
        private WeakReference<ConnectFragment> mFragmentReference;

        CountDownTimerTask(ConnectFragment fragment){
            mFragmentReference = new WeakReference<ConnectFragment>(fragment);
        }

        @Override
        public void run() {
            ConnectFragment fragment = mFragmentReference.get();
            if(fragment != null){
                fragment.mMessageTextView.post(new UpdateRunnable(fragment));
            }
        }

        private static class UpdateRunnable implements Runnable{
            private WeakReference<ConnectFragment> mFragmentReference;

            UpdateRunnable(ConnectFragment fragment){
                mFragmentReference = new WeakReference<ConnectFragment>(fragment);
            }

            @Override
            public void run() {
                ConnectFragment fragment = mFragmentReference.get();
                if(fragment != null && fragment.isVisible()){
                    final long countDown = fragment.mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
                    fragment.mMessageTextView.setText(fragment.getString(R.string.free_used_time, DateUtils.formatElapsedTime(countDown)));
                }
            }
        }
    }

    private void stopFinish(){
        mLoadingView.clearAnimation();
        final long countDown = mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
        mMessageTextView.setText(getString(R.string.free_used_time, DateUtils.formatElapsedTime(countDown)));
    }

    private void error(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.connect_error_red));
        mLoadingView.setImageLevel(1);

        long error = mSharedPreference.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0);
        mFailedConnectTextView.setText(getString(R.string.failed_connect, error));

    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mUpdateStateHandler != null){
            mUpdateStateHandler.removeCallbacks(mUpdateStateDelayedRunable);
            mUpdateStateHandler = null;
            mUpdateStateDelayedRunable = null;
        }
    }

    public void updateUI(){
        init();
    }
}
