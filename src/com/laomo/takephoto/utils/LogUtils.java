package com.laomo.takephoto.utils;

import android.util.Log;

public class LogUtils {
    //private static boolean isLog = false;
    private static boolean isLog = true;

    public static void log(String msg) {
	if (isLog) {
	    Log.d("laomo", msg);
	}
    }
}