package com.github.destinyd.kcextraimageview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ExampleListActivity extends Activity implements View.OnClickListener {

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_list);
        setMenuBtnsClick();
    }


    private void setMenuBtnsClick() {
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);
        Button button3 = (Button) findViewById(R.id.button3);
        Button button4 = (Button) findViewById(R.id.button4);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                startActivity(new Intent(this, ExampleSimpleActivity.class));
                break;
            case R.id.button2:
                startActivity(new Intent(this, ExampleMultipleActivity.class));
                break;
            case R.id.button3:
                startActivity(new Intent(this, ExampleImagesActivity.class));
                break;
            case R.id.button4:
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                startActivity(new Intent(this, ExampleImagesActivity.class));
                break;
        }
    }
}

