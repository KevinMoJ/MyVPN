package com.androapplite.shadowsocks.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.NewUserGuideFragment;
import com.androapplite.shadowsocks.fragment.PagerIndicatorFragment;

public class NewUserGuideActivity extends BaseShadowsocksActivity {
    private ViewPager mWizardPager;
    private NewUserGuidePagerAdapter mNewUserGuidePagerAdapter;
    private int[][] mNewUserGuideResourceIds;
    private PagerIndicatorFragment mPagerIndicatorFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_guide);
        mWizardPager = (ViewPager)findViewById(R.id.wizard_pager);
        mNewUserGuideResourceIds = createNewUserGuideResourceIds();
        FragmentManager fragmentManager = getSupportFragmentManager();
        mNewUserGuidePagerAdapter = new NewUserGuidePagerAdapter(fragmentManager, mNewUserGuideResourceIds);
        mPagerIndicatorFragment = (PagerIndicatorFragment)fragmentManager.findFragmentById(R.id.pager_indicator);
        initPagerIndicatorFragment();
        initWizardPager();
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
        mWizardPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                if(mPagerIndicatorFragment != null){
                    mPagerIndicatorFragment.setCurrent(position);
                }
            }
        });
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
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
    }
}
