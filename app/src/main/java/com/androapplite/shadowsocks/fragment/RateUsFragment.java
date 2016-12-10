package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RateUsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RateUsFragment extends Fragment implements View.OnClickListener, Animation.AnimationListener{
    private OnFragmentInteractionListener mListener;
    private LinearLayout mStartContainer;
    private @IdRes int mClickedStartButtonId;

    public RateUsFragment() {
        // Required empty public constructor
    }

    public static final RateUsFragment newInstance(){
        return new RateUsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_rate_us, container, false);
        mStartContainer = (LinearLayout)rootView.findViewById(R.id.star_container);
        for(int i = 0; i< mStartContainer.getChildCount(); i++){
            View view = mStartContainer.getChildAt(i);
            if(view instanceof ImageButton){
                view.setOnClickListener(this);
            }
        }
        return rootView;
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
    public void onAttach(Context context) {
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

    public interface OnFragmentInteractionListener {
        void onCloseRateUs(RateUsFragment fragment);
        void onRateUs(RateUsFragment fragment);
    }

    @Override
    public void onClick(View v) {
        int selectedIndex = mStartContainer.indexOfChild(v);
        for(int i = 0; i< mStartContainer.getChildCount(); i++){
            View view = mStartContainer.getChildAt(i);
            if(view instanceof ImageButton){
                if(i <= selectedIndex){
                    view.setSelected(true);
                }else{
                    view.setSelected(false);
                }
            }
        }
        mClickedStartButtonId = v.getId();
        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
                animation.setAnimationListener(RateUsFragment.this);
                getView().startAnimation(animation);
            }
        }, 1000);
//        Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
//        animation.setAnimationListener(RateUsFragment.this);
//        getView().startAnimation(animation);
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if(mListener != null){
            if(mClickedStartButtonId == R.id.rate_us_image_button){
                mListener.onRateUs(this);
            }else{
                mListener.onCloseRateUs(this);
            }
        }

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}
