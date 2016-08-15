package com.sky;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(this);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                throw new NullPointerException();
            case R.id.btn1:
                throw new IndexOutOfBoundsException();
            case R.id.btn2:
                throw new ArrayIndexOutOfBoundsException();
            case R.id.btn3:
                throw new ArithmeticException();
            case R.id.btn4:
                throw new ArrayStoreException();
        }
    }
}
