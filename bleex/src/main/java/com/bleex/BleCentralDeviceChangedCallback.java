package com.bleex;

/**
 * Ble服务的中心设备改变回调函数
 *
 * @author Agua
 */
public abstract class BleCentralDeviceChangedCallback<T extends BleCentralDeviceBase> {
    /**
     * 当更新了一个设备
     *
     * @param clientDevice
     */
    public void onUpdateDevice(T clientDevice) {
    }

    /**
     * 当添加了一个设备
     *
     * @param clientDevice
     */
    public void onAddDevice(T clientDevice) {
    }

    /**
     * 当移除了一个设备
     *
     * @param clientDevice
     */
    public void onRemoveDevice(T clientDevice) {
    }
}
