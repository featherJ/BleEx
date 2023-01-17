package com.bleex.old.helpers;

import android.bluetooth.BluetoothDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * 长数据接收器
 *
 * @author Agua.L
 */
public class BytesRecevier {
    //超时时间2秒
    private static final long TIME_OUT = 2000;

    /**
     * 长数据接收器的回调
     *
     * @author Agua.L
     */
    public static abstract class BytesReceiveCallback {
        public void onReceive(BluetoothDevice device, UUID characteristicUuid, byte requestIndex, byte[] data) {

        }

        public void onError(BluetoothDevice device, UUID characteristicUuid, byte requestIndex) {

        }

        public void onTimeout(BluetoothDevice device, UUID characteristicUuid, byte requestIndex) {

        }

        public void onFinish(BluetoothDevice device, UUID characteristicUuid, String key) {
        }
    }


    private BluetoothDevice device;
    private UUID characteristicUuid;
    private String key;
    private byte requestIndex;

    public BytesRecevier(String key, byte requestIndex, BluetoothDevice device, UUID characteristicUuid) {
        this.key = key;
        this.requestIndex = requestIndex;
        this.device = device;
        this.characteristicUuid = characteristicUuid;
        this.initTimer();
    }

    BytesReceiveCallback callback;

    /**
     * 添加回调
     *
     * @param callback
     */
    public void setCallback(BytesReceiveCallback callback) {
        this.callback = callback;
    }

    private int index = 0;
    private int packageSize = 0;
    private int packageNum = 0;
    private ArrayList<byte[]> packages = new ArrayList();

    public void addPackage(byte[] pack) {
        if (index == 0) {
            //是一个首包
            if (pack[1] == 120 && pack[2] == 110) {
                ByteBuffer buffer = ByteBuffer.wrap(pack);
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.position(3);
                packageSize = buffer.getInt();
                packageNum = buffer.getInt();
                byte[] curPack = new byte[pack.length - 11];
                buffer.get(curPack, 0, pack.length - 11);
                packages.add(curPack);
            } else {
                if (callback != null) {
                    cancelTimer();
                    callback.onError(this.device, this.characteristicUuid, requestIndex);
                    callback.onFinish(this.device, this.characteristicUuid, key);
                }
                clear();
            }
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(pack);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.position(1);
            int curIndex = buffer.getInt();
            if (curIndex == index) {
                byte[] curPack = new byte[pack.length - 5];
                buffer.get(curPack, 0, pack.length - 5);
                packages.add(curPack);
            } else {
                cancelTimer();
                if (callback != null) {
                    callback.onError(this.device, this.characteristicUuid, requestIndex);
                    callback.onFinish(this.device, this.characteristicUuid, key);
                }
                clear();
            }
        }
        index++;
        if (packageNum == index) {
            int length = 0;
            for (int i = 0; i < packages.size(); i++) {
                length += packages.get(i).length;
            }
            ByteBuffer finalBuffer = ByteBuffer.allocate(length);
            for (int i = 0; i < packages.size(); i++) {
                finalBuffer.put(packages.get(i));
            }
            byte[] finalBytes = finalBuffer.array();
            if (finalBytes.length == packageSize) {
                cancelTimer();
                if (callback != null) {
                    callback.onReceive(this.device, this.characteristicUuid, requestIndex, finalBytes);
                    callback.onFinish(this.device, this.characteristicUuid, key);
                }
                clear();
            } else {
                cancelTimer();
                if (callback != null) {
                    callback.onError(this.device, this.characteristicUuid, requestIndex);
                    callback.onFinish(this.device, this.characteristicUuid, key);
                }
                clear();
            }
        }
        this.updateTimer();
    }

    private Timer timer;
    private long updateTimestamp = 0;

    private void initTimer() {
        timer = new Timer();
        updateTimestamp = System.currentTimeMillis();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                long nowTimestamp = System.currentTimeMillis();
                if (nowTimestamp - updateTimestamp >= TIME_OUT) {
                    cancelTimer();
                    if (callback != null) {
                        callback.onTimeout(device, characteristicUuid, requestIndex);
                        callback.onFinish(device, characteristicUuid, key);
                    }
                    clear();
                }
            }
        };
        timer.schedule(task, 0, 100);
    }

    private void updateTimer() {
        updateTimestamp = System.currentTimeMillis();
    }

    private void cancelTimer() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = null;
    }

    private void clear() {
        this.cancelTimer();
        this.device = null;
        this.characteristicUuid = null;
        this.callback = null;
    }
}
