package com.androapplite.shadowsocks.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.androapplite.shadowsocks.R;

public class NewUserGuideActivity extends BaseShadowsocksActivity {
    private ViewPager mWizardPager;
    private NewUserGuidePagerAdapter mNewUserGuidePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_guide);
        mWizardPager = (ViewPager)findViewById(R.id.wizard_pager);
        mNewUserGuidePagerAdapter = new NewUserGuidePagerAdapter(getSupportFragmentManager());
        initWizardPager();

    }
    private void initWizardPager(){
        mWizardPager.setAdapter(mNewUserGuidePagerAdapter);
    }

    private static class NewUserGuidePagerAdapter extends FragmentPagerAdapter{
        public NewUserGuidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return null;
        }

        @Override
        public int getCount() {
            return 0;
        }
    }
}
