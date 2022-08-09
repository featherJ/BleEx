package com.bleex;

/**
 * 蓝牙状态改变回调
 *
 * @author Agua
 */
public abstract class BluetoothStateChangedCallback {
    /**
     * 当前蓝牙状态改变
     *
     * @param enable 蓝牙状态
     */
    public void onStateChanged(boolean enable) {
    }

}
