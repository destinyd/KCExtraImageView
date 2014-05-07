package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.FloatMath;
import android.util.Log;
import android.view.*;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

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

    public KCExtraImageViewAttacher(KCExtraImageView imageView) {
        super(imageView);
        this.imageView = imageView;
//        super.setZoomable(false);
//        super.setOnLongClickListener(this);
        super.setOnPhotoTapListener(this);
//        super.setOnViewTapListener(this);
    }

//    @Override
//    public boolean onLongClick(View v) {
//        Log.e(TAG, "onLongClick");
//        return false;
//    }

    @Override
    public void onPhotoTap(View view, float v, float v2) {
        Log.e(TAG, "onPhotoTap");//图事件
        open();
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

        wmParams.gravity = Gravity.LEFT | Gravity.TOP; // 调整悬浮窗口至左上角
//      wmParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT; // 调整悬浮窗口至左上角
        //设置默认显示位置
//      wmParams.x = 0;<span style="font-family: Arial, Helvetica, sans-serif;">// 以屏幕左上角为原点，设置x、y初始值</span>
//      wmParams.y = 0;
//        wmParams.x = windowSize.x;// 以屏幕右边， 距中
//                wmParams.y = windowSize.y / 2;

        wm.addView(mOpenView, wmParams);
//        getRoot().addView(mOpenView, layoutParams);
    }

    private void close_full_screen() {
//        resume_all_view();
        close_open_image_view();
    }

    private void close_open_image_view() {
        WindowManager wm = (WindowManager) imageView.getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        wm.removeView(mOpenView);
    }

    private void hide_all_view() {
        if(null !=getRoot()) {
            for(int i=0; i < getRoot().getChildCount(); i++)
            {
                View view = getRoot().getChildAt(i);
                arrayList.add(view.getVisibility());
                view.setVisibility(View.INVISIBLE);
            }
        }
    }

    private ViewGroup getRoot(){
        if(null != mRoot)
            return mRoot;
        View view = getImageView();
        if(null != view) {
            while (null != view.getParent() && !view.getParent().getClass().getName().equals("android.view.ViewRootImpl")) {
                view = (View) view.getParent();
            }
            mRoot = (ViewGroup)view;
            return mRoot;
        }
        return null;
    }

//    @Override
//    public void onViewTap(View view, float v, float v2) {
//        Log.e(TAG, "onViewTap");
//    }



