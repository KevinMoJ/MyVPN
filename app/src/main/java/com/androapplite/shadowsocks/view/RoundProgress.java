package com.androapplite.shadowsocks.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.androapplite.shadowsocks.utils.DensityUtil;
import com.androapplite.vpn3.R;


/**
 *
 * @author wj
 * @date 2018/1/23
 */
public class RoundProgress extends View {

    private int mBgColor;
    private int mRoundColor;
    private float mTextSize = 10;
    private float mRoundWidth;
    private Paint mPaint;
    private int mCenterY, mViewHeight;
    private int mCenterX, mViewWidth;
    private float mRadius;
    private RectF mRectF;
    private int mProgerss = 0;

    private endAnimListener mListener;
    private ValueAnimator animator;

    public RoundProgress(Context context) {
        this(context, null);
    }

    public RoundProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mBgColor = context.getResources().getColor(R.color.normal_color);
        mRoundColor = context.getResources().getColor(android.R.color.white);
        mRoundWidth = DensityUtil.dip2px(context, 5);
    }

    /**
     * 当layout大小变化后会回调次方法
     * 通过这方法获取宽高
     *
     * @param w
     * @param h
     * @param oldw
     * @param oldh
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2;//控宽的中心点
        mCenterY = h / 2;//控件高的中心点
        mViewWidth = w;
        mViewHeight = h;
        //防止宽高不一致
        int min = Math.min(mCenterX, mCenterY);
        //半径
        mRadius = min - mRoundWidth / 2;
        //为画圆弧准备
        mRectF = new RectF(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //        super.onDraw(canvas); 我们自己来画
        //1、先画背景圆环
        mPaint.setColor(mBgColor);
        mPaint.setStrokeWidth(mRoundWidth);
//        canvas.drawCircle(mCenterX, mCenterY,mRadius,mPaint);
//        //2、画动态圆弧
//        mPaint.setColor(mRoundColor);
//        canvas.drawArc(mRectF,0, (float) mProgerss,false,mPaint);
        canvas.drawLine(0, mViewHeight, mViewWidth * mProgerss * 1.0f / 100, mViewHeight, mPaint);
    }

    public void setProgerss(int percent) {
        mProgerss = percent;
        invalidate();
    }

    public void setStyleCap() {
        if (mPaint != null) {
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mBgColor = getResources().getColor(R.color.normal_color);
        }
    }

    public void startAnim(long time) {
        animator = ValueAnimator.ofInt(0, 100).setDuration(time);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgerss = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    mListener.endAnim();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    public void setEndAnimListener(endAnimListener listener) {
        mListener = listener;
    }

    public void cancelAnimator(){
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
    }

    public interface endAnimListener {
        void endAnim();
    }

}
