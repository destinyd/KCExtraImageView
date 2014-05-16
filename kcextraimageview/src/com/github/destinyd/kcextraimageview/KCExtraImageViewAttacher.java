package com.github.destinyd.kcextraimageview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.FloatMath;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.github.destinyd.kcextraimageview.photoview.Compat;
import com.github.destinyd.kcextraimageview.photoview.PhotoView;
import com.github.destinyd.kcextraimageview.photoview.PhotoViewAttacher;

import java.util.ArrayList;

/**
 * Created by dd on 14-5-6.
 */
public class KCExtraImageViewAttacher extends PhotoViewAttacher implements PhotoViewAttacher.OnPhotoTapListener {

    private static final String TAG = "KCExtraImageViewAttacher";
    private KCExtraImageView imageView;
    private ViewGroup mRoot = null;
    private ArrayList<Integer> arrayList = new ArrayList<Integer>();
    private PhotoView mOpenView = null;

    private int mState = 0;
    static final int STATE_NORMAL = 0;
    static final int STATE_LONG_CLICK = 1;
    static final int STATE_OPEN = 2;
    WindowManager windowManager;
    ViewGroup.LayoutParams layoutParamsImageView;
    ViewGroup viewParent;

    public KCExtraImageViewAttacher(KCExtraImageView imageView) {
        super(imageView);
        this.imageView = imageView;
//        super.setZoomable(false);
//        super.setOnLongClickListener(this);
        super.setOnPhotoTapListener(this);
//        imageView.setOnTouchListener(new TouchListener());
//        super.setOnViewTapListener(this);

        windowManager = (WindowManager) getImageView().getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        layoutParamsImageView = imageView.getLayoutParams();
        viewParent = (ViewGroup) imageView.getParent();
    }

    @Override
    public void onPhotoTap(View view, float v, float v2) {
        Log.e(TAG, "onPhotoTap");//图事件
        open();
//        setRo
    }

    private void open() {
        mState = STATE_OPEN;
        open_full_screen();
    }

    private void open_full_screen() {
//        hide_all_view();
        create_open_image_view();
    }

    private void create_open_image_view() {
        mOpenView = new PhotoView(imageView.getContext());
        mOpenView.setBackgroundColor(Color.BLACK);
        mOpenView.setImageDrawable(imageView.getDrawable());
        mOpenView.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                close_full_screen();
            }
        });


//        Point windowSize = new Point();
//        ((Activity)getImageView().getContext()).getWindowManager().getDefaultDisplay().getSize(windowSize);

        // 获取WindowManager
        WindowManager wm = (WindowManager) getImageView().getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
//        // 设置LayoutParams(全局变量）相关参数
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
//        wmParams.type =  WindowManager.LayoutParams.TYPE_PHONE; // 设置window type
        wmParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置Window flag
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

         /*
         * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
         * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
         * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
         */

        wmParams.gravity = Gravity.CENTER;
//      wmParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT; // 调整悬浮窗口至左上角
        //设置默认显示位置
//      wmParams.x = 0;<span style="font-family: Arial, Helvetica, sans-serif;">// 以屏幕左上角为原点，设置x、y初始值</span>
//      wmParams.y = 0;
//        wmParams.x = windowSize.x;// 以屏幕右边， 距中
//                wmParams.y = windowSize.y / 2;

        wm.addView(mOpenView, wmParams);
