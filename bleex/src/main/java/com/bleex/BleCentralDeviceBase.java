package com.bleex;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

import com.bleex.helpers.BytesWriter;

import java.util.UUID;

/**
 * Ble中心设备基类
 * 
 * @author Agua
 */
@SuppressLint("MissingPermission")
public class BleCentralDeviceBase {

    private BluetoothDevice _device;
    private BleServiceBase _service;
    private String _address;


    public BleCentralDeviceBase(BluetoothDevice device, BleServiceBase service) {
        this._device = device;
        this._service = service;
        this._address = this._device.getAddress();
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

    protected byte[] onCharacteristicReadRequest(UUID characteristicUuid) {
        //TODO 子类重写
        return null;
    }

    protected void onCharacteristicWriteRequest(UUID characteristicUuid, byte[] requestingData) {
        //TODO 子类重写
    }

    /**
     * 收到了有应答的请求，最大长度为mtu
     *
     * @param characteristicUuid
     * @param requestingData
     * @return
     */
    protected byte[] onRequest(UUID characteristicUuid, byte[] requestingData) {
        //TODO 子类重写
        return new byte[]{0};
    }

    /**
     * 收到了有应答的请求，不限长度
     *
     * @param characteristicUuid
     * @param requestingData
     * @return
     */
    protected byte[] onRequestBytes(UUID characteristicUuid, byte[] requestingData) {
        //TODO 子类重写
        return new byte[]{0};
    }

    /**
     * 接收到了数据，可以忽视mtu限制长度的数据
     *
     * @param characteristicUuid
     * @param receivedData
     * @return
     */
    protected void onReceiveBytes(UUID characteristicUuid, byte[] receivedData) {
        //TODO 子类重写
    }

    /**
     * 通知到某一个特征
     *
     * @param characteristicUuid
     * @param data
     * @throws Exception
     */
    public void notifyCharacteristic(UUID characteristicUuid, byte[] data){
        BluetoothGattServer bluetoothGattServer = this._service.getBluetoothGattServer();
        BluetoothGattCharacteristic characteristic = this._service.getCharacteristic(characteristicUuid);
        characteristic.setValue(data);
        bluetoothGattServer.notifyCharacteristicChanged(this._device, characteristic, false);
    }

    /**
     * 写数据
     *
     * @param characteristicUuid
     * @param data
     * @param callback
     */
    public void writeBytes(UUID characteristicUuid, byte[] data, BytesWriter.WriteBytesCallback callback) {
        this._service.writeBytes(this._device, characteristicUuid, data, callback);
    }

    /**
     * 取消连接
     */
    public void disconnect() {
        this._service.disconnect(this._device);
    }

    private boolean isDisposed = false;

    public void dispose() {
        this.isDisposed = true;
        this._device = null;
        this._service = null;
    }
}