//    float x_down = 0;
//    float y_down = 0;
//    PointF start = new PointF();
//    PointF mid = new PointF();
//    float oldDist = 1f;
//    float oldRotation = 0;
//    Matrix matrix = new Matrix();
//    Matrix matrix1 = new Matrix();
//    Matrix savedMatrix = new Matrix();
//
//    private static final int NONE = 0;
//    private static final int DRAG = 1;
//    private static final int ZOOM = 2;
//    int mode = NONE;
//
//    boolean matrixCheck = false;
//
//    int widthScreen;
//    int heightScreen;
//
//    Bitmap gintama;
//
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction() & MotionEvent.ACTION_MASK) {
//            case MotionEvent.ACTION_DOWN:
//                mode = DRAG;
//                x_down = event.getX();
//                y_down = event.getY();
//                savedMatrix.set(matrix);
//                break;
//            case MotionEvent.ACTION_POINTER_DOWN:
//                mode = ZOOM;
//                oldDist = spacing(event);
//                oldRotation = rotation(event);
//                savedMatrix.set(matrix);
//                midPoint(mid, event);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                if (mode == ZOOM) {
//                    matrix1.set(savedMatrix);
//                    float rotation = rotation(event) - oldRotation;
//                    float newDist = spacing(event);
//                    float scale = newDist / oldDist;
//                    matrix1.postScale(scale, scale, mid.x, mid.y);// 縮放
//                    matrix1.postRotate(rotation, mid.x, mid.y);// 旋轉
//                    matrixCheck = matrixCheck();
//                    if (matrixCheck == false) {
//                        matrix.set(matrix1);
//                        invalidate();
//                    }
//                } else if (mode == DRAG) {
//                    matrix1.set(savedMatrix);
//                    matrix1.postTranslate(event.getX() - x_down, event.getY()
//                            - y_down);// 平移
//                    matrixCheck = matrixCheck();
//                    matrixCheck = matrixCheck();
//                    if (matrixCheck == false) {
//                        matrix.set(matrix1);
//                        invalidate();
//                    }
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_POINTER_UP:
//                mode = NONE;
//                break;
//        }
//        return true;
//    }
//
//
//    private boolean matrixCheck() {
//        float[] f = new float[9];
//        matrix1.getValues(f);
//        // 图片4个顶点的坐标
//        float x1 = f[0] * 0 + f[1] * 0 + f[2];
//        float y1 = f[3] * 0 + f[4] * 0 + f[5];
//        float x2 = f[0] * gintama.getWidth() + f[1] * 0 + f[2];
//        float y2 = f[3] * gintama.getWidth() + f[4] * 0 + f[5];
//        float x3 = f[0] * 0 + f[1] * gintama.getHeight() + f[2];
//        float y3 = f[3] * 0 + f[4] * gintama.getHeight() + f[5];
//        float x4 = f[0] * gintama.getWidth() + f[1] * gintama.getHeight() + f[2];
//        float y4 = f[3] * gintama.getWidth() + f[4] * gintama.getHeight() + f[5];
//        // 图片现宽度
//        double width = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
//        // 缩放比率判断
//        if (width < widthScreen / 3 || width > widthScreen * 3) {
//            return true;
//        }
//        // 出界判断
//        if ((x1 < widthScreen / 3 && x2 < widthScreen / 3
//                && x3 < widthScreen / 3 && x4 < widthScreen / 3)
//                || (x1 > widthScreen * 2 / 3 && x2 > widthScreen * 2 / 3
//                && x3 > widthScreen * 2 / 3 && x4 > widthScreen * 2 / 3)
//                || (y1 < heightScreen / 3 && y2 < heightScreen / 3
//                && y3 < heightScreen / 3 && y4 < heightScreen / 3)
//                || (y1 > heightScreen * 2 / 3 && y2 > heightScreen * 2 / 3
//                && y3 > heightScreen * 2 / 3 && y4 > heightScreen * 2 / 3)) {
//            return true;
//        }
//        return false;
//    }

//
//    @Override
//    public boolean onTouch(View v, MotionEvent ev) {
//        boolean handled = false;
//
//        if (mZoomEnabled && hasDrawable((ImageView) v)) {
//            ViewParent parent = v.getParent();
//            switch (ev.getAction()) {
//                case ACTION_DOWN:
//                    // First, disable the Parent from intercepting the touch
//                    // event
//                    if (null != parent)
//                        parent.requestDisallowInterceptTouchEvent(true);
//                    else
//                        Log.i(LOG_TAG, "onTouch getParent() returned null");
//
//                    // If we're flinging, and the user presses down, cancel
//                    // fling
//                    cancelFling();
//                    break;
//
//                case ACTION_CANCEL:
//                case ACTION_UP:
//                    // If the user has zoomed less than min scale, zoom back
//                    // to min scale
//                    if (getScale() < mMinScale) {
//                        RectF rect = getDisplayRect();
//                        if (null != rect) {
//                            v.post(new AnimatedZoomRunnable(getScale(), mMinScale,
//                                    rect.centerX(), rect.centerY()));
//                            handled = true;
//                        }
//                    }
//                    break;
//            }
//
//            // Try the Scale/Drag detector
//            if (null != mScaleDragDetector
//                    && mScaleDragDetector.onTouchEvent(ev)) {
//                handled = true;
//            }
//
//            // Check to see if the user double tapped
//            if (null != mGestureDetector && mGestureDetector.onTouchEvent(ev)) {
//                handled = true;
//            }
//        }
//
//        return handled;
//    }

    // 触碰两点间距离
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    // 取手势中心点
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // 取旋转角度
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }


