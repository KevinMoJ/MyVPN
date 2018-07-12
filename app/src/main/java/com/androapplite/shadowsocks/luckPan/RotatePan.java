package com.androapplite.shadowsocks.luckPan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.androapplite.shadowsocks.utils.Utils;
import com.androapplite.vpn3.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：
 * 作者：Nipuream
 * 时间: 2016-08-16 10:18
 * 邮箱：nipuream@163.com
 */
public class RotatePan extends View {

    private Context context;

    private int panNum = 0;

    private Paint dPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bgShadwPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int InitAngle = 0;
    private float radius = 0;
    private int verPanRadius;
    private int diffRadius;
    public static final int FLING_VELOCITY_DOWNSCALE = 4;
    private Integer[] images;
    private String[] strs;
    private List<Bitmap> bitmaps = new ArrayList<>();
    private GestureDetectorCompat mDetector;
    private ScrollerCompat scroller;
    private int screenWidth, screeHeight;
    private TypedArray typedArray;
    private int redColorNormal;
    private int redColorDark;
    private float textSize;

    private int CircleX, CircleY;

    //旋转一圈所需要的时间
    private static final long ONE_WHEEL_TIME = 500;

    public RotatePan(Context context) {
        this(context, null);
    }

    public RotatePan(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotatePan(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        screeHeight = getResources().getDisplayMetrics().heightPixels;
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        mDetector = new GestureDetectorCompat(context, new RotatePanGestureListener());
        scroller = ScrollerCompat.create(context);

        checkPanState(context, attrs);

//        dPaint.setColor(Color.rgb(255,133,132));
//        sPaint.setColor(Color.rgb(254,104,105));
        dPaint.setColor(redColorDark);
        sPaint.setColor(redColorNormal);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);
        dPaint.setShadowLayer(1, 0, 0, context.getResources().getColor(R.color.colorPrimary));//context.getResources().getColor(R.color.btn_red_normal)
        bgShadwPaint.setStyle(Paint.Style.STROKE);
        bgShadwPaint.setAntiAlias(true);
        InitAngle = 360 / panNum;
        verPanRadius = 360 / panNum;
        diffRadius = verPanRadius / 2;
        setClickable(true);

        for (int i = 0; i < panNum; i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), images[i]);
            bitmaps.add(bitmap);
        }
    }

    private void checkPanState(Context context, AttributeSet attrs) {
        typedArray = context.obtainStyledAttributes(attrs, R.styleable.RotatePan);
        panNum = typedArray.getInteger(R.styleable.RotatePan_pan_num, 0);
        if (360 % panNum != 0)
            throw new RuntimeException("can't split pan for all icon.");
        int nameArray = typedArray.getResourceId(R.styleable.RotatePan_names, -1);
        if (nameArray == -1) throw new RuntimeException("Can't find pan name.");
        strs = context.getResources().getStringArray(nameArray);
        int iconArray = typedArray.getResourceId(R.styleable.RotatePan_icons, -1);
        if (iconArray == -1) throw new RuntimeException("Can't find pan icon.");

        String[] iconStrs = context.getResources().getStringArray(iconArray);
        List<Integer> iconLists = new ArrayList<>();
        for (int i = 0; i < iconStrs.length; i++) {
            iconLists.add(context.getResources().getIdentifier(iconStrs[i], "mipmap", context.getPackageName()));
        }

        radius = typedArray.getDimension(R.styleable.RotatePan_rotate_radius, 125);
        redColorDark = typedArray.getColor(R.styleable.RotatePan_rotate_color_dark, Color.rgb(255, 133, 132));
        redColorNormal = typedArray.getColor(R.styleable.RotatePan_rotate_color, Color.rgb(254, 104, 105));
        textSize = typedArray.getDimension(R.styleable.RotatePan_rotate_text_size, 24);
        images = iconLists.toArray(new Integer[iconLists.size()]);
        typedArray.recycle();
        if (strs == null || images == null)
            throw new RuntimeException("Can't find string or icon resources.");
        if (strs.length != panNum || images.length != panNum)
            throw new RuntimeException("The string length or icon length  isn't equals panNum.");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int MinValue = Math.min(screenWidth, screeHeight);
        MinValue -= Utils.dip2px(context, 58) * 2;
        setMeasuredDimension(MinValue, MinValue);
        Log.d("rorate", "MinValue:" + MinValue + "screenWidth:" + screenWidth + "screeHeight:" + screeHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        int width = getWidth() - paddingLeft - paddingRight;
        int height = getHeight() - paddingTop - paddingBottom;

        int MinValue = Math.min(width, height);

//        radius = MinValue/2;
        CircleX = getWidth() / 2;
        CircleY = getHeight() / 2;

        RectF rectF = new RectF(getPaddingLeft(), getPaddingTop(), width, height);

        int angle = (panNum % 4 == 0) ? InitAngle : InitAngle - diffRadius;

        for (int i = 0; i < panNum; i++) {
            if (i % 2 == 0) {
                canvas.drawArc(rectF, angle, verPanRadius, true, dPaint);
            } else {
                canvas.drawArc(rectF, angle, verPanRadius, true, sPaint);
            }
            angle += verPanRadius;
        }

//        for(int i=0;i<panNum;i++){
//            drawIcon(width/2, height/2, radius, (panNum%4==0)?InitAngle + diffRadius : InitAngle, i, canvas);
//            InitAngle += verPanRadius;
//        }

        for (int i = 0; i < panNum; i++) {
            drawText((panNum % 4 == 0) ? InitAngle + diffRadius + (diffRadius * 3 / 4) : InitAngle + diffRadius, strs[i], (int) (2 * radius), textPaint, canvas, rectF);
            InitAngle += verPanRadius;
        }
//        drawRing(canvas);
    }

    private void drawRing(Canvas canvas) {
        int innerCircle = (int) radius; //内圆半径
        int ringWidth = Utils.dip2px(context, 10);   //圆环宽度

        // 第一种方法绘制圆环
        //绘制内圆
        bgShadwPaint.setColor(getResources().getColor(R.color.colorAccent));//context.getResources().getColor(R.color.btn_red_normal)
        bgShadwPaint.setStrokeWidth(2);
        canvas.drawCircle(CircleX, CircleY, innerCircle, this.bgShadwPaint);
    }

//    drawText((panNum%4==0)?InitAngle+diffRadius + (diffRadius*3/4):InitAngle+diffRadius ,strs[i], (int)( 2*radius), textPaint, canvas,rectF);

    private void drawText(float startAngle, String string, int mRadius, Paint mTextPaint, Canvas mCanvas, RectF mRange) {
        Path path = new Path();

        path.addArc(mRange, startAngle, verPanRadius);
        float textWidth = mTextPaint.measureText(string);

        //圆弧的水平偏移
//        float hOffset = (panNum % 7 == 0) ? ((float) (mRadius * Math.PI / panNum / 2 - textWidth / 4))
//                : ((float) (mRadius * Math.PI / panNum / 2 - textWidth / 4));

        float hOffset = (panNum % 7 == 0) ? ((float) (mRadius * Math.PI / panNum / 2 - textWidth / 4))
                : ((float) (mRadius * Math.PI / panNum - textWidth) / 2);
        if (string.equals("Thanks")) {
            hOffset = ((float) (mRadius * Math.PI / panNum / 2 - textWidth / 4));
        } else {
            hOffset = ((float) (mRadius * Math.PI / panNum / 2 - textWidth / 6));
        }
        Log.d("rorate", "-->" + (mRadius * Math.PI / panNum / 2) + "-->" + (textWidth / 4) + "==>" + hOffset);
        Log.d("rorate", "String-->" + string);

        //圆弧的垂直偏移
        float vOffset = mRadius / 2 / 4;

        mCanvas.drawTextOnPath(string, path, hOffset, vOffset, mTextPaint);
    }

    private void drawIcon(int xx, int yy, int mRadius, float startAngle, int i, Canvas mCanvas) {

        int imgWidth = mRadius / 4;

        float angle = (float) Math.toRadians(verPanRadius + startAngle);

        //确定图片在圆弧中 中心点的位置
        float x = (float) (xx + (mRadius / 2 + mRadius / 12) * Math.cos(angle));
        float y = (float) (yy + (mRadius / 2 + mRadius / 12) * Math.sin(angle));

        // 确定绘制图片的位置
        RectF rect = new RectF(x - imgWidth * 2 / 3, y - imgWidth * 2 / 3, x + imgWidth
                * 2 / 3, y + imgWidth * 2 / 3);

        Bitmap bitmap = bitmaps.get(i);

        mCanvas.drawBitmap(bitmap, null, rect, null);
    }


    public void setImages(List<Bitmap> bitmaps) {
        this.bitmaps = bitmaps;
        this.invalidate();
    }

    public void setStr(String... strs) {
        this.strs = strs;
        this.invalidate();
    }

    public String getStr(int pos) {
        if (strs != null && strs.length != 0) {
            return strs[pos];
        } else {
            return "";
        }
    }


    private ValueAnimator animtor;
    private ValueAnimator finalAnimtor;

    /**
     * 开始转动
     *
     * @param pos 如果 pos = -1 则随机，如果指定某个值，则转到某个指定区域
     */
    protected void startRotate(int pos) {

        //Rotate lap.
        int lap = (int) (Math.random() * 18) + 8;

        //Rotate angle.
        int angle = 0;
        if (pos < 0) {
            angle = (int) (Math.random() * 360);
        } else {
            int initPos = queryPosition();
            if (pos > initPos) {
                angle = (pos - initPos) * verPanRadius;
                lap -= 1;
                angle = 360 - angle;
            } else if (pos < initPos) {
                angle = (initPos - pos) * verPanRadius;
            } else {
                //nothing to do.
            }
        }

        //All of the rotate angle.
        int increaseDegree = lap * 360 + angle;
        long time = (lap + angle / 360) * ONE_WHEEL_TIME;
        int DesRotate = increaseDegree + InitAngle;

        //TODO 为了每次都能旋转到转盘的中间位置
        int offRotate = DesRotate % 360 % verPanRadius;
        DesRotate -= offRotate;
        DesRotate += diffRadius;

        animtor = ValueAnimator.ofInt(InitAngle, DesRotate);
        animtor.setInterpolator(new AccelerateDecelerateInterpolator());
        animtor.setDuration(time);
        Log.d("RotatePan", "time:" + time);
//        animtor.setRepeatCount(-1);
        animtor.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int updateValue = (int) animation.getAnimatedValue();
                Log.d("RotatePan", "updateValue:" + updateValue);
                InitAngle = (updateValue % 360 + 360) % 360;
                Log.d("RotatePan", "InitAngle:" + InitAngle);
                ViewCompat.postInvalidateOnAnimation(RotatePan.this);
            }
        });

        animtor.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (((LuckPanLayout) getParent()).getAnimationEndListener() != null) {
                    ((LuckPanLayout) getParent()).setStartBtnEnable(true);
                    ((LuckPanLayout) getParent()).setDelayTime(LuckPanLayout.DEFAULT_TIME_PERIOD);
                    ((LuckPanLayout) getParent()).getAnimationEndListener().endAnimation(queryPosition());
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (((LuckPanLayout) getParent()).getAnimationEndListener() != null) {
                    ((LuckPanLayout) getParent()).setStartBtnEnable(false);
                    ((LuckPanLayout) getParent()).setDelayTime(LuckPanLayout.DEFAULT_TIME_PERIOD);
                    ((LuckPanLayout) getParent()).getAnimationEndListener().startAnimation(queryPosition());
                }
            }
        });
        animtor.start();
    }

    /**
     * 固定某个位置
     *
     * @param pos 如果 pos = -1 则随机，如果指定某个值，则转到某个指定区域
     */
    protected void startThankRotate(int pos) {
        //Rotate lap.
        int lap = 1;

        //Rotate angle.
        int angle = 0;
        if (pos < 0) {
            angle = (int) (Math.random() * 360);
        } else {
            int initPos = queryPosition();
            if (pos > initPos) {
                angle = (pos - initPos) * verPanRadius;
                lap -= 1;
                angle = 360 - angle;
            } else if (pos < initPos) {
                angle = (initPos - pos) * verPanRadius;
            } else {
                //nothing to do.
            }
        }

        //All of the rotate angle.
        int increaseDegree = lap * 360 + angle;
//        long time = (lap + angle / 360) * ONE_WHEEL_TIME;
        long time = 500;
        int DesRotate = increaseDegree + InitAngle;

        //TODO 为了每次都能旋转到转盘的中间位置
        int offRotate = DesRotate % 360 % verPanRadius;
        DesRotate -= offRotate;
        DesRotate += diffRadius;

        finalAnimtor = ValueAnimator.ofInt(InitAngle, DesRotate);
        finalAnimtor.setInterpolator(new DecelerateInterpolator());
        finalAnimtor.setDuration(time);
        Log.d("RotatePan", "startThankRotate time:" + time);
        finalAnimtor.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int updateValue = (int) animation.getAnimatedValue();
                InitAngle = (updateValue % 360 + 360) % 360;
                ViewCompat.postInvalidateOnAnimation(RotatePan.this);
            }
        });

        finalAnimtor.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        finalAnimtor.start();
    }

    private int queryPosition() {
        InitAngle = (InitAngle % 360 + 360) % 360;
        int pos = InitAngle / verPanRadius;
        if (panNum == 4) pos++;
        return calcumAngle(pos);
    }

    private int calcumAngle(int pos) {
        if (pos >= 0 && pos <= panNum / 2) {
            pos = panNum / 2 - pos;
        } else {
            pos = (panNum - pos) + panNum / 2;
        }
        return pos;
    }


    @Override
    protected void onDetachedFromWindow() {
        clearAnimation();
        if (getParent() instanceof LuckPanLayout) {
            ((LuckPanLayout) getParent()).getHandler().removeCallbacksAndMessages(null);
        }
        super.onDetachedFromWindow();
    }


    // TODO ==================================== 手势处理 ===============================================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//手势处理
