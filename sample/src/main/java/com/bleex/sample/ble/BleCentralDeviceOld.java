package com.bleex.sample.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.bleex.old.BleCentralDeviceBaseOld;
import com.bleex.BleLogger;
import com.bleex.old.BleServiceBaseOld;
import com.bleex.old.helpers.BytesWriter;
import com.bleex.old.utils.BytesUtil;

import java.util.UUID;

public class BleCentralDeviceOld extends BleCentralDeviceBaseOld {
    private static final String TAG = "BleCentralDevice";

    public BleCentralDeviceOld(BluetoothDevice device, BleServiceBaseOld service, Context context) {
        super(device, service, context);
    }

    @Override
    public void onUpdateDevice(BluetoothDevice device) {
        BleLogger.log(TAG, "onUpdateDevice device:" + device.toString());
        super.onUpdateDevice(device);
    }

    @Override
    protected byte[] onCharacteristicReadRequest(UUID characteristicUuid) {
        BleLogger.log(TAG, "onCharacteristicReadRequest characteristicUuid:" + characteristicUuid.toString());
        //Read from central device
        if (characteristicUuid.equals(BleUUIDs.BASE_READ_TEST)) {
            byte[] data = new byte[20];
            for (int i = 0; i < 20; i++) {
                data[i] = (byte) i;
            }
            return data;
        }
        return super.onCharacteristicReadRequest(characteristicUuid);
    }

    @Override
    protected void onCharacteristicWriteRequest(UUID characteristicUuid, byte[] requestingData) {
        BleLogger.log(TAG, "onCharacteristicWriteRequest characteristicUuid:" + characteristicUuid.toString() + " ");
        if (characteristicUuid.equals(BleUUIDs.BASE_WRITE_TEST)) {

            //Test notify to central device
            byte[] datas = new byte[10];
            for (int i = 0; i < 10; i++) {
                datas[i] = 1;
            }
            try {
                this.notifyCharacteristic(BleUUIDs.BASE_NOTIFY_TEST, datas);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onCharacteristicWriteRequest(characteristicUuid, requestingData);
    }

    @Override
    protected byte[] onRequest(UUID characteristicUuid, byte[] requestingData) {
        BleLogger.log(TAG, "onRequest characteristicUuid:" + characteristicUuid.toString() + " requestingData: (length:" + requestingData.length + ") " + BytesUtil.bytesToHex(requestingData));
        //Request large bytes from central device
        if (characteristicUuid.equals(BleUUIDs.REQUEST_DATA_TEST)) {
            byte[] data = new byte[requestingData.length];
            for (int i = 0; i < requestingData.length; i++) {
                data[i] = (byte) (requestingData[i] + 1);
            }
            return data;
        }
        return super.onRequest(characteristicUuid, requestingData);
    }


    @Override
    protected byte[] onRequestBytes(UUID characteristicUuid, byte[] requestingData) {
        BleLogger.log(TAG, "onRequestBytes characteristicUuid:" + characteristicUuid.toString() + " requestingData: (length:" + requestingData.length + ") " + BytesUtil.bytesToHex(requestingData));
        //Request large bytes from central device
        if (characteristicUuid.equals(BleUUIDs.REQUEST_LARGE_DATA_TEST)) {
            byte[] data = new byte[100000];
            for (int i = 0; i < 100000; i++) {
                data[i] = (byte) 9;
            }
            return data;
        }
        return super.onRequestBytes(characteristicUuid, requestingData);
    }

    @Override
    protected void onReceiveBytes(UUID characteristicUuid, byte[] receivedData) {
        BleLogger.log(TAG, "onReceiveBytes characteristicUuid:" + characteristicUuid.toString() + " receivedData: (length:" + receivedData.length + ") " + BytesUtil.bytesToHex(receivedData));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Test writing large bytes to central device
        byte[] datas = new byte[2000];
        for (int i = 0; i < 2000; i++) {
            datas[i] = 2;
        }
        try {
            this.writeBytes(BleUUIDs.WRITE_LARGE_DATA_TO_CENTRAL_TEST, datas, new BytesWriter.WriteBytesCallback() {
                @Override
                public void onSent() {
                    BleLogger.log(TAG, "Write large bytes to " + BleUUIDs.WRITE_LARGE_DATA_TO_CENTRAL_TEST.toString() + " succeeded.");
                }

                @Override
                public void onError() {
                    BleLogger.log(TAG, "Error writing large bytes to " + BleUUIDs.WRITE_LARGE_DATA_TO_CENTRAL_TEST.toString() + ".");
                }

                @Override
                public void onTimeout() {
                    BleLogger.log(TAG, "Timeout writing large bytes to " + BleUUIDs.WRITE_LARGE_DATA_TO_CENTRAL_TEST.toString() + ".");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
