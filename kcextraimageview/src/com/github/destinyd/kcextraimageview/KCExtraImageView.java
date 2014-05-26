package com.github.destinyd.kcextraimageview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.github.destinyd.kcextraimageview.photoview.PhotoView;
import com.github.destinyd.kcextraimageview.photoview.PhotoViewAttacher;

import java.lang.reflect.Field;

import static com.github.destinyd.kcextraimageview.KCExtraImageViewTopShower.*;

/**
 * Created by dd on 14-5-14.
 */
public class KCExtraImageView extends ImageView implements View.OnTouchListener, OnAnimatedListener,
        ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "KCExtraImageViewNew";
    private static final float DISTANCE_TO_FULLSCREEN = 100;
    private static final long OPEN_TIME = 1000; // 打开闲置时间1秒
    WindowManager windowManager;

    // These are set so we don't keep allocating them on the heap
    private final Matrix mBaseMatrix = new Matrix();
    private final Matrix mDrawMatrix = new Matrix();
    protected final Matrix mSuppMatrix = new Matrix();
    private final RectF mDisplayRect = new RectF();
    private final float[] mMatrixValues = new float[9];

    public KCExtraImageView(Context context) {
        this(context, null);
    }

    public KCExtraImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KCExtraImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
        initMatrix();
    }

    ScaleType mPendingScaleType = null;

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (null != getDrawable()) {
            super.setScaleType(scaleType);
        } else {
            mPendingScaleType = scaleType;
        }
    }

    float scaleBase = 1;

    private void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setOnTouchListener(this);
        ViewTreeObserver observer = getViewTreeObserver();
        if (null != observer)
            observer.addOnGlobalLayoutListener(this);
        if (null != mPendingScaleType) {
            setScaleType(mPendingScaleType);
            mPendingScaleType = null;
        }
    }

    private void initMatrix() {
        resetMatrix();
        setImageViewScaleTypeMatrix(this);
    }

    void initOnLayout() {
        RectF rect = getDisplayRect();
        if (rect == null)
            return;
        if (rect.width() * getImageViewHeight() > rect.height() * getImageViewWidth()) {//过宽
            scaleBase = getImageViewWidth() / rect.width();
        } else {
            scaleBase = getImageViewHeight() / rect.height();
        }

        mSuppMatrix.setScale(scaleBase, scaleBase);
        setImageMatrix(getDrawMatrix());

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            initOnLayout();
        }
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private void resetMatrix() {
        mSuppMatrix.reset();
        setImageMatrix(getDrawMatrix());
        checkMatrixBounds();
    }

    protected static final int EDGE_NONE = -1;
    protected static final int EDGE_LEFT = 0;
    protected static final int EDGE_RIGHT = 1;
    protected static final int EDGE_BOTH = 2;
    protected int mScrollEdge = EDGE_BOTH;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;

    protected boolean checkMatrixBounds() {
        final RectF rect = getDisplayRect(getDrawMatrix());
        if (null == rect) {
            return false;
        }

        final float height = rect.height(), width = rect.width();
        float deltaX = 0, deltaY = 0;

        final int viewHeight = getImageViewHeight();
        if (height <= viewHeight) {
            switch (mScaleType) {
                case FIT_START:
                    deltaY = -rect.top;
                    break;
                case FIT_END:
                    deltaY = viewHeight - height - rect.top;
                    break;
                default:
                    deltaY = (viewHeight - height) / 2 - rect.top;
                    break;
            }
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom;
        }

        final int viewWidth = getImageViewWidth();
        if (width <= viewWidth) {
            switch (mScaleType) {
                case FIT_START:
                    deltaX = -rect.left;
                    break;
                case FIT_END:
                    deltaX = viewWidth - width - rect.left;
                    break;
                default:
                    deltaX = (viewWidth - width) / 2 - rect.left;
                    break;
            }
            mScrollEdge = EDGE_BOTH;
        } else if (rect.left > 0) {
            mScrollEdge = EDGE_LEFT;
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
            mScrollEdge = EDGE_RIGHT;
        } else {
            mScrollEdge = EDGE_NONE;
        }

        // Finally actually translate the matrix
        mSuppMatrix.postTranslate(deltaX, deltaY);
        return true;
    }


    protected int getImageViewWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    protected int getImageViewHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }


    public RectF getDisplayRect() {
        return getDisplayRect(getDrawMatrix());
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    protected RectF getDisplayRect(Matrix matrix) {
        Drawable d = getDrawable();
        if (null != d) {
            mDisplayRect.set(0, 0, d.getIntrinsicWidth(),
                    d.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        return null;
    }

    public Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
    }

    protected PointF startPoint = new PointF();
    protected PointF currentPoint = new PointF();
    private int mActionMode = 0;
    private static final int ACTION_MODE_NONE = 0;
    private static final int ACTION_MODE_DRAG = 1;
    private static final int ACTION_MODE_ZOOM = 2;
    private float startDis;// 开始距离
    private PointF midPoint;// 中间点
    private double lastFingerAngle;// 开始角度
    private double currentFingerAngle;// 开始角度
    private long FLOAT_TIME = 500; // 0.5s

    StateRunnable mStateRunnable = null;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (getDrawable() == null)
            return false;
        boolean handled = false;
        long costTime;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:// 手指压下屏幕
                if (mState == STATE_NORMAL) {
                    startPoint.set(event.getX(), event.getY());
                    currentPoint.set(event.getX(), event.getY());
                    handled = true;
                    if (mStateRunnable != null)
                        mStateRunnable.stop();
                    mStateRunnable = new StateRunnable(FLOAT_TIME);
                    mStateRunnable.run();
                }
                break;

            case MotionEvent.ACTION_MOVE:// 手指在屏幕移动，该 事件会不断地触发
                if (mState == STATE_SUSPENDED) {
                    if (mActionMode == ACTION_MODE_DRAG) {
                        move(event);
                    } else if (mActionMode == ACTION_MODE_ZOOM) {// 缩放
                        if (event.getPointerCount() >= 2) {
                            float endDis = distance(event);// 结束距离
                            currentFingerAngle = angle(event);
                            int turnAngle = (int) (currentFingerAngle - lastFingerAngle);// 变化的角度
                            if (endDis > 10f) {
                                float scale = imageViewTop.getBaseScale() * endDis / startDis;// 得到缩放倍数
                                //放大
                                imageViewTop.setScale(scale, false);
                                if (Math.abs(turnAngle) > 5) {
                                    lastFingerAngle = currentFingerAngle;
                                    imageViewTop.setRotation(turnAngle, false);
                                }

                            }
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mStateRunnable.stop();
                break;
            case MotionEvent.ACTION_UP:// 手指离开屏
                if (mStateRunnable.running && !mStateRunnable.isDone()) {
                    to_open();
                    break;
                }
                costTime = System.currentTimeMillis() - mStartOpen;
                if (costTime > OPEN_TIME && mState != STATE_FULLSCREEN && (mActionMode == ACTION_MODE_DRAG || mActionMode == ACTION_MODE_ZOOM)) {
                    fall();
                }
//                mStateRunnable.stop();
                break;
            case MotionEvent.ACTION_POINTER_UP:// 有手指离开屏幕,但屏幕还有触点（手指）
//                costTime = System.currentTimeMillis() - mStartOpen;
                if (mState == STATE_NORMAL) {
//                        (costTime > OPEN_TIME && mState != STATE_FULLSCREEN && (mActionMode == ACTION_MODE_DRAG || mActionMode == ACTION_MODE_ZOOM) && imageViewTop != null) {
//                    if (imageViewTop.getScale() <= imageViewTop.getBaseScale()) {
//                        Log.e(TAG, "ACTION_POINTER_UP fall");
//                        fall();
//                    } else {
//                        Log.e(TAG, "ACTION_POINTER_UP open");
//                        open();
//                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:// 当屏幕上还有触点（手指），再有一个手指压下屏幕
                if (mState != STATE_SUSPENDED && mState != STATE_BACKING) {
                    mStateRunnable.stop();
                    mActionMode = ACTION_MODE_ZOOM;
                    suspended();
                    startDis = distance(event);
                    lastFingerAngle = angle(event);
                    currentFingerAngle = lastFingerAngle;
                    if (startDis > 10f) {
                        midPoint = mid(this, event);
                    }
                }
                break;
        }

        return true;
    }

    /**
     * 计算两点之间的中间点
     *
     * @param event
     * @return
     */
    public static PointF mid(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float midX = (event.getX(1) + event.getX(0)) / 2 + location[0];
        float midY = (event.getY(1) + event.getY(0)) / 2 + location[1];
        return new PointF(midX, midY);
    }


    private void move(MotionEvent event) {
        PointF newPoint = new PointF(event.getX(), event.getY());
        float distanceX = newPoint.x - currentPoint.x;
        float distanceY = newPoint.y - currentPoint.y;
        imageViewTop.setTranslate(distanceX, distanceY);
        currentPoint = newPoint;
        float dis = distance(startPoint, newPoint);

        float percent = dis / DISTANCE_TO_FULLSCREEN;
        int alpha = (int) (percent * 255);
        if (alpha > 255)
            alpha = 255;
        imageViewTop.setBackgroundAlpha(alpha);
        if (dis >= DISTANCE_TO_FULLSCREEN) {
            open();
        }
    }

    long mStartOpen = 0;

    public void open() {
        mState = STATE_OPENING;
        imageViewTop.setAnimatedTranslateListener(new OnAnimatedListener() {
            @Override
            public void onAnimated() {
//                open_full_screen();
                imageViewTop.setAnimatedTranslateListener(KCExtraImageView.this);
                mState = STATE_FULLSCREEN;
            }
        });
        mStartOpen = System.currentTimeMillis();

        imageViewTop.to_fullscreen();
    }

    public int getState() {
        return mState;
    }

    private void open_full_screen() {
//        hide_all_view();
        create_open_image_view();
    }

    private PhotoView mOpenView = null;

    private void create_open_image_view() {
        mOpenView = new PhotoView(getContext());
        mOpenView.setBackgroundColor(Color.BLACK);
        mOpenView.setImageDrawable(getDrawable());
        mOpenView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                close_full_screen();
            }
        });

//        // 获取WindowManager
//        WindowManager wm = (WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
////        // 设置LayoutParams(全局变量）相关参数
//        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
////        wmParams.type =  WindowManager.LayoutParams.TYPE_PHONE; // 设置window type
//        wmParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
//        // 设置Window flag
//        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//
//        wmParams.gravity = Gravity.CENTER;
//        wm.addView(mOpenView, wmParams);
//        imageViewTop.setVisibility(GONE);
        if (frameLayoutTop != null)
            frameLayoutTop.addView(mOpenView);
    }


    public void close_full_screen() {
//        resume_all_view();
        close_open_image_view();
//        mState = STATE_NORMAL;
    }

    public void close_open_image_view() {
        if (mOpenView != null && frameLayoutTop != null) {
            frameLayoutTop.removeView(mOpenView);
            mOpenView = null;
//            imageViewTop.setBackgroundResource(android.R.color.transparent);
        }

        imageViewTop.setVisibility(VISIBLE);
        fall();
    }

    public void fall() {
        if (mState == STATE_FULLSCREEN || mState == STATE_SUSPENDED) {
            if (mState == STATE_SUSPENDED) {
                if (mActionMode == ACTION_MODE_ZOOM || mActionMode == ACTION_MODE_DRAG) {
                    anime_to_original();
                }
            } else {
                anime_to_original();
            }
            setControled(false);
        }
    }

    private void fullscreen_to_original() {
        mState = STATE_BACKING;
        imageViewTop.setScale(scaleBase, true);
        imageViewTop.moveToOrigin();
    }

    private void anime_to_original() {
        mState = STATE_BACKING;
        imageViewTop.moveToOrigin();
        imageViewTop.rotationToOrigin(true);
        imageViewTop.setScale(scaleBase, true);
    }

    private int mState = 0;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_SUSPENDED = 1;
    public static final int STATE_FULLSCREEN = 2;
    public static final int STATE_OPENING = 3;
    public static final int STATE_BACKING = 4;

    private void suspended() {
        mState = STATE_SUSPENDED;
        create_top_shower(STATE_SUSPENDED);
        setVisibility(INVISIBLE);
        setControled(true);
    }

    private void to_open() {
        mState = STATE_SUSPENDED;
        create_top_shower(STATE_FULLSCREEN);
        setVisibility(INVISIBLE);
        setControled(true);
    }

    FrameLayout frameLayoutTop = null;
    KCExtraImageViewTopShower imageViewTop;

    private void create_top_shower(int state) {
        initTopestShower();

        int actionBarHeight = get_actionbar_height();
        int statusBarHeight = get_statusbar_height();
        int heightTopLayer = windowManager.getDefaultDisplay().getHeight() - actionBarHeight - statusBarHeight;
        int widthTopLayer = windowManager.getDefaultDisplay().getWidth();

        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(widthTopLayer, heightTopLayer);
        wmParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置Window flag
        wmParams.flags = //WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        ;


        wmParams.gravity = Gravity.TOP | Gravity.LEFT;
        wmParams.y = actionBarHeight + statusBarHeight;
        wmParams.width = widthTopLayer;
        wmParams.height = heightTopLayer;

        int[] location = new int[2];
        getLocationOnScreen(location);

        int leftImageView = location[0] + getPaddingLeft();// getAbsoluteLeft();
        int topImageView = location[1] - actionBarHeight - statusBarHeight + getPaddingTop();
        frameLayoutTop = new FrameLayout(getContext());
        FrameLayout.LayoutParams layoutParamsImageViewInTopLayer = new FrameLayout.LayoutParams(widthTopLayer, heightTopLayer);
        layoutParamsImageViewInTopLayer.gravity = Gravity.TOP | Gravity.LEFT;

        imageViewTop.setImageDrawable(getDrawable());

        initTopShowerLocationAndScale(leftImageView, topImageView);

        imageViewTop.setPendingState(state);

        frameLayoutTop.addView(imageViewTop, layoutParamsImageViewInTopLayer);

        windowManager.addView(frameLayoutTop, wmParams);
        imageViewTop.setParent(frameLayoutTop);


    }

    float mToperShowerScale;

    private void initTopShowerLocationAndScale(int leftImageView, int topImageView) {
        RectF rectTop = imageViewTop.getDisplayRect();
        boolean isTooWide = false;
        if (rectTop == null) {
            return;
        }
        if (rectTop.width() * getImageViewHeight() > rectTop.height() * getImageViewWidth())//too wide
        {
            mToperShowerScale = getImageViewWidth() / rectTop.width();
            isTooWide = true;
        } else {
            mToperShowerScale = getImageViewHeight() / rectTop.height();
        }

        imageViewTop.setScaleBase(mToperShowerScale);
        int left, top, fix;
        if (isTooWide) {
            fix = (int) ((getImageViewHeight() - getDisplayRect().height()) / 2);
            left = leftImageView;
            top = topImageView + fix;// - getPaddingTop();// - getPaddingBottom();
        } else {
            fix = (int) (getImageViewWidth() - getDisplayRect().width()) / 2;
            left = leftImageView + fix;// - getPaddingRight();
            top = topImageView;
        }
        imageViewTop.setLocationBase(left, top);
        imageViewTop.setImageViewMatrix(imageViewTop.getDrawMatrix()); // 原始尺寸
    }

    private void initTopestShower() {
        imageViewTop = new KCExtraImageViewTopShower(getContext());

        imageViewTop.setFromImageView(this);

        imageViewTop.setDrawingCacheEnabled(true);
        imageViewTop.setAnimatedRotationListener(this);
        imageViewTop.setAnimatedTranslateListener(this);
        // Make sure we using MATRIX Scale Type
        setImageViewScaleTypeMatrix(imageViewTop);

        imageViewTop.setShadowable(true);

        if (imageViewTop.isInEditMode()) {
            return;
        }

    }

    private int get_statusbar_height() {
        Class c;
        try {
            c =
                    Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            int y = getContext().getResources().getDimensionPixelSize(x);
            return y;
        } catch (Exception e) {
        }
        return 0;

    }

    private int get_actionbar_height() {
        TypedValue tv = new TypedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                return TypedValue.complexToDimensionPixelSize(tv.data, getContext().getResources().getDisplayMetrics());
//        } else if (imageView.getContext().getTheme().resolveAttribute(com.actionbarsherlock.R.attr.actionBarSize, tv, true)) {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, imageView.getContext().getResources().getDisplayMetrics());
        }
        return 0;
    }

    private boolean isParentNotAboveRoot(View view) {
        View tmp = (View) view.getParent();
        return tmp.getId() != android.R.id.content;
    }

    @Override
    public void onAnimated() {
        if (mState == STATE_BACKING) {
            remove_top_shower();
            setVisibility(VISIBLE);
            mState = STATE_NORMAL;
        }
    }

    private void remove_top_shower() {
        frameLayoutTop.removeView(imageViewTop);
        windowManager.removeView(frameLayoutTop);
        imageViewTop = null;
        frameLayoutTop = null;
    }

    private int mIvTop, mIvRight, mIvBottom, mIvLeft;

    @Override
    public void onGlobalLayout() {
        Drawable drawable = getDrawable();
        if (null == drawable) {
            return;
        }
        final int top = getTop();
        final int right = getRight();
        final int bottom = getBottom();
        final int left = getLeft();

        /**
         * We need to check whether the ImageView's bounds have changed.
         * This would be easier if we targeted API 11+ as we could just use
         * View.OnLayoutChangeListener. Instead we have to replicate the
         * work, keeping track of the ImageView's bounds and then checking
         * if the values change.
         */
        if (top != mIvTop || bottom != mIvBottom || left != mIvLeft
                || right != mIvRight) {
            // Update our base matrix, as the bounds have changed
            updateBaseMatrix(getDrawable());

            // Update values as something has changed
            mIvTop = top;
            mIvRight = right;
            mIvBottom = bottom;
            mIvLeft = left;
        }
    }


