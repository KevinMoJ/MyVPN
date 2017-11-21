package com.androapplite.shadowsocks.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.Checkable;

import com.androapplite.vpn3.R;

/**
 * Created by jim on 17/6/14.
 */

public class CheckableFrameLayout extends FrameLayout implements Checkable {
    private  boolean mIsChecked;

    public CheckableFrameLayout(Context context) {
        super(context);
    }

    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setChecked(boolean checked) {
        mIsChecked = checked;
        if(mIsChecked){
            setBackgroundResource(R.color.grey_cc);
        }else{
            setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void toggle() {
        mIsChecked = !mIsChecked;
    }
}
