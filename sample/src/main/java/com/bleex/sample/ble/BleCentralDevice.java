package com.bleex.sample.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.bleex.BleCentralDeviceBase;
import com.bleex.BleLogger;
import com.bleex.BleServicesBase;
import com.bleex.utils.BytesUtil;

import java.util.UUID;

public class BleCentralDevice extends BleCentralDeviceBase {
    private static final String TAG = "BleCentralDevice";

    public BleCentralDevice(BluetoothDevice device, BleServicesBase services, Context context) {
        super(device, services, context);
    }

    @Override
    public void onUpdateDevice(BluetoothDevice device) {
        BleLogger.log(TAG, "onUpdateDevice device:" + device);
        super.onUpdateDevice(device);
    }

    @Override
    protected byte[] onRead(UUID service, UUID characteristic) {
        BleLogger.log(TAG, "onRead service:" + service + " characteristic:" + characteristic);
        if (service.equals(BleUUIDs.SERVICE_1) && characteristic.equals(BleUUIDs.READ_TEST)) {
            byte[] data = new byte[20];
            for (int i = 0; i < 20; i++) {
                data[i] = (byte) i;
            }
            return data;
        }
        return super.onRead(service, characteristic);
    }

    @Override
    protected void onWrite(UUID service, UUID characteristic, byte[] data) {
        BleLogger.log(TAG, "onWrite service:" + service + " characteristic:" + characteristic + " "+ BytesUtil.bytesToString(data,false));
        if (service.equals(BleUUIDs.SERVICE_1) && characteristic.equals(BleUUIDs.WRITE_TEST)) {
            //Test indicate to central device
            byte[] datas = new byte[10];
            for (int i = 0; i < 10; i++) {
                datas[i] = 1;
            }
            try {
                this.indicate(BleUUIDs.SERVICE_1, BleUUIDs.INDICATE_TEST, datas);
            } catch (Exception e) {
                e.printStackTrace();
            }
            datas = new byte[10];
            for (int i = 0; i < 10; i++) {
                datas[i] = 2;
            }
            try {
                this.notify(BleUUIDs.SERVICE_2, BleUUIDs.NOTIFY_TEST, datas);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onWrite(service, characteristic, data);
    }


    @Override
    protected byte[] onRequest(UUID service, UUID characteristic, byte[] data) {
        BleLogger.log(TAG, "onRequest service:" + service + " characteristic:" + characteristic + " data: (length:" + data.length + ") " + BytesUtil.bytesToString(data, false));
        if (characteristic.equals(BleUUIDs.REQUEST_TEST)) {
            byte[] response = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (data[i] + 1);
            }
            return data;
        }
        return super.onRequest(service, characteristic, data);
    }

    @Override
    protected byte[] onRequestLarge(UUID service, UUID characteristic, byte[] data) {
        BleLogger.log(TAG, "onRequestLarge service:" + service + " characteristic:" + characteristic + " data: (length:" + data.length + ") " + BytesUtil.bytesToString(data, false));
        if (characteristic.equals(BleUUIDs.REQUEST_LARGE_TEST)) {
            byte[] response = new byte[2000];
            for (int i = 0; i < 2000; i++) {
                response[i] = (byte) 9;
            }
            return response;
        }
        return super.onRequestLarge(service, characteristic, data);
    }

    @Override
    protected void onWriteLarge(UUID service, UUID characteristic, byte[] data) {
        BleLogger.log(TAG, "onWriteLarge service:" + service + " characteristic:" + characteristic + " data: (length:" + data.length + ") " + BytesUtil.bytesToString(data, false));
        //Test writing large bytes to central device
        byte[] datas = new byte[2000];
        for (int i = 0; i < 2000; i++) {
            datas[i] = 2;
        }
        try {
            this.indicateLarge(service, BleUUIDs.INDICATE_LARGE_TEST, datas);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onWriteLarge(service, characteristic, data);
    }
}
