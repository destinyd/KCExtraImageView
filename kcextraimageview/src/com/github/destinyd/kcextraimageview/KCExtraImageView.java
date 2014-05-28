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

import java.lang.reflect.Field;

import static com.github.destinyd.kcextraimageview.KCExtraImageViewTopShower.*;

/**
 * Created by dd on 14-5-14.
 */
public class KCExtraImageView extends ImageView implements OnAnimatedListener,
        ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "KCExtraImageView";
    private static final float DISTANCE_TO_FULLSCREEN = 200;
    private static final long OPEN_TIME = 1000; // 打开闲置时间1秒
    private static final float DISTANCE_DRAG = 10.0f;
    private static final float CONST_TO_FULLSCREEN_SCALE_THRESHOLD = 1.0f;
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

    protected PointF currentPoint = new PointF();
    private int mActionMode = 0;
    private static final int ACTION_MODE_NONE = 0;
    private static final int ACTION_MODE_DRAG = 1;
    private static final int ACTION_MODE_ZOOM = 2;
    private float startDis;// 开始距离
    private double lastFingerAngle;// 开始角度
    private double currentFingerAngle;// 开始角度
    private long FLOAT_TIME = 500; // 0.5s

    StateRunnable mStateRunnable = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null)
            return false;
        boolean handled = false;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:// 手指压下屏幕
                if (mState == STATE_NORMAL) {
                    initDrag(event);
                    if (mStateRunnable != null)
                        mStateRunnable.stop();
                    mStateRunnable = new StateRunnable(FLOAT_TIME);
                    mStateRunnable.run();
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:// 手指在屏幕移动，该 事件会不断地触发
                if (mStateRunnable != null) {
                    switch (mState) {
                        case STATE_SUSPENDED:
                            if (mActionMode == ACTION_MODE_DRAG) {
                                move(event);
                                handled = true;
                            } else if (mActionMode == ACTION_MODE_ZOOM) {// 缩放
                                if (event.getPointerCount() >= 2) {
                                    PointF midPointNew = mid(event);
                                    PointF vector = new PointF(midPointNew.x - currentPoint.x,
                                            midPointNew.y - currentPoint.y);
                                    move(vector);
                                    float endDis = distance(event);// 结束距离
                                    currentFingerAngle = angle(event);
                                    int turnAngle = (int) (currentFingerAngle - lastFingerAngle);// 变化的角度
                                    if (endDis > 10f) {
                                        float scale = imageViewTop.getBaseScale() * endDis / startDis;// 得到缩放倍数
                                        //放大
                                        imageViewTop.setScale(scale, false);
//                                    if (Math.abs(turnAngle) > 5) {
                                        lastFingerAngle = currentFingerAngle;
                                        imageViewTop.setRotation(turnAngle, false);
//                                    }

                                    }
                                    currentPoint = midPointNew;
                                    handled = true;
                                }
                            }
                            break;
                        case STATE_NORMAL:
                            PointF newPoint = new PointF(event.getX(), event.getY());
                            float distanceX = newPoint.x - currentPoint.x;
                            float distanceY = newPoint.y - currentPoint.y;
                            if (distanceY < 0 && Math.abs(distanceY) > Math.abs(distanceX) * 2
                                    && distance(newPoint, currentPoint) > DISTANCE_DRAG) {
                                mStateRunnable.stop();
                                suspend();
                                mActionMode = ACTION_MODE_DRAG;
                                handled = true;
                            }
                            break;
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mStateRunnable != null) {
                    mStateRunnable.stop();
                    handled = true;
                }
                break;
            case MotionEvent.ACTION_UP:// 手指离开屏
                switch (mState) {
                    case STATE_NORMAL:
                        if (mStateRunnable != null && mStateRunnable.running && !mStateRunnable.isDone()) {
                            Log.d(TAG, "ACTION_UP STATE_NORMAL to_open");
                            to_open();
                            mStateRunnable.stop();
                            handled = true;
                        }
                        break;
                    case STATE_SUSPENDED:
                        if (imageViewTop.getScale() <= imageViewTop.getBaseScale() * CONST_TO_FULLSCREEN_SCALE_THRESHOLD) {
                            Log.d(TAG, "ACTION_UP STATE_SUSPENDED fall");
                            fall();
                        } else {
                            Log.d(TAG, "ACTION_UP STATE_SUSPENDED open");
                            open();
                        }
                        handled = true;
                        break;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:// 有手指离开屏幕,但屏幕还有触点（手指）
                if (mStateRunnable != null) {
                    if (mState == STATE_SUSPENDED) {
                        if (event.getPointerCount() == 2) {
                            mActionMode = ACTION_MODE_DRAG;
                            initDrag(event);
                            handled = true;
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:// 当屏幕上还有触点（手指），再有一个手指压下屏幕
                if (event.getPointerCount() == 2) {
                    if (mState == STATE_NORMAL) {
                        Log.d(TAG, "ACTION_POINTER_DOWN STATE_NORMAL");
                        mStateRunnable.stop();
                        suspend();
                        initZoom(event);
                        handled = true;
                    } else if (mState == STATE_SUSPENDED) {
                        Log.d(TAG, "ACTION_POINTER_DOWN STATE_SUSPENDED");
                        initZoom(event);
                        handled = true;
                    }
                }
                break;
        }

        return handled;
    }

    private void initDrag(MotionEvent event) {
        if (is_first_pointer_up(event)) {
            currentPoint.set(event.getX(1), event.getY(1));
        } else {
            currentPoint.set(event.getX(), event.getY());
        }
    }

    private boolean is_first_pointer_up(MotionEvent event) {
        return ((event.getAction() >> (8 * 0)) & 0x0f) == 6 && ((event.getAction() >> (8 * 1)) & 0x0f) == 0;
    }

    private void initZoom(MotionEvent event) {
        mActionMode = ACTION_MODE_ZOOM;
        startDis = distance(event);
        lastFingerAngle = angle(event);
        currentFingerAngle = lastFingerAngle;
        if (startDis > 10f) {
            currentPoint = mid(event);
        }
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

    public static PointF mid(PointF currentPoint, MotionEvent event) {
        float x = (event.getX(1) + currentPoint.x) / 2;
        float y = (event.getY(1) + currentPoint.y) / 2;
        return new PointF(x, y);
    }

    public static PointF mid(MotionEvent event) {
        float x = (event.getX(1) + event.getX(0)) / 2;
        float y = (event.getY(1) + event.getY(0)) / 2;
        return new PointF(x, y);
    }

    private void move(MotionEvent event) {
        PointF newPoint = new PointF(event.getX(), event.getY());
        float distanceX = newPoint.x - currentPoint.x;
        float distanceY = newPoint.y - currentPoint.y;
        imageViewTop.setTranslate(distanceX, distanceY);
        currentPoint = newPoint;
    }

    private void move(PointF vector) {
        imageViewTop.setTranslate(vector.x, vector.y);
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

    public void fall() {
        anime_to_original();
        KCTopestHookLayer.getFactory(getContext()).unhook();
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

    private void suspend() {
        mState = STATE_SUSPENDED;
        create_top_shower(STATE_SUSPENDED);
        setVisibility(INVISIBLE);
    }

    private void to_open() {
        mState = STATE_SUSPENDED;
        create_top_shower(STATE_FULLSCREEN);
        setVisibility(INVISIBLE);
        KCTopestHookLayer.getFactory(getContext()).hook(this);
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
        wmParams.flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
//                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//        ;


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
        return get_statusbar_height(getContext());
    }

    private int get_actionbar_height() {
        return get_actionbar_height(getContext());
    }

    public static int get_statusbar_height(Context context) {
        Class c;
        try {
            c =
                    Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            int y = context.getResources().getDimensionPixelSize(x);
            return y;
        } catch (Exception e) {
        }
        return 0;
    }

    public static int get_actionbar_height(Context context) {
        TypedValue tv = new TypedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
//        } else if (imageView.getContext().getTheme().resolveAttribute(com.actionbarsherlock.R.attr.actionBarSize, tv, true)) {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, imageView.getContext().getResources().getDisplayMetrics());
        }
        return 0;
    }

    @Override
    public void onAnimated() {
        if (mState == STATE_BACKING) {
            remove_top_shower();
            setVisibility(VISIBLE);
            KCTopestHookLayer.getFactory(getContext()).unhook();
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
//        if(hasWindowFocus)
//            fall();
        if(!hasWindowFocus){
            if(frameLayoutTop != null){
                windowManager.removeView(frameLayoutTop);
                frameLayoutTop.destroyDrawingCache();
                frameLayoutTop = null;
                setVisibility(VISIBLE);
                mState = STATE_NORMAL;
                mActionMode = ACTION_MODE_NONE;
            }
        }
    }

    public int getDuration() {
        return imageViewTop.DURATION;
    }

    public void setDuration(int Duration) {
        imageViewTop.DURATION = Duration;
    }
}