//        getRoot().addView(mOpenView, layoutParams);
    }

    public void close_full_screen() {
//        resume_all_view();
        close_open_image_view();
        mState = STATE_NORMAL;
    }

    private void close_open_image_view() {
        if(mOpenView != null) {
            windowManager.removeView(mOpenView);
            mOpenView = null;
        }
    }

    private void hide_all_view() {
        if (null != getRoot()) {
            for (int i = 0; i < getRoot().getChildCount(); i++) {
                View view = getRoot().getChildAt(i);
                arrayList.add(view.getVisibility());
                view.setVisibility(View.INVISIBLE);
            }
        }
    }

    private ViewGroup getRoot() {
        if (null != mRoot)
            return mRoot;
        View view = getImageView();
        if (null != view) {
            while (null != view.getParent() && isNotAboveRoot(view)) {
                view = (View) view.getParent();
            }
            mRoot = (ViewGroup) view;
            return mRoot;
        }
        return null;
    }

    private boolean isNotAboveRoot(View view) {
        String strClass = view.getParent().getClass().getName();
        return !strClass.equals("android.view.ViewRootImpl") && !strClass.equals("android.view.ViewRoot");
    }

    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;

    protected PointF startPoint = new PointF();
    private int mode = 0;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private float startDis;// 开始距离
    private PointF midPoint;// 中间点
    private double startAngle;// 开始角度
    private double currentAngle;// 开始角度
    private long FLOAT_TIME = 500; // 0.5s

    private MotionEvent lastEvent;

    public MotionEvent getLastEvent() {
        return lastEvent;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean handled = false;
        lastEvent = event;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:// 手指压下屏幕
                Log.e(TAG, "normal ACTION_DOWN");
                startPoint.set(event.getX(), event.getY());
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:// 手指在屏幕移动，该 事件会不断地触发
//                Log.e(TAG, "ACTION_MOVE");
//                    Log.e(TAG, "event.getEventTime() - event.getDownTime() : " + (event.getEventTime() - event.getDownTime()));
                if (mode == NONE) {
                    if (event.getEventTime() - event.getDownTime() >= FLOAT_TIME) {
                        Log.e(TAG, "normal to DRAG");
                        mode = DRAG;
                        to_window();
                        imageView.setZoomable(true);
                    }
                }
                // Log.e("onTouch", "ACTION_MOVE");
                if (mode == DRAG) {
                    return super.onTouch(v, event);
                } else if (mode == ZOOM) {// 缩放
                    if (event.getPointerCount() >= 2) {
                        float endDis = distance(event);// 结束距离
                        currentAngle = angle(event);
                        int turnAngel = (int) (currentAngle - startAngle);// 变化的角度
//                        if(imageView.imageViewFrom != null)
//                            Log.v(TAG, "imageViewFrom turnAngel=" + turnAngel);
//                        else
//                            Log.e(TAG, "null turnAngel=" + turnAngel);
                        if (endDis > 10f) {
                            float scale = endDis / startDis;// 得到缩放倍数
//
                            //放大
                            setScale(scale, midPoint.x, midPoint.y, true);
//                        Log.v("ACTION_MOVE", "imageView.getHeight()="
//                                + getImageView().getHeight());
//                        Log.v("ACTION_MOVE", "imageView.getWidth()="
//                                + getImageView().getWidth());
                            if (Math.abs(turnAngel) > 3) {

//                                Log.e(TAG, "currentAngle:" + currentAngle);
//                                Log.e(TAG, "turnAngel:" + turnAngel);
//                                Log.e(TAG, "currentAngle + turnAngel:" + currentAngle + turnAngel);
                                // 设置变化的角度
//                                setRotationTo(turnAngel);
                                setPhotoViewRotation(turnAngel, false);
//                            update();
                                // 设置变化的角度
//                            matrix.postRotate(turnAngel, midPoint.x, midPoint.y);
                            }

                        }
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                Log.e("onTouch", "ACTION_CANCEL");
//                remove、invisiable触发
//                int action = event.getAction();
//                event.setAction( action &= ~ACTION_CANCEL);
//                return true;
                break;
            case MotionEvent.ACTION_UP:// 手指离开屏
                Log.e("onTouch", "ACTION_UP");
                mode = NONE;
                from_window();
//                remove_from_window();


                // If the user has zoomed less than min scale, zoom back
                // to min scale
                if (getScale() < mMinScale) {
                    RectF rect = getDisplayRect();
                    if (null != rect) {
                        v.post(new AnimatedZoomRunnable(getScale(), mMinScale,
                                rect.centerX(), rect.centerY()));
                        handled = true;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:// 有手指离开屏幕,但屏幕还有触点（手指）
                Log.e(TAG, "ACTION_POINTER_UP");
//                Log.e(TAG, "event.getPointerCount():" + event.getPointerCount());
                if (event.getPointerCount() == 2) {
                    Log.e(TAG, "onTouch to DRAG");
                    mode = DRAG;
                } else if (event.getPointerCount() <= 1) {
                    Log.e(TAG, "onTouch to NONE");
                    mode = NONE;
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:// 当屏幕上还有触点（手指），再有一个手指压下屏幕
                Log.e(TAG, "onTouch ACTION_POINTER_DOWN");
                if (mode != ZOOM && event.getEventTime() - event.getDownTime() >= FLOAT_TIME) {
                    Log.e(TAG, "onTouch to ZOOM");
                    mode = ZOOM;
                    startDis = distance(event);
                    startAngle = angle(event);
                    if (startDis > 10f) {
                        midPoint = mid(event);
                    }
                }
                break;

            case MotionEvent.ACTION_OUTSIDE:
                Log.e(TAG, "ACTION_OUTSIDE");
                break;

            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_HOVER_ENTER:
                Log.e(TAG, "ACTION_HOVER");
                break;

            case MotionEvent.ACTION_MASK:
            case MotionEvent.ACTION_POINTER_INDEX_MASK:
            case MotionEvent.ACTION_SCROLL:
                Log.e(TAG, "ACTION_OTHER");
                break;
        }


        // Try the Scale/Drag detector
        if (null != mScaleDragDetector
                && mScaleDragDetector.onTouchEvent(event)) {
            handled = true;
        }

        // Check to see if the user double tapped
        if (null != mGestureDetector && mGestureDetector.onTouchEvent(event)) {
            handled = true;
        }

        return true;
    }

    private void from_window() {
        anime_to_original();
        //必须动画走完再设置回原始，否则会出直接变当前比例原始图BUG
//        to_original_layout_params();
    }

    private void anime_to_original() {
//        imageView.setScale(1, true);
//        Log.e(TAG, "currentAngle :" + currentAngle);
        setPhotoViewRotation(-(float) currentAngle, true);
        setScale(1, true);
        //怎么回0度。。
//        if (currentAngle <= 180)
//            setRotation(-(float) currentAngle, true);
//        else {
//            setRotation((float) (360 - currentAngle), true);
//        }
    }

    private void to_original_layout_params() {
        if(layoutParamsImageView != null) {
            imageView.setLayoutParams(layoutParamsImageView);
        }
    }

    private void remove_from_window() {
        windowManager.removeView(imageView);
        viewParent.addView(imageView, layoutParamsImageView);
    }

    KCExtraImageView topImageView;

    private void to_window() {
        Log.e(TAG, "to_window");
        mState = STATE_LONG_CLICK;
        imageView.setShadowable(true);
        set_this_view_to_topest();


        String strClass = imageView.getParent().getClass().getName();
        if (strClass.equals("android.widget.RelativeLayout")) {
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        } else if (strClass.equals("android.widget.LinearLayout")) {
            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        } else if (strClass.equals("android.widget.FrameLayout")) {
            imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

//        getRoot().bringChildToFront(imageView);
//        topImageView = new KCExtraImageView(imageView.getContext());
//        topImageView.setImageDrawable(imageView.getDrawable());
//        topImageView.setImageViewFrom(imageView);
//        imageView.setImageViewTo(topImageView);
////        imageView.setOnTouchListener(new TopOnTouchListener());
//        topImageView.setOnTouchListener(new TopOnTouchListener());
//        imageView.setVisibility(View.INVISIBLE);
//        getRoot().addView(topImageView, getRoot().getChildCount(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
////        getRoot().setOnDragListener(new OnDragListener());
////        ClipData data = ClipData.newPlainText("", "");
////        MyDragShadowBuilder shadowBuilder = new MyDragShadowBuilder(imageView);
////        imageView.startDrag(data, shadowBuilder, null, 0);
////        imageView.setVisibility(View.INVISIBLE);
    }

    private void set_this_view_to_topest() {
        ViewGroup viewGroup = (ViewGroup) imageView.getParent();
        View obj = imageView;

        while (viewGroup != null && isNotAboveRoot(viewGroup)) {
            viewGroup.bringChildToFront(obj);
            obj = viewGroup;
            viewGroup = (ViewGroup) viewGroup.getParent();
        }
    }

//    }

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public class MyDragShadowBuilder extends View.DragShadowBuilder {
        public static final float SHADOW_BOTTOM_SIZE = 10f;
        public static final float SHADOW_RIGHT_SIZE = 5f;
        KCExtraImageView mView;
        private final Drawable shadow;
        private int width, height;
        final int paddingLeft, paddingTop;

        public MyDragShadowBuilder(View view) {
            super(view);
            shadow = new ColorDrawable(Color.parseColor("#ffff0000"));
            mView = (KCExtraImageView) view;
            paddingLeft = mView.getPaddingLeft();
            paddingTop = mView.getPaddingTop();
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            int width = getView().getWidth();
            int height = getView().getHeight();

            shadowSize.set(width + 5, height + 5);

            shadowTouchPoint.set(width / 2, height / 2);
//            super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            // Set Drag image background or anything you want
//            int width = getView().getWidth();
//            int height = getView().getHeight();
            Paint paint = new Paint();
            paint.setColor(0x55858585);
//            Matrix matrix1 = imageView.getMatrix();
//            float[] values = new float[256];
//            matrix1.getValues(values);
            RectF rect = mView.getDisplayRect();
            rect.top += paddingTop + SHADOW_BOTTOM_SIZE;
            rect.left += paddingLeft + SHADOW_RIGHT_SIZE;
            rect.bottom += paddingTop + SHADOW_BOTTOM_SIZE;
            rect.right += paddingLeft + SHADOW_RIGHT_SIZE;

            canvas.drawRect(rect, paint);

            super.onDrawShadow(canvas);
        }
    }


    //    float currentAngle = 0;
//
//    /**
//     * @deprecated use {@link #setRotationTo(float)}
//     */
    @Override
    public void setPhotoViewRotation(float degrees) {
        setPhotoViewRotation(degrees, false);
    }

//    @Override
//    public void setRotationTo(float degrees) {
//        super.setRotationTo(degrees);
//        currentAngle = degrees;
//    }
//
//    @Override
//    public void setRotationBy(float degrees) {
//        super.setRotationBy(degrees);
//        currentAngle = degrees;
//    }

    AnimatedRotationRunnable runnableRotation = null;

    public void setPhotoViewRotation(float degrees, boolean animate) {
        float targetAngle = degrees % 360;
        if (animate) {
            if (runnableRotation != null)
                runnableRotation.stop();
            runnableRotation = new AnimatedRotationRunnable(targetAngle);
            imageView.post(runnableRotation);
        } else {
            mSuppMatrix.setRotate(targetAngle);
            checkAndDisplayMatrix();
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
                if (imageView == null) {
                    return;
                }

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
//                checkAndDisplayMatrix();
                setImageViewMatrix(getDrawMatrix()); // not check
                fromDegrees = t * degrees;

                // We haven't hit our target scale yet, so post ourselves again
                if (t < 1f && running) {
//                    Log.e(TAG, "totalDegrees:" + totalDegrees);
                    Compat.postOnAnimation(imageView, this);
                } else {
                    if (mode == NONE) {
                        to_original_layout_params();
                        imageView.setShadowable(false);
                        update(); // 直接恢复，不太平滑
                    }
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
}
