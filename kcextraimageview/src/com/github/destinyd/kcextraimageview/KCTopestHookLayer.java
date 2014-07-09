package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Iterator;

import static com.github.destinyd.kcextraimageview.KCExtraImageView.get_actionbar_height;
import static com.github.destinyd.kcextraimageview.KCExtraImageView.get_statusbar_height;

/**
 * Created by dd on 14-5-20.
 */
public class KCTopestHookLayer extends FrameLayout {
    private static final String TAG = "KCTopestHookLayer";
    static WindowManager _windowManager = null;
    static KCTopestHookLayer _factory = null;
    ArrayList<View> views = new ArrayList<View>();
    View hookView = null;
    Activity activity;
    Rect outRect = new Rect();
    int[] location = new int[2];
    private boolean working = false;

    public KCTopestHookLayer(Context context) {
        this(context, null);
    }

    public KCTopestHookLayer(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public KCTopestHookLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    static public KCTopestHookLayer getFactory(Context context) {
        if (_factory == null)
            _factory = new KCTopestHookLayer(context);
        return _factory;
    }

    static public void clear(Context context) {
        try {
            getWindowManager(context).removeView(_factory);
            _factory = null;
        } catch (Exception ex) {
            _factory = null;
        }
    }

    static public KCTopestHookLayer init(Context context) {
        clear(context);
        KCTopestHookLayer topestHookLayer = getFactory(context);
        getWindowManager(context).addView(topestHookLayer, getHookParams(context));
        Activity activity = (Activity) context;
        topestHookLayer.setActivity(activity);
        topestHookLayer.work();
        return topestHookLayer;
    }

    static public KCTopestHookLayer initOnce(Context context) {
        KCTopestHookLayer topestHookLayer = getFactory(context);
        if (topestHookLayer.getActivity() == null) {
//            topestHookLayer.setBackgroundColor(Color.RED);
            getWindowManager(context).addView(topestHookLayer, getHookParams(context));
            topestHookLayer.work();
        }
        Activity activity = (Activity) context;
        topestHookLayer.setActivity(activity);
        return topestHookLayer;
    }

    static public WindowManager getWindowManager(Context context) {
        if (_windowManager == null)
            _windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        return _windowManager;
    }

    static public WindowManager.LayoutParams getHookParams(Context context) {
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        wmParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置Window flag

        wmParams.gravity = Gravity.TOP | Gravity.LEFT;

        wmParams.flags =
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
////                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
//                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
////                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
////                | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
//        ; // 可以截获了
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        ;

        return wmParams;
    }

    public void work() {
        working = true;
    }

    public void hook(KCExtraImageView view) {
        hookView = view;
    }

    public void unhook() {
        hookView = null;
    }

    public void addHookView(View view) {
        views.add(view);
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        event.offsetLocation(0.0f, get_statusbar_height(getContext()));
        if (working) {
            if (hookView != null) {
                hookView.dispatchTouchEvent(event);
                return true;
            }
            Iterator<View> iterator = views.iterator();
            while (iterator.hasNext()) {
                View view = iterator.next();
                if (isViewContains(
                        view,
                        (int) event.getX(),
                        (int) event.getY())) {
                    boolean result = view.dispatchTouchEvent(event);
                    if (result) {
                        hookView = (KCExtraImageView) view;
                        return true;
                    }
                }
            }
        }

        boolean b = activity.dispatchTouchEvent(event);
        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (working) {
            if (hookView != null) {
                return hookView.onTouchEvent(event);
            }
//            return false;
        }

        boolean b = activity.onTouchEvent(event);
        Log.d(TAG, "onTouchEvent:" + b);
        return b;
    }

    private boolean isViewContains(View view, int rx, int ry) {
        int[] l = new int[2];
        view.getLocationOnScreen(l);
        int x = l[0];
        int y = l[1];
        int w = view.getWidth();
        int h = view.getHeight();

        if (rx < x || rx > x + w || ry < y || ry > y + h) {
            return false;
        }
        return true;
    }
//
//    private boolean testViewContains(View v, int rx, int ry) {
//        Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
//        return rect.contains(v.getLeft() + (int) rx, v.getTop() + (int) ry);
//    }
}
