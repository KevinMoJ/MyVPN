package com.androapplite.shadowsocks.fragment;


import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectivityFragment extends Fragment {


    public ConnectivityFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_connectivity, container, false);
        final ProgressBar progressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.connecting);
        set.setTarget(progressBar);
        set.getChildAnimations().get(0).addListener(new AnimatorListenerAdapter() {
            private int mCount = 0;

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d("end", "");
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                if(mCount == 0){
                    progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.connecting_phase_2));
                    progressBar.setBackgroundResource(R.drawable.connecting_phase_1_end);
                }else if(mCount == 1){
                    progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.connecting_phase_3));
                    progressBar.setBackgroundResource(R.drawable.connecting_phase_2_end);
                }
                mCount++;
            }
        });
//        set.addListener(new AnimatorListenerAdapter() {
//            private int mCount = 0;
//
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                Log.d("end", mCount + "");
//            }
//
//            @Override
//            public void onAnimationRepeat(Animator animation) {
//                Log.d("repeat", mCount + "");
//            }
//        });
        set.start();

//        Animator animator = AnimatorInflater.loadAnimator(getActivity(), R.animator.connecting);
//        animator.setTarget(progressBar);
//        animator.start();
//        animator.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationRepeat(Animator animation) {
//                Log.d("repeat", "-");
//            }
//
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                Log.d("end", "-");
//            }
//        });
        return rootView;
    }

}