//    private final class TouchListener implements View.OnTouchListener {
//        private PointF startPoint = new PointF();
//        private Matrix matrix = new Matrix();
//        private Matrix currentMatrix = new Matrix();
//        private int mode = 0;
//        private static final int DRAG = 1;
//        private static final int ZOOM = 2;
//        private float startDis;// 开始距离
//        private PointF midPoint;// 中间点
//        private double startAngle;// 开始角度
//
//        public boolean onTouch(View v, MotionEvent event) {
//            switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                case MotionEvent.ACTION_DOWN:// 手指压下屏幕
//                    Log.e("onTouch", "ACTION_DOWN");
//                    mode = DRAG;
//                    currentMatrix.set(getImageView().getImageMatrix());// 记录ImageView当前的移动位置
//                    startPoint.set(event.getX(), event.getY());
//                    break;
//
//                case MotionEvent.ACTION_MOVE:// 手指在屏幕移动，该 事件会不断地触发
//                    // Log.e("onTouch", "ACTION_MOVE");
//                    if (mode == DRAG) {
//                        float dx = event.getX() - startPoint.x;// 得到在x轴的移动距离
//                        float dy = event.getY() - startPoint.y;// 得到在y轴的移动距离
//                        matrix.set(currentMatrix);// 在没有进行移动之前的位置基础上进行移动
//                        matrix.postTranslate(dx, dy);
//                    } else if (mode == ZOOM) {// 缩放
//                        float endDis = distance(event);// 结束距离
//                        int trunAngel = (int) (angle(event) - startAngle);// 变化的角度
//                        // Log.v("ACTION_MOVE", "trunAngel="+trunAngel);
//                        if (endDis > 10f) {
//                            float scale = endDis / startDis;// 得到缩放倍数
//                            matrix.set(currentMatrix);
//                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
//                            Log.v("ACTION_MOVE", "imageView.getHeight()="
//                                    + getImageView().getHeight());
//                            Log.v("ACTION_MOVE", "imageView.getWidth()="
//                                    + getImageView().getWidth());
//                            if (Math.abs(trunAngel) > 5) {
//                                // 设置变化的角度
//                                matrix.postRotate(trunAngel, midPoint.x, midPoint.y);
//                            }
//
//                        }
//                    }
//                    break;
//
//                case MotionEvent.ACTION_UP:// 手指离开屏
//                    // Log.e("onTouch", "ACTION_UP");
//                    break;
//                case MotionEvent.ACTION_POINTER_UP:// 有手指离开屏幕,但屏幕还有触点（手指）
//                    // Log.e("onTouch", "ACTION_POINTER_UP");
//                    mode = 0;
//                    break;
//
//                case MotionEvent.ACTION_POINTER_DOWN:// 当屏幕上还有触点（手指），再有一个手指压下屏幕
//                    // Log.e("onTouch", "ACTION_POINTER_DOWN");
//                    mode = ZOOM;
//                    startDis = distance(event);
//                    startAngle = angle(event);
//                    if (startDis > 10f) {
//                        midPoint = mid(event);
//                        currentMatrix.set(getImageView().getImageMatrix());// 记录ImageView当前的缩放倍数
//                    }
//                    break;
//            }
//            // Bitmap
//            // bitmap0=((BitmapDrawable)getResources().getDrawable(R.drawable.test2)).getBitmap();
//            // LayerDrawable layerDrawable=LayerDrawable.
//            // Bitmap bitmap=Bitmap.createBitmap(bitmap0, x, y, width, height,
//            // m, filter)
//            getImageView().setImageMatrix(matrix);
//            return true;
//        }
//
//    }
//
//    /**
//     * 计算两点之间的距离
//     *
//     * @param event
//     * @return
//     */
//    public static float distance(MotionEvent event) {
//        float dx = event.getX(1) - event.getX(0);
//        float dy = event.getY(1) - event.getY(0);
//        return FloatMath.sqrt(dx * dx + dy * dy);
//    }
//
//    /**
//     * 计算两点之间的中间点
//     *
//     * @param event
//     * @return
//     */
//    public static PointF mid(MotionEvent event) {
//        float midX = (event.getX(1) + event.getX(0)) / 2;
//        float midY = (event.getY(1) + event.getY(0)) / 2;
//        return new PointF(midX, midY);
//    }
//
//    /**
//     * 计算两个手指连线与坐标轴的角度（单位为。C）
//     *
//     * @param event
//     * @return
//     */
//    public static double angle(MotionEvent event) {
//        double delta_x = (event.getX(0) - event.getX(1));
//        double delta_y = (event.getY(0) - event.getY(1));
//        double radians = Math.atan2(delta_y, delta_x);
//        return (float) Math.toDegrees(radians);
//    }
}
