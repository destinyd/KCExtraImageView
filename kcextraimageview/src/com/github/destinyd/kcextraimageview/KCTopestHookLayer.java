package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by dd on 14-5-20.
 */
public class KCTopestHookLayer extends FrameLayout {
    private static final String TAG = "KCTopestHookLayer";
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

    public void work() {
        working = true;
    }

    public void hook(KCExtraImageView view){
        hookView = view;
    }

    public void unhook() {
        hookView = null;
    }

    ArrayList<View> views = new ArrayList<View>();
    KCExtraImageView hookView = null;
    Activity activity;

    public void addHookView(KCExtraImageView view) {
        views.add(view);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.e(TAG, "onInterceptTouchEvent");
        if (working) {
            Iterator<View> iterator = views.iterator();
            while (iterator.hasNext()) {
                View view = iterator.next();
                if (inViewBounds(view, (int) ev.getX(), (int) ev.getY())) {
                    hookView = (KCExtraImageView) view;
                    Log.e(TAG, "onInterceptTouchEvent true");
                    return true;
                }
            }
            hookView = null;
//            return false;
        }

        return super.onInterceptTouchEvent(ev);
//        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (working) {
            if(hookView != null) {
                Log.e(TAG, "hookView not null");
                hookView.dispatchTouchEvent(event);
                return true;
            }
            Iterator<View> iterator = views.iterator();
            while (iterator.hasNext()) {
                View view = iterator.next();
                if (inViewBounds(view, (int) event.getX(), (int) event.getY())) {
                    boolean result = view.dispatchTouchEvent(event);
                    if(result) {
                        Log.e(TAG, "find a hookView");
                        hookView = (KCExtraImageView) view;
                        return true;
                    }
                }
            }
//            hookView = null;
//            return false;
        }

        boolean b = activity.dispatchTouchEvent(event);
        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e(TAG, "onTouchEvent");
        if (working)
        {
            if(hookView != null) {
                return hookView.onTouch(hookView, event);
            }
//            return false;
        }

        boolean b = activity.onTouchEvent(event);
        Log.e(TAG, "onTouchEvent:" + b);
        return b;
    }

    Rect outRect = new Rect();
    int[] location = new int[2];

    public boolean inViewBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    public Activity getActivity() {
        return activity;
    }

    static WindowManager _windowManager = null;
    static KCTopestHookLayer _factory = null;

    static public KCTopestHookLayer getFactory(Context context) {
        if (_factory == null)
            _factory = new KCTopestHookLayer(context);
        return _factory;
    }

    static public void clear(Context context) {
        if(_factory != null) {
            getWindowManager(context).removeView(_factory);
            _factory = null;
        }
    }
//
//    static public KCTopestHookLayer init(View view){
//        return init(view.getContext());
//    }

    static public KCTopestHookLayer init(Context context){
        clear(context);
        KCTopestHookLayer topestHookLayer = getFactory(context);
        getWindowManager(context).addView(topestHookLayer, getHookParams(context));
        Activity activity = (Activity) context;
        topestHookLayer.setActivity(activity);
        topestHookLayer.work();
        return topestHookLayer;
    }

    static public KCTopestHookLayer initOnce(Context context){
        KCTopestHookLayer topestHookLayer = getFactory(context);
        if(topestHookLayer.getActivity() == null) {
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

//    static public int get_statusbar_height(Context context) {
//        Class c;
//        try {
//            c =
//                    Class.forName("com.android.internal.R$dimen");
//            Object obj = c.newInstance();
//            Field field = c.getField("status_bar_height");
//            int x = Integer.parseInt(field.get(obj).toString());
//            int y = context.getResources().getDimensionPixelSize(x);
//            return y;
//        } catch (Exception e) {
//        }
//        return 0;
//
//    }
//
//    static public int get_actionbar_height(Context context) {
//        TypedValue tv = new TypedValue();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
//                return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
//        }
//        return 0;
//    }
}
