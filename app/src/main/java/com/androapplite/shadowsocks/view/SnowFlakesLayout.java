package com.androapplite.shadowsocks.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.androapplite.vpn3.R;

import java.util.Random;


/**
 * Created by User on 2016/12/6.音符飘动View
 */
public class SnowFlakesLayout extends RelativeLayout {
    public static final int LEFT_BOTTOM = 0;
    public static final int RIGHT_BOTTOM = 1;
    Context context;
    float height;
    float width;
    int animateDuration = 10000;
    int wholeAnimateTiming = 300000;
    int generateSnowTiming = 1000;
    int imageResourceID;
    int snowMaxSize = 40;
    int snowMinSize = 1;
    boolean enableRandomCurving = false;
    boolean enableAlphaFade = false;
    int mDirection = LEFT_BOTTOM;
    //雪片状Y初始化位置
    final int snowFlakeYInitializePosition = 0;
    CountDownTimer mainCountdownSnowTimer;
    Random generator = new Random();
    Handler mHandler = new Handler();

    private FlakesEndListener mFlakesEndListener;

    public boolean isPaying = false;

    public void setFlakesEndListener(FlakesEndListener flakesEndListener) {
        mFlakesEndListener = flakesEndListener;
    }

    public SnowFlakesLayout(Context context) {
        super(context);
        this.context = context;
    }

    public SnowFlakesLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public SnowFlakesLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SnowFlakesLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    public void setEnableAlphaFade(boolean enableAlphaFade) {
        this.enableAlphaFade = enableAlphaFade;
    }

    public void setImageResourceID(int imageResourceID) {
        this.imageResourceID = imageResourceID;
    }

    public void setWholeAnimateTiming(int wholeAnimateTiming) {
        this.wholeAnimateTiming = wholeAnimateTiming;
    }

    public void setAnimateDuration(int animateDuration) {
        this.animateDuration = animateDuration;
    }

    public void setGenerateSnowTiming(int generateSnowTiming) {
        this.generateSnowTiming = generateSnowTiming;
    }

    public void setEnableRandomCurving(boolean enableRandomCurving) {
        this.enableRandomCurving = enableRandomCurving;
    }

    public void init() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        height = displaymetrics.heightPixels;
        width = displaymetrics.widthPixels;
//        imageResourceID = R.drawable.snow_flakes_pic;
    }

    private void showSnow(int direction) {
        final ImageView snowAnimationView = new ImageView(context);
        snowAnimationView.setClickable(false);
        int n = generator.nextInt(10);
        switch (n) {
            case 0:
            case 1:
            case 2:
            case 3:
                imageResourceID = R.drawable.icon_music_small;
                break;
            case 4:
            case 5:
            case 6:
                imageResourceID = R.drawable.icon_music_middle;
                break;
            case 7:
            case 8:
            case 9:
                imageResourceID = R.drawable.icon_music_big;
                break;
            default:
                break;
        }
        snowAnimationView.setImageResource(imageResourceID);
        LayoutParams snowParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        if (height == 0) {
            height = 720;
        }
        int i2 = generator.nextInt((int) height * 1 / 3) + 80;
        snowParam.setMargins(0, -i2, 0, 0);
        AnimationSet animationSet = new AnimationSet(false);
        this.addView(snowAnimationView, snowParam);
        TranslateAnimation scale;
        if (direction == LEFT_BOTTOM) {
            scale = new TranslateAnimation(0, width, height * 2 / 3, snowFlakeYInitializePosition);
        } else {
            scale = new TranslateAnimation(width, 0, height * 2 / 3, snowFlakeYInitializePosition);
        }
        scale.setDuration(animateDuration);
        animationSet.addAnimation(scale);

        if (enableRandomCurving) {
            int i3 = generator.nextInt(180) - 90;
            RotateAnimation r = new RotateAnimation(0f, i3);
            r.setStartOffset(animateDuration / 10);
            r.setDuration(animateDuration);
            animationSet.addAnimation(r);
        }


        if (enableAlphaFade) {
            AlphaAnimation animation = new AlphaAnimation(1.0f, 0.3f);
            animation.setDuration(animateDuration);
            animationSet.addAnimation(animation);
        }
        CountDownTimer timer = new CountDownTimer(animateDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                SnowFlakesLayout.this.removeView(snowAnimationView);
            }
        }.start();
        snowAnimationView.setTag(R.id.tag_countdown_timer, timer);
        snowAnimationView.setAnimation(animationSet);
        animationSet.startNow();
    }

    public void startSnowing(final int direction) {
        mainCountdownSnowTimer = new CountDownTimer(wholeAnimateTiming, generateSnowTiming) {

            @Override
            public void onTick(long millisUntilFinished) {
                showSnow(direction);
            }

            @Override
            public void onFinish() {
                stopSnowing();
                isPaying = false;

            }
        }.start();
        isPaying = true;
    }

    public void stopSnowing() {
        isPaying = false;
        if (mainCountdownSnowTimer != null) {
            mainCountdownSnowTimer.cancel();
        }
        //Thanks to @byronshlin for the help on removing view when snowing stops
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int count = getChildCount();
                    for (int i = 0; i < count; i++) {
                        View view = getChildAt(i);
                        CountDownTimer timer = (CountDownTimer) view.getTag(R.id.tag_countdown_timer);
                        if (timer != null) {
                            timer.cancel();
                        }
                    }
                    if (mFlakesEndListener != null) {
                        mFlakesEndListener.onEndListener();
                    }
                    removeAllViews();
                }
            });
        }
    }

    public void stopSnowingClear() {
        isPaying = false;
        if (mainCountdownSnowTimer != null) {
            mainCountdownSnowTimer.cancel();
        }
        //Thanks to @byronshlin for the help on removing view when snowing stops
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int count = getChildCount();
                    for (int i = 0; i < count; i++) {
                        View view = getChildAt(i);
                        view.clearAnimation();
                        CountDownTimer timer = (CountDownTimer) view.getTag(R.id.tag_countdown_timer);
                        if (timer != null) {
                            timer.cancel();
                        }
                    }
                    removeAllViews();
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view != null) {
                view.clearAnimation();
            }
        }
    }

    public interface FlakesEndListener {
        void onEndListener();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPaying) {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}
