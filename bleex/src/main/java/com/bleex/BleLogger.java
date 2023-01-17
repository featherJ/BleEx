package com.bleex;

import android.util.Log;

/**
 * Ble内部日志输出工具
 *
 * @author Agua.L
 */
public class BleLogger {
    public static boolean enable = true;
    /**
     * ble内容输出
     *
     * @param tag
     * @param msg
     */
    public static void log(String tag, String msg) {
        if(enable){
            Log.i("[BleLog: " + tag + "]", msg);
        }
    }
}