//    float locationTopFix = 0, locationLeftFix = 0;

    /**
     * Calculate Matrix for FIT_CENTER
     *
     * @param d - Drawable being displayed
     */
    private void updateBaseMatrix(Drawable d) {
        final float viewWidth = getImageViewWidth();
        final float viewHeight = getImageViewHeight();
        final int drawableWidth = d.getIntrinsicWidth();
        final int drawableHeight = d.getIntrinsicHeight();

        mBaseMatrix.reset();

        final float widthScale = viewWidth / drawableWidth;
        final float heightScale = viewHeight / drawableHeight;

        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate((viewWidth - drawableWidth) / 2F,
                    (viewHeight - drawableHeight) / 2F);

        } else if (mScaleType == ScaleType.CENTER_CROP) {
            float scale = Math.max(widthScale, heightScale);
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                    (viewHeight - drawableHeight * scale) / 2F);

        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            float scale = Math.min(1.0f, Math.min(widthScale, heightScale));
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                    (viewHeight - drawableHeight * scale) / 2F);

        } else {
            RectF mTempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF mTempDst = new RectF(0, 0, viewWidth, viewHeight);

            switch (mScaleType) {
                case FIT_CENTER:
                    mBaseMatrix
                            .setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.CENTER);
                    break;

                case FIT_START:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.START);
                    break;

                case FIT_END:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.END);
                    break;

                case FIT_XY:
                    mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.FILL);
                    break;

                default:
                    break;
            }
        }

        resetMatrix();
    }

    public class StateRunnable implements Runnable {

        private boolean running = true;
        private final long mStartTime;
        private final long mDuration;
        boolean done = false;

        public void stop() {
            this.running = false;
        }

        public StateRunnable(final long millisecond) {
            mStartTime = System.currentTimeMillis();
            mDuration = millisecond;
        }

        @Override
        public void run() {
            if (running) {
                long t = System.currentTimeMillis() - mStartTime;
                // We haven't hit our target scale yet, so post ourselves again
                if (t < mDuration) {
                    post(this);
                } else {
                    done();
                }
            }
        }

        private void done() {
            done = true;
        }

        public boolean isDone() {
            return done;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        int status = hasWindowFocus ? VISIBLE : GONE;
        if (!hasWindowFocus) {
            if (frameLayoutTop != null)
                frameLayoutTop.setVisibility(status);
            if (imageViewTop != null)
                imageViewTop.setVisibility(status);
            if (mOpenView != null)
                mOpenView.setVisibility(status);
            setControled(false);
        }
    }

    public int getDuration() {
        return imageViewTop.DURATION;
    }

    public void setDuration(int Duration) {
        imageViewTop.DURATION = Duration;
    }

    static private boolean controled = false;

    public static void setControled(boolean controled) {
        KCExtraImageView.controled = controled;
    }
}
