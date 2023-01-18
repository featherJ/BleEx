package com.bleex;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.UUID;


/**
 * Ble中心设备基类
 *
 * @author Agua.L
 */
@SuppressLint("MissingPermission")
public class BleCentralDeviceBase {
    private BluetoothDevice _device;
    private BleServicesBase _service;
    private Context _context;
    private String _address;

    public BleCentralDeviceBase(BluetoothDevice device, BleServicesBase service, Context context) {
        this._device = device;
        this._service = service;
        this._context = context;
        this._address = this._device.getAddress();
    }

    /**
     * 得到内部包含的蓝牙设备
     *
     * @return
     */
    public BluetoothDevice getDevice() {
        return this._device;
    }

    /**
     * 上下文
     *
     * @return
     */
    public Context getContext() {
        return this._context;
    }

    /**
     * 设备地址
     */
    public String getAddress() {
        return this._address;
    }

    public void onUpdateDevice(BluetoothDevice device) {
        this._device = device;
        //TODO 子类重写
    }

    /**
     * 收到了读取数据，最大长度为mtu
     *
     * @param characteristic
     * @param service
     * @return
     */
    protected byte[] onRead(UUID service, UUID characteristic) {
        //TODO 子类重写
        return null;
    }

    /**
     * 收到了写入数据
     *
     * @param characteristic
     * @param service
     * @param data
     */
    protected void onWrite(UUID service, UUID characteristic, byte[] data) {
        //TODO 子类重写
    }

    /**
     * 收到了有应答的请求，最大长度为mtu
     *
     * @param characteristic
     * @param service
     * @param data
     * @return
     */
    protected byte[] onRequest(UUID service, UUID characteristic, byte[] data) {
        //TODO 子类重写
        return new byte[]{0};
    }


    /**
     * 取消连接
     */
    public void disconnect() {
        this._service.disconnect(this._device);
    }

    private boolean isDisposed = false;

    /**
     * 是否已经释放了
     */
    public boolean getIsDisposed() {
        return isDisposed;
    }

    public void dispose() {
        this.isDisposed = true;
        this._device = null;
        this._service = null;
        this._context = null;
    }
}
