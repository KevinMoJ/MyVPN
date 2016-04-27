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
//    private static final int[][] mNewUserGuideResourceIds = {
//            {R.drawable.new_user_guide_1, R.string.forever_free, R.string.forever_free_explain},
//            {R.drawable.new_user_guide_2, R.string.browse_without_borders, R.string.browse_without_borders_explain},
//            {R.drawable.new_user_guide_3, R.string.protect_your_identify, R.string.protect_your_identity_explain}
//    };
    private TypedArray[] mNewUserGuideResourceIds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_guide);
        mWizardPager = (ViewPager)findViewById(R.id.wizard_pager);
        mNewUserGuideResourceIds = createNewUserGuideResourceIds();
        mNewUserGuidePagerAdapter = new NewUserGuidePagerAdapter(getSupportFragmentManager(), mNewUserGuideResourceIds);
        initWizardPager();

    }

    private TypedArray[] createNewUserGuideResourceIds(){
        final Resources resources = getResources();
        TypedArray rows = resources.obtainTypedArray(R.array.new_user_guide_resources);
        int len = rows.length();
        TypedArray[] resourceIds = new TypedArray[len];
        for(int i=0; i < len; i++){
            resourceIds[i] = resources.obtainTypedArray(rows.getResourceId(i, 0));
        }
        return resourceIds;
//        return new int[][]{
//          new int[]{},
//                new int[]{},
//                new int[]{},
//        };
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
