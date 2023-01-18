package com.bleex.sample.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ContextWrapper;

import com.bleex.old.BleCentralDeviceBaseOld;
import com.bleex.old.BleServiceBaseOld;
import com.bleex.old.utils.BytesUtil;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class BleServiceOld extends BleServiceBaseOld<BleCentralDeviceOld> {
    public BleServiceOld(ContextWrapper activity) {
        super(activity, BleUUIDs.SERVICE);
        this.initCharacteristics();
    }

    public void startAdvertising() {
        this.startAdvertising(false, Constants.SERVICE_MANUFACTURER_TAG);
    }

    private void initCharacteristics() {
        // Add Central verify characteristic
        this.addRequestCharacteristic(BleUUIDs.VERIFY_CENTRAL);
        // Add base characteristics
        this.addCharacteristic(BleUUIDs.BASE_NOTIFY_TEST, BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        this.addCharacteristic(BleUUIDs.BASE_READ_TEST, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        this.addCharacteristic(BleUUIDs.BASE_WRITE_TEST, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);

        // Add BleEx supported characteristics
        this.addRequestCharacteristic(BleUUIDs.REQUEST_DATA_TEST);
        this.addReceiveBytesCharacteristic(BleUUIDs.WRITE_LARGE_DATA_TO_PERIPHERAL_TEST);
        this.addWriteBytesCharacteristic(BleUUIDs.WRITE_LARGE_DATA_TO_CENTRAL_TEST);
        this.addRequestBytesCharacteristic(BleUUIDs.REQUEST_LARGE_DATA_TEST);
    }

    @Override
    protected BleCentralDeviceBaseOld createCentralDevice(BluetoothDevice device) {
        return new BleCentralDeviceOld(device, this,this.context);
    }

    @Override
    protected void onAddDevice(BluetoothDevice device) {
        // do nothing. Add device is taken over by onRequest method
    }

    @Override
    protected boolean doFilterDevice(BluetoothDevice device, UUID characteristicUuid) {
        // VERIFY_CENTRAL used for authenticate, so return true;
        if (characteristicUuid.equals(BleUUIDs.VERIFY_CENTRAL)) {
            return true;
        }
        // Check if this device is valid
        if (this.getDevice(device.getAddress()) != null) {
            return true;
        }
        return false;
    }

    @Override
    protected byte[] onRequest(BluetoothDevice device, UUID characteristicUuid, byte[] requestingData) {
        if (characteristicUuid.equals(BleUUIDs.VERIFY_CENTRAL)) {
            if (BytesUtil.equals(requestingData, Constants.VERIFY_TAG)) {
                if (this.getDevices().size() == 0) {
                    this.doAddDevice(device);//Do add central device
                    return new byte[]{1};
                } else {
                    BleCentralDeviceOld existDevice = this.getDevice(device.getAddress());
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
        return super.onRequest(device, characteristicUuid, requestingData);
    }
}
