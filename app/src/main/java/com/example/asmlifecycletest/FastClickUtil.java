package com.example.asmlifecycletest;

import android.content.Context;
import android.widget.Toast;

public class FastClickUtil {
    private static long lastClickTime;

    public static boolean isFastDoubleClick(Context context) {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < 600) {
            Toast.makeText(context,"点的太快啦",Toast.LENGTH_SHORT).show();
            return true;
        }
        lastClickTime = time;
        return false;
    }
}
