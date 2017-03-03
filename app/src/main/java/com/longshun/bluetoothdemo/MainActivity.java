package com.longshun.bluetoothdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


/*蓝牙相关小demo*/
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void priDev(View view) {
        startActivity(new Intent(this, ClassicBluetoothDevActivity.class));
    }
}
