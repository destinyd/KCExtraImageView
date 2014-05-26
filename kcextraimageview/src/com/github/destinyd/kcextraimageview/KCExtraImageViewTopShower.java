package com.github.destinyd.kcextraimageview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.github.destinyd.kcextraimageview.photoview.Compat;
import com.github.destinyd.kcextraimageview.photoview.log.LogManager;

/**
 * Created by dd on 14-5-14.
 */
public class KCExtraImageViewTopShower extends ImageView {

    private static final String TAG = "KCExtraImageViewNewTopShower";
    public static final float DEFAULT_MAX_SCALE = 10.0f;
    public static final float DEFAULT_MID_SCALE = 1.0f;
    public static final float DEFAULT_MIN_SCALE = 0.0f;
    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;
    private boolean isShadowable;
    private int pendingState = KCExtraImageView.STATE_FULLSCREEN;

    public KCExtraImageViewTopShower(Context context) {
        this(context, null);
    }

    public KCExtraImageViewTopShower(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public KCExtraImageViewTopShower(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    KCExtraImageView fromImageView;

    public KCExtraImageView getFromImageView() {
        return fromImageView;
    }

    public void setFromImageView(KCExtraImageView fromImageView) {
        this.fromImageView = fromImageView;
    }


    protected static final Interpolator sInterpolator = new AccelerateDecelerateInterpolator();
    private static final int DEFAULT_DURATION = 1000;
    protected int DURATION = DEFAULT_DURATION;

    // These are set so we don't keep allocating them on the heap
    protected final Matrix mBaseMatrix = new Matrix();
    protected final Matrix mDrawMatrix = new Matrix();
    protected final Matrix mSuppMatrix = new Matrix();
    protected final RectF mDisplayRect = new RectF();
    protected final float[] mMatrixValues = new float[9];
    private int mIvTop, mIvRight, mIvBottom, mIvLeft;

    public RectF getDisplayRect() {
        return getDisplayRect(getDrawMatrix());
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    public void checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(getDrawMatrix());
        }
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private RectF getDisplayRect(Matrix matrix) {
        Drawable d = getDrawable();
        if (null != d) {
            mDisplayRect.set(0, 0, d.getIntrinsicWidth(),
                    d.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        return null;
    }

    public Matrix getDisplayMatrix() {
        return new Matrix(getDrawMatrix());
    }

    public Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    public float getScale() {
        return FloatMath.sqrt((float) Math.pow(getValue(mSuppMatrix, Matrix.MSCALE_X), 2) + (float) Math.pow(getValue(mSuppMatrix, Matrix.MSKEW_Y), 2));
    }

    private float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    private float scaleBase;//常规图时大小
    private float scaleFull;

    public float getBaseScale() {
        return scaleBase;
    }

    public void setScaleBase(float scale) {
        scaleBase = scale;
        setScale(scaleBase);
    }

    public float getScaleFull() {
        return scaleFull;
    }

    int xBase, yBase;

    public void setLocationBase(int left, int top) {
        xBase = left;
        yBase = top;
        setTranslate(left, top);
    }

    public void setScale(float scale) {
        setScale(scale, false);
    }

    public void setScale(float scale, boolean animate) {
        setScale(scale,
                x + (getRight()) / 2,
                y + (getBottom()) / 2,
                animate);
    }

    public void setScale(float scale, float focalX, float focalY,
                         boolean animate) {
        // Check to see if the scale is within bounds
        if (scale < mMinScale || scale > mMaxScale) {
            LogManager
                    .getLogger()
                    .i(TAG,
                            "Scale must be within the range of minScale and maxScale");
            return;
        }

        if (animate) {
            if (mCurrentAnimatedZoomRunnable != null)
                mCurrentAnimatedZoomRunnable.stop();
            mCurrentAnimatedZoomRunnable = new AnimatedZoomRunnable(getScale(), scale,
                    focalX, focalY);
            post(mCurrentAnimatedZoomRunnable);
        } else {
            float toScale = scale / getScale();
//            mSuppMatrix.postScale(toScale, toScale, focalX, focalY);
            mSuppMatrix.postScale(toScale, toScale, x, y);
            setImageViewMatrix(getDrawMatrix());
        }
    }

    AnimatedZoomRunnable mCurrentAnimatedZoomRunnable = null;

    public void moveToOrigin() {
        setTranslate(xBase - x, yBase - y, true, true);
    }

    public void rotationToOrigin(boolean anime) {
        setRotation(getBackAngle(), anime);
    }

    public class AnimatedZoomRunnable implements Runnable {

        private final float mFocalX, mFocalY;
        private final long mStartTime;
        private final float mZoomStart, mZoomEnd;
        boolean running = true;
        final float imageViewHeight, imageViewWidth;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mStartTime = System.currentTimeMillis();
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
            imageViewHeight = getImageViewHeight();
            imageViewWidth = getImageViewWidth();
        }


        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            if (running) {
                float t = interpolate();
                float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
                float deltaScale = scale / getScale();

                float midX = x;
                float midY = y;
                mSuppMatrix.postScale(deltaScale, deltaScale, midX, midY);
                setImageViewMatrix(getDrawMatrix());

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f) {
                    Compat.postOnAnimation(KCExtraImageViewTopShower.this, this);
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }


    AnimatedRotationRunnable runnableRotation = null;
    float angle = 0;

    public void setRotation(float degrees, boolean animate) {
        float targetAngle = degrees % 360;
        if (animate) {
            if (runnableRotation != null)
                runnableRotation.stop();
            runnableRotation = new AnimatedRotationRunnable(targetAngle);
            post(runnableRotation);
        } else {
            RectF rectF = getDisplayRect();
            mSuppMatrix.postRotate(targetAngle, rectF.centerX(), rectF.centerY());
            setImageViewMatrix(getDrawMatrix());
            addAngle(targetAngle);
        }
    }

    private void addAngle(float targetAngle) {
        angle = (angle + targetAngle) % 360;
    }

    public float getAngle() {
        return angle;
    }

    public float getBackAngle() {
        if (Math.abs(angle) > 180) {
            if (angle > 0)
                return 360 - angle;
            else
                return -360 - angle;
        } else {
            return -angle;
        }
    }

    public class AnimatedRotationRunnable implements Runnable {

        private boolean running = true;
        private final long mStartTime;
        private final float degrees;
        private float rotateDegrees;
        private float fromDegrees = 0;
        private float totalDegrees = 0;

        public void stop() {
            this.running = false;
        }

        public AnimatedRotationRunnable(final float degrees) {
            mStartTime = System.currentTimeMillis();
            this.degrees = degrees;
        }

        @Override
        public void run() {
            if (running) {
                float t = interpolate();

                rotateDegrees = t * degrees - fromDegrees;
                totalDegrees += rotateDegrees;
                RectF rectF = getDisplayRect();
                mSuppMatrix.postRotate(rotateDegrees, rectF.centerX(), rectF.centerY());
                setImageViewMatrix(getDrawMatrix()); // not check
                angle += rotateDegrees;
                fromDegrees = t * degrees;

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f && running) {
                    Compat.postOnAnimation(KCExtraImageViewTopShower.this, this);
                } else {
                    if (mAnimatedRotationListener != null)
                        mAnimatedRotationListener.onAnimated();
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }

    OnAnimatedListener mAnimatedRotationListener = null;

    /**
     * Interface definition for a callback to be invoked when the internal Matrix has changed for
     * this View.
     *
     * @author Chris Banes
     */
    public static interface OnAnimatedListener {
        /**
         * Callback for when the Matrix displaying the Drawable has changed. This could be because
         * the View's bounds have changed, or the user has zoomed.
         */
        void onAnimated();
    }

    public void setAnimatedRotationListener(OnAnimatedListener mAnimatedRotationListener) {
        this.mAnimatedRotationListener = mAnimatedRotationListener;
    }

    public void update() {
        if (true) {// if drawable not change
            // Make sure we using MATRIX Scale Type
            setImageViewScaleTypeMatrix(this);

            // Update the base matrix using the current drawable
            updateBaseMatrix(getDrawable());
        } else {
            // Reset the Matrix...
            resetMatrix();
        }
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private void resetMatrix() {
        mSuppMatrix.reset();
        setImageViewMatrix(getDrawMatrix());
        checkMatrixBounds();
    }


    static final int EDGE_NONE = -1;
    static final int EDGE_LEFT = 0;
    static final int EDGE_RIGHT = 1;
    static final int EDGE_BOTH = 2;
    private int mScrollEdge = EDGE_BOTH;
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


    public int getImageViewWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    public int getImageViewHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

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


    protected void setImageViewMatrix(Matrix matrix) {

        setImageMatrix(matrix);
    }


    /**
     * Set's the ImageView's ScaleType to Matrix.
     */
    public static void setImageViewScaleTypeMatrix(ImageView imageView) {
        /**
         * PhotoView sets it's own ScaleType to Matrix, then diverts all calls
         * setScaleType to this.setScaleType automatically.
         */
        if (null != imageView) {
            if (!ScaleType.MATRIX.equals(imageView.getScaleType())) {
                imageView.setScaleType(ScaleType.MATRIX);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isShadowable) {
            drawShadow(canvas);
            super.onDraw(canvas);

        } else {
            super.onDraw(canvas);
        }
    }

    public boolean isShadowable() {
        return isShadowable;
    }

    public void setShadowable(boolean isShadowable) {
        this.isShadowable = isShadowable;
    }

    static final int SHADOW_SIZE = 50;

    private void drawShadow(Canvas canvas) {

        RectF rectF = getDisplayRect();
        Rect rect = new Rect();
        rect.top = (int) (rectF.top + rectF.bottom / 2);// + SHADOW_PADDING_TOP;
        rect.left = (int) (rectF.left);// + rectF.width() * SHADOW_PADDING_HOR_PERCENT);
        rect.bottom = (int) rectF.bottom + SHADOW_SIZE;
        rect.right = (int) (rectF.right);// - rectF.width() * SHADOW_PADDING_HOR_PERCENT);

        Paint mShadow = new Paint();
        mShadow.setColor(getResources().getColor(android.R.color.transparent));
        mShadow.setShadowLayer(10.0f, 5f, 2f, 0x11000000);
        canvas.drawRect(rect, mShadow);

    }

    public float x = 0, y = 0;
    AnimatedTranslateRunnable animatedTranslateRunnable = null;

    public void setTranslate(float dX, float dY) {
        setTranslate(dX, dY, false, false);
    }

    public void setTranslate(float dX, float dY, boolean animate) {
        setTranslate(dX, dY, animate, false);
    }

    public void setTranslate(float dX, float dY,
                             boolean animate, boolean changeAlpha) {
        if (animate) {
            if (animatedTranslateRunnable != null)
                animatedTranslateRunnable.stop();
            post(new AnimatedTranslateRunnable(dX, dY, changeAlpha));
        } else {
            mSuppMatrix.postTranslate(dX, dY);
            setImageViewMatrix(getDrawMatrix());
            x += dX;
            y += dY;
        }
    }

    int alpha = 0;

    FrameLayout mParent;

    public void setParent(FrameLayout frameLayout) {
        mParent = frameLayout;
    }

    public void setBackgroundAlpha(int alpha) {
        setBackgroundAlpha(alpha, Color.BLACK, false);
    }

    public void setBackgroundAlpha(int alpha, boolean anime) {
        setBackgroundAlpha(alpha, Color.BLACK, anime);
    }

    AnimatedBackgroundAlphaRunnable mAnimatedBackgroundAlphaRunnable = null;

    public void setBackgroundAlpha(int toAlpha, int pcolor, boolean anime) {
        int color = pcolor & 0x00FFFFFF;
        int pAlpha;
        if (toAlpha > 255) {
            pAlpha = 255;
        } else if (toAlpha < 0) {
            pAlpha = 0;
        } else {
            pAlpha = toAlpha;
        }
        if (anime) {
            if (mAnimatedBackgroundAlphaRunnable != null)
                mAnimatedBackgroundAlphaRunnable.stop();

            mAnimatedBackgroundAlphaRunnable = new AnimatedBackgroundAlphaRunnable(alpha, pAlpha, color);
            post(mAnimatedBackgroundAlphaRunnable);
        } else {
            int backgroundColor = pAlpha * 0x1000000 + color;
            mParent.setBackgroundColor(backgroundColor);
            this.alpha = pAlpha;
        }
    }

    public class AnimatedBackgroundAlphaRunnable implements Runnable {
        private final long mStartTime;
        boolean running = true;
        float lastT = 0;
        final int fromAlpha;
        final int toAlpha;
        final int disAlpha;
        final int color;

        public AnimatedBackgroundAlphaRunnable(final int fromAlpha, final int toAlpha, final int color) {
            mStartTime = System.currentTimeMillis();
            this.fromAlpha = fromAlpha;
            this.toAlpha = toAlpha;
            this.disAlpha = toAlpha - fromAlpha;
            this.color = color;

        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            if (running) {
                float t = interpolate();
                int toAlpha = fromAlpha + (int) (t * disAlpha);
                int backgroundColor = toAlpha * 0x1000000;
                mParent.setBackgroundColor(backgroundColor);
                alpha = toAlpha;
                if (t < 1f) {
                    Compat.postOnAnimation(KCExtraImageViewTopShower.this, this);
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }


    public class AnimatedTranslateRunnable implements Runnable {

        private final long mStartTime;
        private final float mDX, mDY;
        boolean running = true;
        float lastT = 0;
        final int fromAlpha;
        final boolean changeAlpha;

        public AnimatedTranslateRunnable(final float dX, final float dY, final boolean change) {
            mStartTime = System.currentTimeMillis();
            mDX = dX;
            mDY = dY;
            fromAlpha = alpha;
            changeAlpha = change;
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            if (running) {
                float t = interpolate();
                float dT = t - lastT;
                float x = dT * mDX;
                float y = dT * mDY;

                mSuppMatrix.postTranslate(x, y);
                setImageViewMatrix(getDrawMatrix());
                KCExtraImageViewTopShower.this.x += x;
                KCExtraImageViewTopShower.this.y += y;

                if (changeAlpha) {
                    int toAlpha = (int) ((1 - t) * fromAlpha);
                    if (toAlpha < 0)
                        toAlpha = 0;
                    int backgroundColor = toAlpha * 0x1000000;
                    mParent.setBackgroundColor(backgroundColor);
                }
                lastT = t;

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f) {
                    Compat.postOnAnimation(KCExtraImageViewTopShower.this, this);
                } else {
                    if (mAnimatedTranslateListener != null)
                        mAnimatedTranslateListener.onAnimated();
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }

    OnAnimatedListener mAnimatedTranslateListener = null;

    public float getFitViewScale() {
        RectF rect = getDisplayRect();
        if (rect.width() * getImageViewHeight() > rect.height() * getImageViewWidth()) {//过宽
            return getImageViewWidth() / rect.width();
        } else {
            return getImageViewHeight() / rect.height();
        }
    }

    public void setAnimatedTranslateListener(OnAnimatedListener mAnimatedTranslateListener) {
        this.mAnimatedTranslateListener = mAnimatedTranslateListener;
    }

    public OnAnimatedListener getAnimatedTranslateListener() {
        return mAnimatedTranslateListener;
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            scaleFull = getFitViewScale();// * scaleBase;
            if(pendingState == KCExtraImageView.STATE_FULLSCREEN) {
                fromImageView.open();
            }
        }
    }

    public void to_fullscreen() {
        float fitScale = getScaleFull() * getBaseScale();// / imageViewTop.getScale();// / (imageViewTop.getScale() / imageViewTop.getBaseScale());// / imageViewTop.getFitViewScale());

        Drawable d = getDrawable();
        if (d == null)
            return;
        int imageWidth = d.getIntrinsicWidth();
        int imageHeight = d.getIntrinsicHeight();
        int left = 0;
        int top = 0;
        if (imageWidth * getHeight() > imageHeight * getWidth()) {
            top += (int) (
                    getHeight() -
                            (imageHeight * getScaleFull() * getBaseScale()) // 得到实际图片height
            ) / 2;
        } else if (imageWidth * getHeight() < imageHeight * getWidth()) {
            left += (int) (
                    getWidth() -
                            (imageWidth * getScaleFull() * getBaseScale()) // 得到实际图片width
            ) / 2;
        }

        setScale(fitScale, true);
        rotationToOrigin(true);
        setBackgroundAlpha(255, true);
        setTranslate(left - x, top - y, true);
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
    private long FLOAT_TIME = 100; // 0.1s 释放时间低于这个视为点击操作
    float currentScale;

    StateRunnable mStateRunnable = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (fromImageView.getState() == KCExtraImageView.STATE_FULLSCREEN) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:// 手指压下屏幕
//                    Log.e(TAG, "ACTION_DOWN");
                    currentPoint.set(event.getX(), event.getY());
                    if (mStateRunnable != null)
                        mStateRunnable.stop();
                    mStateRunnable = new StateRunnable(FLOAT_TIME);
                    mStateRunnable.run();
                    mActionMode = ACTION_MODE_DRAG;
                    break;

                case MotionEvent.ACTION_MOVE:// 手指在屏幕移动，该 事件会不断地触发
                    if (mStateRunnable != null && !mStateRunnable.isRunning())
                        if (mActionMode == ACTION_MODE_DRAG) {
                            move(event);
                        } else if (mActionMode == ACTION_MODE_ZOOM) {// 缩放
                            if (event.getPointerCount() >= 2) {
                                float endDis = distance(event);// 结束距离
                                currentFingerAngle = angle(event);
                                int turnAngle = (int) (currentFingerAngle - lastFingerAngle);// 变化的角度
                                if (endDis > 10f) {
                                    float scale = currentScale * endDis / startDis;// 得到缩放倍数
                                    //放大
                                    setScale(scale, false);
                                    if (Math.abs(turnAngle) > 5) {
                                        lastFingerAngle = currentFingerAngle;
                                        setRotation(turnAngle, false);
                                    }
                                }
                            }
                        }
                    break;

                case MotionEvent.ACTION_CANCEL:
                    mStateRunnable.stop();
                    break;
                case MotionEvent.ACTION_UP:// 手指离开屏
                    if (mActionMode != ACTION_MODE_NONE) {
//                        Log.e(TAG, "ACTION_UP !mStateRunnable.isDone():" + !mStateRunnable.isDone());
                        mActionMode = ACTION_MODE_NONE;
                        if (mStateRunnable.isRunning() && !mStateRunnable.isDone()) {
                            fromImageView.fall();
                        }
                    }
                    mStateRunnable.stop();
                    break;
                case MotionEvent.ACTION_POINTER_UP:// 有手指离开屏幕,但屏幕还有触点（手指）
                    if (mActionMode == ACTION_MODE_ZOOM) {
                        mActionMode = ACTION_MODE_NONE;
                    }
                    mStateRunnable.stop();
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:// 当屏幕上还有触点（手指），再有一个手指压下屏幕
                    if (mActionMode == ACTION_MODE_NONE
                            || (mActionMode == ACTION_MODE_DRAG && mStateRunnable.isRunning())) {
                        mActionMode = ACTION_MODE_ZOOM;
                        startDis = distance(event);
                        currentScale = getScale();
                        lastFingerAngle = angle(event);
                        currentFingerAngle = lastFingerAngle;
                        if (startDis > 10f) {
                            midPoint = mid(event);
                        }
                    }
                    mStateRunnable.stop();
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void move(MotionEvent event) {
        PointF newPoint = new PointF(event.getX(), event.getY());
        float distanceX = newPoint.x - currentPoint.x;
        float distanceY = newPoint.y - currentPoint.y;
        setTranslate(distanceX, distanceY);
        currentPoint = newPoint;
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

        public boolean isDone() {
            return done;
        }

        public boolean isRunning() {
            return running;
        }

        @Override
        public void run() {
            if (running) {
                long t = System.currentTimeMillis() - mStartTime;
                // We haven't hit our target scale yet, so post ourselves again
                if (t < mDuration) {
                    post(this);
                } else {
                    stop();
                    done = true;
                }
            }
        }
    }

    public void setPendingState(int pendingState) {
        this.pendingState = pendingState;
    }

    public static float distance(PointF fromP, PointF toP) {
        float dx = toP.x - fromP.x;
        float dy = toP.y - fromP.y;
        return FloatMath.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两点之间的距离
     *
     * @param event
     * @return
     */
    public static float distance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return FloatMath.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两点之间的中间点
     *
     * @param event
     * @return
     */
    public static PointF mid(MotionEvent event) {
        float midX = (event.getX(1) + event.getX(0)) / 2;
        float midY = (event.getY(1) + event.getY(0)) / 2;
        return new PointF(midX, midY);
    }

    /**
     * 计算两个手指连线与坐标轴的角度（单位为。C）
     *
     * @param event
     * @return
     */
    public static double angle(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

}
