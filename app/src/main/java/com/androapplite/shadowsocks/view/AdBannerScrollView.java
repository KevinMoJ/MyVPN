package com.androapplite.shadowsocks.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.androapplite.shadowsocks.R;


/**
 * Created by jim on 17/3/22.
 */

public class AdBannerScrollView extends ScrollView {
    public AdBannerScrollView(Context context) {
        super(context);
    }

    public AdBannerScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int oriHeight = MeasureSpec.getSize(heightMeasureSpec);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.ad_banner_height);
        int minHeight = Math.min(oriHeight, maxHeight);
        int minHeightMeasureSpec = MeasureSpec.makeMeasureSpec(minHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, minHeightMeasureSpec);
    }
}
