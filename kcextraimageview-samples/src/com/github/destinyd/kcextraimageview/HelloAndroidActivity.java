package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class HelloAndroidActivity extends Activity {

    private static final String TAG = "HelloAndroidActivity";
    ImageView iv_image;
    KCExtraImageViewAttacher mAttacher;

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_image = (ImageView) findViewById(R.id.iv_image);

//        Drawable bitmap = getResources().getDrawable(R.drawable.test);
//        iv_image.setImageDrawable(bitmap);
//
//        // Attach a PhotoViewAttacher, which takes care of all of the zooming functionality.
//        mAttacher = new KCExtraImageViewAttacher(iv_image);

    }
}

