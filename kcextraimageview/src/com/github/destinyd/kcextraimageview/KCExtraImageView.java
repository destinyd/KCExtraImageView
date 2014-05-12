package com.github.destinyd.kcextraimageview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
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

    public KCExtraImageView(Context context) {
        this(context, null);
    }

    public KCExtraImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KCExtraImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAttacher = new KCExtraImageViewAttacher(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(isShadowable) {
//            if (superOnDraw(canvas)) return; // couldn't resolve the URI
            drawShadow(canvas);
            super.onDraw(canvas);
        }
        else{
            super.onDraw(canvas);
        }
    }

    private Matrix mMatrix;
    private Paint mPaint;
//    static final int SHADOW_PADDING_TOP = -10;
    static final float SHADOW_PADDING_HOR_PERCENT = 0.2f;
    static final int SHADOW_SIZE = 50;
    private void drawShadow(Canvas canvas) {

        RectF rectF = mAttacher.getDisplayRect();
        Rect rect = new Rect();
        rect.top = (int)(rectF.top + rectF.bottom / 2);// + SHADOW_PADDING_TOP;
        rect.left = (int) (rectF.left);// + rectF.width() * SHADOW_PADDING_HOR_PERCENT);
        rect.bottom = (int)rectF.bottom + SHADOW_SIZE;
        rect.right = (int)(rectF.right);// - rectF.width() * SHADOW_PADDING_HOR_PERCENT);

        Paint mShadow = new Paint();
        mShadow.setColor(getResources().getColor(android.R.color.transparent));
        mShadow.setShadowLayer(10.0f, 5f, 2f, 0x11000000);
        canvas.drawRect(rect, mShadow);

    }
}