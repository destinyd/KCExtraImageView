package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class ExampleMultipleActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_multiple);
    }

    @Override
    protected void onResume() {
        super.onResume();
        KCTopestHookLayer topestHookLayer = KCTopestHookLayer.init(this);
        topestHookLayer.addHookView((KCExtraImageView) findViewById(R.id.iv_image));
    }

    @Override
    protected void onPause() {
        KCTopestHookLayer.clear(this);
        super.onPause();
    }
}

