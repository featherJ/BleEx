package com.bleex.sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.bleex.BleCentralDeviceChangedCallback;
import com.bleex.BleLogger;
import com.bleex.sample.ble.BleCentralDevice;
import com.bleex.sample.ble.BleService;
import com.bleex.utils.PermissionsUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionsUtils.checkPermissions(MainActivity.this);

        BleService bleService = new BleService(this);
        bleService.startAdvertising();
        bleService.addDeviceChangedListener(new BleCentralDeviceChangedCallback<BleCentralDevice>() {
            @Override
            public void onUpdateDevice(BleCentralDevice clientDevice) {
                BleLogger.log(TAG, "onUpdateDevice: " + clientDevice.getAddress());
            }

            @Override
            public void onAddDevice(BleCentralDevice clientDevice) {
                BleLogger.log(TAG, "onAddDevice: " + clientDevice.getAddress());
            }

            @Override
            public void onRemoveDevice(BleCentralDevice clientDevice) {
                BleLogger.log(TAG, "onRemoveDevice: " + clientDevice.getAddress());
            }
        });
        bleService.startService();
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsUtils.onRequestPermissionsResult(MainActivity.this, requestCode, permissions, grantResults);
    }
}