package com.androapplite.shadowsocks.activity;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.fragment.NewUserGuideFragment;

public class NewUserGuideActivity extends BaseShadowsocksActivity {
    private ViewPager mWizardPager;
    private NewUserGuidePagerAdapter mNewUserGuidePagerAdapter;
    private int[][] mNewUserGuideResourceIds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_guide);
        mWizardPager = (ViewPager)findViewById(R.id.wizard_pager);
        mNewUserGuideResourceIds = createNewUserGuideResourceIds();
        mNewUserGuidePagerAdapter = new NewUserGuidePagerAdapter(getSupportFragmentManager(), mNewUserGuideResourceIds);
        initWizardPager();

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
}
