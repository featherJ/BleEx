package com.bleex.sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.bleex.BleCentralDeviceChangedCallback;
import com.bleex.BleLogger;
import com.bleex.BluetoothStateChangedCallback;
import com.bleex.sample.ble.BleCentralDeviceOld;
import com.bleex.sample.ble.BleServiceOld;
import com.bleex.utils.PermissionsUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionsUtils.checkPermissions(MainActivity.this);

        BleServiceOld bleService = new BleServiceOld(this);
        bleService.addDeviceChangedListener(new BleCentralDeviceChangedCallback<BleCentralDeviceOld>() {
            @Override
            public void onUpdateDevice(BleCentralDeviceOld clientDevice) {
                BleLogger.log(TAG, "onUpdateDevice: " + clientDevice.getAddress());
            }

            @Override
            public void onAddDevice(BleCentralDeviceOld clientDevice) {
                BleLogger.log(TAG, "onAddDevice: " + clientDevice.getAddress());
            }

            @Override
            public void onRemoveDevice(BleCentralDeviceOld clientDevice) {
                BleLogger.log(TAG, "onRemoveDevice: " + clientDevice.getAddress());
            }
        });
        bleService.addBluetoothStateChangedListener(new BluetoothStateChangedCallback() {
            @Override
            public void onStateChanged(boolean enable) {
                if(enable){
                    bleService.startAdvertising();
                    bleService.startService();
                }else{
                    bleService.stopAdvertising();
                    bleService.disconnectAll();
                    bleService.stopService();
                }
            }
        });
        if(bleService.isEnable()){
            bleService.startAdvertising();
            bleService.startService();
        }
    }



    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsUtils.onRequestPermissionsResult(MainActivity.this, requestCode, permissions, grantResults);
    }
}