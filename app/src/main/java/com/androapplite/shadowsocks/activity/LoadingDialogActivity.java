package com.androapplite.shadowsocks.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.androapplite.shadowsocks.utils.DensityUtil;
import com.androapplite.vpn3.R;

public class LoadingDialogActivity extends AppCompatActivity {
    private ObjectAnimator alphaAnim1;
    private ObjectAnimator alphaAnim2;
    private Animation animationScan;
    private ObjectAnimator jumpAnim1;
    private ObjectAnimator jumpAnim2;

    private ImageView mIvBg, mIvLogo, mIvScan;
    private static int currentType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_dialog);
        mIvBg = (ImageView) findViewById(R.id.iv_bg);
        mIvLogo = (ImageView) findViewById(R.id.iv_logo);
        mIvScan = (ImageView) findViewById(R.id.iv_scan);
        startSplashAnim();
    }

    //透明动画 1s
    private void startSplashAnim() {
        alphaAnim1 = ObjectAnimator.ofFloat(mIvBg, "alpha", 0, 1).setDuration(1000);
        alphaAnim2 = ObjectAnimator.ofFloat(mIvLogo, "alpha", 0, 1).setDuration(1000);
        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIvBg.setVisibility(View.VISIBLE);
                mIvLogo.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mIvScan != null) {
                    mIvScan.clearAnimation();
                    mIvScan.setVisibility(View.GONE);
                }
                startLogoAnim();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.playTogether(alphaAnim1, alphaAnim2);
        set.start();
    }

    //上下跳的平移动画,水波纹扩散动画 3s
    private void startLogoAnim() {
        animationScan = AnimationUtils.loadAnimation(this, R.anim.anim_tobig);
        float dy = DensityUtil.dip2px(this, 8);
        jumpAnim1 = ObjectAnimator.ofFloat(mIvLogo, "translationY",
                -dy, dy, -dy, dy, -dy, dy, 0);
        jumpAnim2 = ObjectAnimator.ofFloat(mIvBg, "translationY",
                -dy, dy, -dy, dy, -dy, dy, 0);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(3000);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIvScan.startAnimation(animationScan);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIvScan.clearAnimation();
                mIvScan.setVisibility(View.GONE);
                WarnDialogActivity.start(LoadingDialogActivity.this, currentType);
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.playTogether(jumpAnim1, jumpAnim2);
        set.start();
    }

    public static void start(Context context, int type) {
        currentType = type;
        Intent intent = new Intent(context, LoadingDialogActivity.class);
        intent.putExtra(WarnDialogActivity.SHOW_DIALOG_TYPE, type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onBackPressed() {

    }
}