//        boolean consume = mDetector.onTouchEvent(event);
//        if (consume) {
//            getParent().getParent().requestDisallowInterceptTouchEvent(true);
//            return true;
//        }

        return super.onTouchEvent(event);
    }


    public void setRotate(int rotation) {
        rotation = (rotation % 360 + 360) % 360;
        InitAngle = rotation;
        ViewCompat.postInvalidateOnAnimation(this);
    }


    @Override
    public void computeScroll() {

        if (scroller.computeScrollOffset()) {
            setRotate(scroller.getCurrY());
        }

        super.computeScroll();
    }

    private class RotatePanGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return super.onDown(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float centerX = (RotatePan.this.getLeft() + RotatePan.this.getRight()) * 0.5f;
            float centerY = (RotatePan.this.getTop() + RotatePan.this.getBottom()) * 0.5f;

            float scrollTheta = vectorToScalarScroll(distanceX, distanceY, e2.getX() - centerX, e2.getY() -
                    centerY);
            int rotate = InitAngle -
                    (int) scrollTheta / FLING_VELOCITY_DOWNSCALE;

            setRotate(rotate);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float centerX = (RotatePan.this.getLeft() + RotatePan.this.getRight()) * 0.5f;
            float centerY = (RotatePan.this.getTop() + RotatePan.this.getBottom()) * 0.5f;

            float scrollTheta = vectorToScalarScroll(velocityX, velocityY, e2.getX() - centerX, e2.getY() -
                    centerY);

            scroller.abortAnimation();
            scroller.fling(0, InitAngle, 0, (int) scrollTheta / FLING_VELOCITY_DOWNSCALE,
                    0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            return true;
        }
    }

    //TODO 判断滑动的方向
    private float vectorToScalarScroll(float dx, float dy, float x, float y) {

        float l = (float) Math.sqrt(dx * dx + dy * dy);

        float crossX = -y;
        float crossY = x;

        float dot = (crossX * dx + crossY * dy);
        float sign = Math.signum(dot);

        return l * sign;
    }


}
