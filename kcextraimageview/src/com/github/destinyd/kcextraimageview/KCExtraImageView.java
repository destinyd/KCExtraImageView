package com.github.destinyd.kcextraimageview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import uk.co.senab.photoview.PhotoView;

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
}
