package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.NewUserGuideFragment;
import com.androapplite.shadowsocks.fragment.PagerIndicatorFragment;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;

import java.util.ArrayList;
import java.util.List;

public class NewUserGuideActivity extends BaseShadowsocksActivity {
    private ViewPager mWizardPager;
    private NewUserGuidePagerAdapter mNewUserGuidePagerAdapter;
    private int[][] mNewUserGuideResourceIds;
    private PagerIndicatorFragment mPagerIndicatorFragment;
    private Button mNewUserGuideFinishButton;
    private List<View> mViews;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_guide);
        mWizardPager = (ViewPager)findViewById(R.id.wizard_pager);
        mNewUserGuideResourceIds = createNewUserGuideResourceIds();
        FragmentManager fragmentManager = getSupportFragmentManager();
        mNewUserGuidePagerAdapter = new NewUserGuidePagerAdapter(fragmentManager, mNewUserGuideResourceIds);
        mPagerIndicatorFragment = (PagerIndicatorFragment)fragmentManager.findFragmentById(R.id.pager_indicator);
        mNewUserGuideFinishButton = (Button)findViewById(R.id.new_user_guide_finish_button);
        initPagerIndicatorFragment();
        initWizardPager();
        initNewUserGuideFinishButton();
        initBackgroundReceiverIntentFilter();
        initBackgroundReceiver();
    }

    private void initPagerIndicatorFragment(){
        mPagerIndicatorFragment.setSpotCount(mNewUserGuideResourceIds.length);
        mPagerIndicatorFragment.setCurrent(0);
    }

    private int[][] createNewUserGuideResourceIds(){
        final Resources resources = getResources();
        TypedArray rows = resources.obtainTypedArray(R.array.new_user_guide_resources);
        int len = rows.length();
        int[][] resourceIds = new int[len][];
        for(int i=0; i < len; i++){
            int resid = rows.getResourceId(i, 0);
            TypedArray row = resources.obtainTypedArray(resid);
            int rowlen= row.length();
            int[] rowResourceIds = new int[len];
            for(int j=0; j<rowlen; j++){
                rowResourceIds[j] = row.getResourceId(j, 0);
            }
            resourceIds[i] = rowResourceIds;
        }
        return resourceIds;
    }


    private void initWizardPager(){
        mWizardPager.setAdapter(mNewUserGuidePagerAdapter);
        mWizardPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mPagerIndicatorFragment != null) {
                    mPagerIndicatorFragment.setCurrent(position);
                }
                if (mNewUserGuideFinishButton != null) {
                    mNewUserGuideFinishButton.setVisibility(isLastWizardStep(position) ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private boolean isLastWizardStep(int current){
        return current == mNewUserGuideResourceIds.length - 1;
    }

    private void initNewUserGuideFinishButton(){
        mNewUserGuideFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = NewUserGuideActivity.this;
                initSharedPreferenceValue(activity);
                startActivity(new Intent(activity, ConnectivityActivity.class));
            }
        });
    }

    private void initSharedPreferenceValue(@NonNull Context context){
        DefaultSharedPrefeencesUtil.markAsOldUser(context);
    }

    private static class NewUserGuidePagerAdapter extends FragmentPagerAdapter{
        private int[][] mResourceIds;
        public NewUserGuidePagerAdapter(FragmentManager fm, int[][] resourceIds) {
            super(fm);
            mResourceIds = resourceIds;
        }

        @Override
        public Fragment getItem(int position) {
            return NewUserGuideFragment.newInstance(mResourceIds[position]);
        }

        @Override
        public int getCount() {
            return mResourceIds.length;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.NEW_USER_GUIDE_ACTIVITY_SHOW));
    }

    private void initBackgroundReceiver(){
        mBackgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(action.equals(Action.CONNECTION_ACTIVITY_SHOW)){
                    finish();
                }
            }
        };
    }

    private void initBackgroundReceiverIntentFilter(){
        mBackgroundReceiverIntentFilter = new IntentFilter();
        mBackgroundReceiverIntentFilter.addAction(Action.CONNECTION_ACTIVITY_SHOW);
    }
}
