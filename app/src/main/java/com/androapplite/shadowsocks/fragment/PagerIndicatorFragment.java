package com.androapplite.shadowsocks.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class PagerIndicatorFragment extends Fragment {
    private int mSpotCount;
    private int mCurrent;
    private LinearLayout mRootLinearLayout;

    public PagerIndicatorFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootLinearLayout = (LinearLayout)inflater.inflate(R.layout.fragment_pager_indicator, container, false);
//        inflater.inflate(R.layout.view_pager_indicator_spot, linearLayout, true);
        return mRootLinearLayout;
    }

    public void setSpotCount(int count){
        mSpotCount = count;
        clear();
        if(count > 0){
            addSpot();
        }
    }

    public void setCurrent(int current){
        mCurrent = current;
        if(current < mSpotCount){
            for(int i=0; i<mSpotCount; i++){
                View spotView = mRootLinearLayout.getChildAt(i);
                spotView.setAlpha(i == current ? 1 : (float) 0.5);
            }
        }
    }

    private void clear(){
        if(mRootLinearLayout != null){
            mRootLinearLayout.removeAllViews();
        }
    }

    private void addSpot(){
        if(mRootLinearLayout != null){
            LayoutInflater inflater = getActivity().getLayoutInflater();
            for(int i=0; i<mSpotCount;i++){
                inflater.inflate(R.layout.view_pager_indicator_spot, mRootLinearLayout, true);
            }
        }
    }

}
