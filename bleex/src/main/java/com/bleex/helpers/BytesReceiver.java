package com.bleex.helpers;

import android.bluetooth.BluetoothDevice;

import com.bleex.consts.DataTags;

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
public class BytesReceiver {
    //超时时间20秒，所有会使用到接收器的均为有应答的写，所以超时时间仅作为方式内存移除使用，固超时时间很长
    private static final long TIME_OUT = 20000;

    /**
     * 长数据接收器的回调
     *
     * @author Agua.L
     */
    public static abstract class BytesReceiveCallback {
        public void onReceive(BluetoothDevice device, UUID service, UUID characteristic, byte requestIndex, byte[] data) {

        }

        public void onError(BluetoothDevice device, UUID service, UUID characteristic, byte requestIndex) {

        }

        public void onTimeout(BluetoothDevice device, UUID service, UUID characteristic, byte requestIndex) {

        }

        public void onFinish(BluetoothDevice device, UUID service, UUID characteristic, String key) {
        }
    }

    /**
     * 创建一个key
     *
     * @param device
     * @param service
     * @param characteristic
     * @param index
     * @return
     */
    public static String createKey(BluetoothDevice device, UUID service, UUID characteristic, byte index) {
        String key = device.getAddress() + "-" + service + "-" + characteristic + "_" + Integer.toHexString(index & 0xFF);
        return key;
    }

    private BluetoothDevice device;
    private UUID characteristic;
    private UUID service;
    private String key;
    private byte requestIndex;

    public BytesReceiver(String key, byte requestIndex, BluetoothDevice device, UUID service, UUID characteristic) {
        this.key = key;
        this.requestIndex = requestIndex;
        this.device = device;
        this.service = service;
        this.characteristic = characteristic;
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
            if (pack[1] == DataTags.MS_WRITE_LARGE[0] && pack[2] == DataTags.MS_WRITE_LARGE[1]) {
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
                    callback.onError(this.device, this.service, this.characteristic, requestIndex);
                    callback.onFinish(this.device, this.service, this.characteristic, key);
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
                    callback.onError(this.device, this.service, this.characteristic, requestIndex);
                    callback.onFinish(this.device, this.service, this.characteristic, key);
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
                    callback.onReceive(this.device, this.service, this.characteristic, requestIndex, finalBytes);
                    callback.onFinish(this.device, this.service, this.characteristic, key);
                }
                clear();
            } else {
                cancelTimer();
                if (callback != null) {
                    callback.onError(this.device, this.service, this.characteristic, requestIndex);
                    callback.onFinish(this.device, this.service, this.characteristic, key);
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
                        callback.onTimeout(device, service, characteristic, requestIndex);
                        callback.onFinish(device, service, characteristic, key);
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
        this.service = null;
        this.characteristic = null;
        this.callback = null;
    }
}
