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
    private BleServicesBase services;
    private Context _context;
    private String _address;

    public BleCentralDeviceBase(BluetoothDevice device, BleServicesBase services, Context context) {
        this._device = device;
        this.services = services;
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
     * 收到了长数据的写入
     *
     * @param service
     * @param characteristic
     * @param data
     */
    protected void onWriteLarge(UUID service, UUID characteristic, byte[] data) {
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
     * 收到了有应答的请求，最大长度为mtu
     *
     * @param characteristic
     * @param service
     * @param data
     * @return
     */
    protected byte[] onRequestLarge(UUID service, UUID characteristic, byte[] data) {
        //TODO 子类重写
        return new byte[]{0};
    }

    /**
     * 通知到某一个特征，最大长度为mtu，有丢包概率
     *
     * @param service
     * @param characteristic
     * @param data
     */
    public void notify(UUID service, UUID characteristic, byte[] data) throws Exception {
        if (isDisposed) {
            throw new Exception("Can not call notify after device disposed.");
        }
        this.services.notifyCharacteristicChanged(this.getDevice(), service, characteristic, data, false);
    }

    /**
     * 指示到某一个特征，最大长度为mtu，不易丢包
     *
     * @param service
     * @param characteristic
     * @param data
     */
    public void indicate(UUID service, UUID characteristic, byte[] data) throws Exception {
        if (isDisposed) {
            throw new Exception("Can not call indicate after device disposed.");
        }
        this.services.notifyCharacteristicChanged(this.getDevice(), service, characteristic, data, true);
    }

    /**
     * 指示长数据到某一个特征，不易丢包
     *
     * @param service
     * @param characteristic
     * @param data
     */
    public void indicateLarge(UUID service, UUID characteristic, byte[] data) throws Exception {
        if (isDisposed) {
            throw new Exception("Can not call indicateLarge after device disposed.");
        }
        this.services.indicateLarge(this.getDevice(), service, characteristic, data);
    }

    /**
     * 取消连接
     */
    public void disconnect() {
        this.services.disconnect(this._device);
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
        this.services = null;
        this._context = null;
    }
}
