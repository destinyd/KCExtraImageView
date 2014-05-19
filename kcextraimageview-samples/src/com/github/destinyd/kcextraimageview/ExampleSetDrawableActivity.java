package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

public class ExampleSetDrawableActivity extends Activity {
    KCExtraImageView iv_image1, iv_image2, iv_image3, iv_image4;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_new);
        iv_image1 = (KCExtraImageView) findViewById(R.id.iv_image1);
        iv_image2 = (KCExtraImageView) findViewById(R.id.iv_image2);
        iv_image3 = (KCExtraImageView) findViewById(R.id.iv_image3);
        iv_image4 = (KCExtraImageView) findViewById(R.id.iv_image4);
        iv_image1.setImageResource(R.drawable.test);
        iv_image2.setImageResource(R.drawable.test1);
        iv_image3.setImageResource(R.drawable.test1);

        Drawable test1 = getResources().getDrawable( R.drawable.test );
        iv_image4.setImageDrawable(test1);

    }
}

