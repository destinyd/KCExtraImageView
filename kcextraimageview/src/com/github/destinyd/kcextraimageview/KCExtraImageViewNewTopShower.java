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
import android.widget.ImageView;
import com.github.destinyd.kcextraimageview.photoview.Compat;
import com.github.destinyd.kcextraimageview.photoview.log.LogManager;

/**
 * Created by dd on 14-5-14.
 */
public class KCExtraImageViewNewTopShower extends ImageView {

    private static final String TAG = "KCExtraImageViewNewTopShower";
    public static final float DEFAULT_MAX_SCALE = 3.0f;
    public static final float DEFAULT_MID_SCALE = 1.0f;
    public static final float DEFAULT_MIN_SCALE = 0.1f;
    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;
    private boolean isShadowable;

    public KCExtraImageViewNewTopShower(Context context) {
        this(context, null);
    }

    public KCExtraImageViewNewTopShower(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public KCExtraImageViewNewTopShower(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    KCExtraImageViewNew fromImageView;

    public KCExtraImageViewNew getFromImageView() {
        return fromImageView;
    }

    public void setFromImageView(KCExtraImageViewNew fromImageView) {
        this.fromImageView = fromImageView;
    }


    protected static final Interpolator sInterpolator = new AccelerateDecelerateInterpolator();
    private static final int DEFAULT_ZOOM_DURATION = 200;
    protected int ZOOM_DURATION = DEFAULT_ZOOM_DURATION;

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


    public void setScale(float scale) {
        setScale(scale, false);
    }

    public void setScale(float scale, boolean animate) {
//        Log.e(TAG, "setScale:" + scale);
        setScale(scale,
                (getRight()) / 2,
                (getBottom()) / 2,
                animate);
    }

    public void setScale(float scale, float focalX, float focalY,
                         boolean animate) {
        Log.e(TAG, "setScale:" + scale);
        // Check to see if the scale is within bounds
        if (scale < mMinScale || scale > mMaxScale) {
            LogManager
                    .getLogger()
                    .i(TAG,
                            "Scale must be within the range of minScale and maxScale");
            return;
        }

        if (animate) {
            mCurrentAnimatedZoomRunnable = new AnimatedZoomRunnable(getScale(), scale,
                    focalX, focalY);
            mCurrentAnimatedZoomRunnable.stop();
            post(new AnimatedZoomRunnable(getScale(), scale,
                    focalX, focalY));
        } else {
            mSuppMatrix.setScale(scale, scale, focalX, focalY);
            setImageViewMatrix(getDrawMatrix());
        }
    }

    AnimatedZoomRunnable mCurrentAnimatedZoomRunnable = null;


    public class AnimatedZoomRunnable implements Runnable {

        private final float mFocalX, mFocalY;
        private final long mStartTime;
        private final float mZoomStart, mZoomEnd;
        boolean running = true;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mStartTime = System.currentTimeMillis();
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
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

                mSuppMatrix.postScale(deltaScale, deltaScale, mFocalX, mFocalY);
                setImageViewMatrix(getDrawMatrix());
                ;

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f) {
                    Compat.postOnAnimation(KCExtraImageViewNewTopShower.this, this);
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / ZOOM_DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }


    AnimatedRotationRunnable runnableRotation = null;

    public void setPhotoViewRotation(float degrees, boolean animate) {
        float targetAngle = degrees % 360;
        if (animate) {
            if (runnableRotation != null)
                runnableRotation.stop();
            runnableRotation = new AnimatedRotationRunnable(targetAngle);
            post(runnableRotation);
        } else {
            mSuppMatrix.setRotate(targetAngle);
            setImageViewMatrix(getDrawMatrix());
            ;
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
//            Log.e(TAG, "degrees:" + degrees);
        }

        @Override
        public void run() {
            if (running) {
//                ImageView imageView = getImageView();
                float t = interpolate();

                rotateDegrees = t * degrees - fromDegrees;
                totalDegrees += rotateDegrees;
                if (Math.abs(totalDegrees) > Math.abs(degrees)) {
                    Log.e(TAG, "totalDegrees:" + totalDegrees);
                    Log.e(TAG, "Math.abs(totalDegrees) > Math.abs(degrees)");
                    rotateDegrees = degrees - fromDegrees;
                }
//                Log.e(TAG, "rotateDegrees:" + rotateDegrees);
                mSuppMatrix.postRotate(rotateDegrees);
//                setImageViewMatrix(getDrawMatrix());;
                setImageViewMatrix(getDrawMatrix()); // not check
                fromDegrees = t * degrees;

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f && running) {
//                    Log.e(TAG, "totalDegrees:" + totalDegrees);
                    Compat.postOnAnimation(KCExtraImageViewNewTopShower.this, this);
                } else {
                    if (mAnimatedRotationListener != null)
                        mAnimatedRotationListener.onAnimated();
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / ZOOM_DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }

    AnimatedRotationListener mAnimatedRotationListener = null;

    /**
     * Interface definition for a callback to be invoked when the internal Matrix has changed for
     * this View.
     *
     * @author Chris Banes
     */
    public static interface AnimatedRotationListener {
        /**
         * Callback for when the Matrix displaying the Drawable has changed. This could be because
         * the View's bounds have changed, or the user has zoomed.
         */
        void onAnimated();
    }

    public void setAnimatedRotationListener(AnimatedRotationListener mAnimatedRotationListener) {
        this.mAnimatedRotationListener = mAnimatedRotationListener;
    }

    public void update() {
//        if (mZoomEnabled) {
        if (true) {
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


    private int getImageViewWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getImageViewHeight() {
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
//            if (superOnDraw(canvas)) return; // couldn't resolve the URI
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

    private Matrix mMatrix;
    private Paint mPaint;
    //    static final int SHADOW_PADDING_TOP = -10;
    static final float SHADOW_PADDING_HOR_PERCENT = 0.2f;
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

    float x, y;
    AnimatedTranslateRunnable animatedTranslateRunnable = null;

    public void setTranlate(float dX, float dY){
        setTranlate(dX, dY, false);
    }

    public void setTranlate(float dX, float dY,
                            boolean animate) {
        if (animate) {
            if (animatedTranslateRunnable != null)
                animatedTranslateRunnable.stop();
            post(new AnimatedTranslateRunnable(x, y,
                    dX, dY));
        } else {
            mSuppMatrix.postTranslate(dX, dY);
//            checkAndDisplayMatrix();
            setImageViewMatrix(getDrawMatrix());
        }
        x = dX;
        y = dY;
    }


    public class AnimatedTranslateRunnable implements Runnable {

        private final float mFromX, mFromY;
        private final long mStartTime;
        private final float mToX, mToY;
        boolean running = true;

        public AnimatedTranslateRunnable(final float fromX, final float fromY,
                                         final float toX, final float toY) {
            mStartTime = System.currentTimeMillis();
            mFromX = fromX;
            mFromY = fromY;
            mToX = toX;
            mToY = toY;
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            if (running) {
                float t = interpolate();
                float x = mFromX + t * (mToX - mFromX);
                float y = mFromY + t * (mToY - mFromY);

                mSuppMatrix.postTranslate(x, y);
                setImageViewMatrix(getDrawMatrix());

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f) {
                    Compat.postOnAnimation(KCExtraImageViewNewTopShower.this, this);
                }
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / ZOOM_DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }
    }


    public static float distance(PointF fromP, PointF toP){
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
