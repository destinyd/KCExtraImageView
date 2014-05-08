package com.github.destinyd.kcextraimageview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import com.github.destinyd.kcextraimageview.photoview.PhotoView;

/**
 * Created by dd on 14-5-6.
 */
public class KCExtraImageView extends PhotoView {
    private static final String TAG = "KCExtraImageView";
    private static final int MAX_TOUCHPOINTS = 10;
    private static final long FLOAT_TOUCH_TIME = 500; // 0.5s

    private final KCExtraImageViewAttacher mAttacher;
    private Drawable mDrawable;
    private boolean isShadowable = false;


//    private Rect mRect;
    private Paint mPaint;

    public KCExtraImageView(Context context) {
        this(context, null);
    }

    public KCExtraImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KCExtraImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAttacher = new KCExtraImageViewAttacher(this);

//        mRect = new Rect();
//        mPaint = new Paint();
//        mPaint.setAntiAlias(true);
//        mPaint.setShadowLayer(4f, 0.0f, 2.0f, Color.BLACK);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mDrawable = drawable;
    }

    public boolean isShadowable() {
        return isShadowable;
    }

    public void setShadowable(boolean isShadowable) {
        this.isShadowable = isShadowable;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

//    @Override
//    protected void onDraw(Canvas canvas)
//    {
//        isShadowable = true;
//        Log.e(TAG, "isShadowable:" + isShadowable);
//        if(isShadowable) {
////            Rect r = mRect;
////            Paint paint = mPaint;
////            canvas.drawRect(r, paint);
//
//            Bitmap bitmap = drawableToBitmap(getDrawable());
//            Paint mShadow = new Paint();
//// radius=10, y-offset=2, color=black
//            mShadow.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
//// in onDraw(Canvas)
//            canvas.drawBitmap(bitmap, 0.0f, 0.0f, mShadow);
//        }
//        super.onDraw(canvas);
//    }

    private static Bitmap getDropShadow3(Bitmap bitmap) {

        if (bitmap==null) return null;
        int think = 6;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int newW = w - (think);
        int newH = h - (think);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Bitmap sbmp = Bitmap.createScaledBitmap(bitmap, newW, newH, false);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Canvas c = new Canvas(bmp);

        // Right
        Shader rshader = new LinearGradient(newW, 0, w, 0, Color.GRAY, Color.LTGRAY, Shader.TileMode.CLAMP);
        paint.setShader(rshader);
        c.drawRect(newW, think, w, newH, paint);

        // Bottom
        Shader bshader = new LinearGradient(0, newH, 0, h, Color.GRAY, Color.LTGRAY, Shader.TileMode.CLAMP);
        paint.setShader(bshader);
        c.drawRect(think, newH, newW  , h, paint);

        //Corner
        Shader cchader = new LinearGradient(0, newH, 0, h, Color.LTGRAY, Color.LTGRAY, Shader.TileMode.CLAMP);
        paint.setShader(cchader);
        c.drawRect(newW, newH, w  , h, paint);


        c.drawBitmap(sbmp, 0, 0, null);

        return bmp;
    }
//
//    @Override
//    protected void onMeasure(int w, int h)
//    {
//        super.onMeasure(w,h);
//        isShadowable = true;
//        if(isShadowable) {
//            int mH, mW;
//            mW = getSuggestedMinimumWidth() < getMeasuredWidth() ? getMeasuredWidth() : getSuggestedMinimumWidth();
//            mH = getSuggestedMinimumHeight() < getMeasuredHeight() ? getMeasuredHeight() : getSuggestedMinimumHeight();
//            setMeasuredDimension(mW - 5, mH - 5);
//            Log.e(TAG, "mW - 5:" + (mW - 5));
//            Log.e(TAG, "mH - 5:" + (mH - 5));
//        }
//    }
}
