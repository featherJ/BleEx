package com.bleex.sample.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import com.bleex.BleServicesBase;
import com.bleex.utils.BytesUtil;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class BleServices extends BleServicesBase<BleCentralDevice> {
    public BleServices(Context context) {
        super(context);
        try {
            this.initServices();
            this.initCharacteristics();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startAdvertising() {
        this.startAdvertising(false, true, Constants.SERVICE_MANUFACTURER_TAG, true);
    }

    private void initServices() throws Exception {
        this.addService(BleUUIDs.SERVICE_1);
        this.addService(BleUUIDs.SERVICE_2);
    }

    private void initCharacteristics() throws Exception {
        // Add Central verify characteristic
        this.addRequestCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.VERIFY_CENTRAL);

        // Add base characteristics
        this.addCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.READ_TEST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        this.addCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.WRITE_TEST,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        this.addCharacteristic(BleUUIDs.SERVICE_2, BleUUIDs.NOTIFY_TEST,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        this.addCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.INDICATE_TEST,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        // Add BleEx supported characteristics
        this.addRequestCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.REQUEST_TEST);
        this.addRequestLargeCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.REQUEST_LARGE_TEST);
        this.addWriteLargeCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.WRITE_LARGE_TEST);
        this.addIndicateLargeCharacteristic(BleUUIDs.SERVICE_1, BleUUIDs.INDICATE_LARGE_TEST);
    }

    @Override
    protected void onAddDevice(BluetoothDevice device) {
        // do nothing. Add device is taken over by onRequest method
    }

    @Override
    protected boolean doFilterDevice(BluetoothDevice device, UUID service, UUID characteristic) {
        // VERIFY_CENTRAL used for authenticate, so return true;
        if (characteristic.equals(BleUUIDs.VERIFY_CENTRAL)) {
            return true;
        }
        // Check if this device is valid
        if (this.getDevice(device.getAddress()) != null) {
            return true;
        }
        return false;
    }

    @Override
    protected byte[] onRequest(BluetoothDevice device, UUID service, UUID characteristic, byte[] value) {
        if (characteristic.equals(BleUUIDs.VERIFY_CENTRAL)) {
            if (BytesUtil.equals(value, Constants.VERIFY_TAG)) {
                if (this.getDevices().size() == 0) {
                    this.doAddDevice(device);//Do add central device
                    return new byte[]{1};
                } else {
                    BleCentralDevice existDevice = this.getDevice(device.getAddress());
                    if (existDevice != null) {
                        this.doUpdateDevice(device);// Update the central device
                        return new byte[]{1};
                    }
                }
            } else {
                this.disconnect(device);
            }
            return new byte[]{0};
        }
        return super.onRequest(device, service, characteristic, value);
    }
}
