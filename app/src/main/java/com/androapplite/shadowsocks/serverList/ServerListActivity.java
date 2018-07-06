package com.androapplite.shadowsocks.serverList;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.androapplite.shadowsocks.activity.BaseShadowsocksActivity;
import com.androapplite.vpn3.R;
import com.ironsource.sdk.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class ServerListActivity extends BaseShadowsocksActivity implements View.OnClickListener {
    private static final String TAG = "ServerListActivity";

    private static final int PAGE_COUNT = 2;

    private ViewPager mContentViewPager;
    private LinearLayout mServerListFreeLinear;
    private LinearLayout mServerListVipLinear;
    private FrameLayout mTabLineView;

    private Fragment mFreeServerFragment;
    private Fragment mVIPServerFragment;

    private List<Fragment> mFragmentList;
    private List<LinearLayout> mTabLinearViewList;

    private int screenWidth;
    private int screenHeight;
    private int mCurrentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        actionBar.setHomeAsUpIndicator(upArrow);
        actionBar.setTitle(getResources().getString(R.string.app_name).toUpperCase());
        actionBar.setElevation(0);

        initView();
        initData();

        FragmentPagerAdapter mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public Fragment getItem(int arg0) {
                return mFragmentList.get(arg0);
            }
        };

        mContentViewPager.setAdapter(mAdapter);
        mContentViewPager.setCurrentItem(0);
        mContentViewPager.setOffscreenPageLimit(PAGE_COUNT - 1);
        mContentViewPager.setOnPageChangeListener(mOnPageChangeListener);
    }

    private void initView() {
        mServerListFreeLinear = (LinearLayout) findViewById(R.id.server_list_free_linear);
        mServerListVipLinear = (LinearLayout) findViewById(R.id.server_list_vip_linear);
        mTabLineView = (FrameLayout) findViewById(R.id.server_list_tab_line);
        mContentViewPager = (ViewPager) findViewById(R.id.activity_server_view_pager);

        mServerListFreeLinear.setOnClickListener(this);
        mServerListVipLinear.setOnClickListener(this);
    }

    private void initData() {
        mTabLinearViewList = new ArrayList<>();
        mTabLinearViewList.add(mServerListFreeLinear);
        mTabLinearViewList.add(mServerListVipLinear);

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        LinearLayout.LayoutParams tabLineParams = (LinearLayout.LayoutParams) mTabLineView.getLayoutParams();
        tabLineParams.width = screenWidth / PAGE_COUNT;
        mTabLineView.setLayoutParams(tabLineParams);

        mFreeServerFragment = new FreeServerFragment();
        mVIPServerFragment = new VIPServerFragment();

        mFragmentList = new ArrayList<Fragment>();
        mFragmentList.add(mFreeServerFragment);
        mFragmentList.add(mVIPServerFragment);
    }

    private void selectPage(int position, int currentPosition) {
        if (position == currentPosition)
            return;

        refreshTextColor(position);

        int count = Math.abs(position - currentPosition) * 50;
        int startMargin = screenWidth / PAGE_COUNT * currentPosition;
        int endMargin = screenWidth / PAGE_COUNT * position;
        Logger.e("tag", "selectPage " + startMargin + " " + endMargin);
        float offset = (endMargin - startMargin) * 1f / count;
        for (int i = 0; i <= count; i++) {
            LinearLayout.LayoutParams tabLineParams = (LinearLayout.LayoutParams) mTabLineView.getLayoutParams();
            tabLineParams.leftMargin = (int) (startMargin + offset * i);
            mTabLineView.setLayoutParams(tabLineParams);
        }

        mContentViewPager.setCurrentItem(position);
    }

    private void refreshTextColor(int position) {
//        for (int i = 0; i < mTabTextViewList.size(); i++) {
//            if (position == i) {
//                mTabTextViewList.get(i).setTextColor(getResources().getColor(R.color.app_mamage_tab_text_normal));
//            } else {
//                mTabTextViewList.get(i).setTextColor(getResources().getColor(R.color.app_mamage_tab_text_unselected));
//            }
//        }
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            refreshTextColor(position);
            mCurrentIndex = position;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            double tabWidth = screenWidth * 1.0 / PAGE_COUNT;
            if (mCurrentIndex == position) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTabLineView
                        .getLayoutParams();
                if (Build.VERSION.SDK_INT >= 17)
                    lp.setMarginStart((int) (positionOffset * tabWidth + mCurrentIndex * tabWidth));
                else
                    lp.leftMargin = (int) (positionOffset * tabWidth + mCurrentIndex * tabWidth);
                mTabLineView.setLayoutParams(lp);
            } else if (mCurrentIndex - position == 1) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mTabLineView
                        .getLayoutParams();
                if (Build.VERSION.SDK_INT >= 17)
                    lp.setMarginStart((int) ((positionOffset - 1) * tabWidth + mCurrentIndex * tabWidth));
                else
                    lp.leftMargin = (int) ((positionOffset - 1) * tabWidth + mCurrentIndex * tabWidth);
                mTabLineView.setLayoutParams(lp);
            }
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.menu_repair) {
            Fragment fragment = mFragmentList.get(mContentViewPager.getCurrentItem());
            if (fragment instanceof FreeServerFragment)
                ((FreeServerFragment) fragment).disconnectToRefresh("免费菜单");
            else if (fragment instanceof VIPServerFragment)
                ((VIPServerFragment) fragment).disconnectToRefresh("vip菜单");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_server_list, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.server_list_free_linear:
                selectPage(0, mContentViewPager.getCurrentItem());
                break;
            case R.id.server_list_vip_linear:
                selectPage(1, mContentViewPager.getCurrentItem());
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFreeServerFragment != null) {
            mFreeServerFragment = null;
        }
        if (mVIPServerFragment != null) {
            mVIPServerFragment = null;
        }
    }
}
