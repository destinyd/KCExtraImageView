package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.os.Bundle;

public class ExampleImagesActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_images);
    }

    @Override
    protected void onResume() {
        super.onResume();
        KCTopestHookLayer topestHookLayer = KCTopestHookLayer.init(this);
        topestHookLayer.addHookView(findViewById(R.id.iv_image1));
        topestHookLayer.addHookView(findViewById(R.id.iv_image2));
        topestHookLayer.addHookView(findViewById(R.id.iv_image3));
        topestHookLayer.addHookView(findViewById(R.id.iv_image4));
    }

    @Override
    protected void onPause() {
        KCTopestHookLayer.clear(this);
        super.onPause();
    }
}

