package com.bleex;

import android.util.Log;

/**
 * Ble内部日志输出工具
 *
 * @author Agua.L
 */
public class BleLogger {
    /**
     * ble内容输出
     *
     * @param tag
     * @param msg
     */
    public static void log(String tag, String msg) {
        Log.i("[BleLog: " + tag + "]", msg);
    }
}
