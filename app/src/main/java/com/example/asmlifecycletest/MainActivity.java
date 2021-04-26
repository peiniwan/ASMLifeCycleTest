package com.example.asmlifecycletest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = findViewById(R.id.btn);
        Map map = new HashMap();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BActivity.class));
            }
        });

        TestAdd testAdd = new TestAdd();
        testAdd.release();
    }

    public void onClick(View view) {
        a();
        b();
        c();
    }

    public void a() {
        final long start = System.currentTimeMillis();
        SystemClock.sleep(780);
        final long delta = System.currentTimeMillis() - start;
//        long start = getTime();
        toLong(start);
    }

    private void toLong(long start) {
        long delta = System.currentTimeMillis() - start;
        if (delta > 2000) {
            System.out.print("时间太长了");
        }
    }

    private long getTime() {
        return System.currentTimeMillis();
    }


    public void b() {

    }

    public void c() {
        SystemClock.sleep(200);
    }
}
