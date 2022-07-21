package com.bleex.helpers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

import com.bleex.BleLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * 长数据发送器
 *
 * @author Agua
 */
@SuppressLint("MissingPermission")
public class BytesWriter {
    private static String TAG = "BytesWriter";

    /**
     * 写数据回调
     */
    public static abstract class WriteBytesCallback {
        /**
         * 写数据完成
         */
        public void onSent() {
        }

        /**
         * 写数据错误
         */
        public void onError() {
        }

        /**
         * 写数据超时
         */
        public void onTimeout() {
        }

        /**
         * 写数据完成
         */
        public void onFinish(String key) {
        }
    }

    /**
     * 创建一个key
     *
     * @param device
     * @param characteristicUuid
     * @param index
     * @return
     */
    public static String createKey(BluetoothDevice device, UUID characteristicUuid, byte index) {
        String key = device.getAddress() + "-" + characteristicUuid.toString() + "_" + Integer.toHexString(index & 0xFF);
        return key;
    }

    byte writeIndex;
    int packageSize;
    String key;
    BluetoothDevice device;
    BluetoothGattServer bluetoothGattServer;
    BluetoothGattCharacteristic characteristic;

    public BytesWriter(byte writeIndex, int packageSize, String key, BluetoothDevice device, BluetoothGattServer bluetoothGattServer, BluetoothGattCharacteristic characteristic) {
        this.writeIndex = writeIndex;
        this.packageSize = packageSize;
        this.key = key;
        this.device = device;
        this.bluetoothGattServer = bluetoothGattServer;
        this.characteristic = characteristic;
    }


    private Timer sendTimer;


    WriteBytesCallback callback;

    /**
     * 添加回调
     *
     * @param callback
     */
    public void setCallback(WriteBytesCallback callback) {
        this.callback = callback;
    }

    boolean completed = false;

    /**
     * 写数据
     *
     * @param data
     */
    public void writeBytes(byte[] data) {
        //首包：请求号+起始包标识+包数据长度+包个数+报数据
        //其他包：请求号+包索引+包数据
        int dataSize = data.length;
        int start = 0;
        int index = 0;
        ArrayList<byte[]> packages = new ArrayList();
        ByteBuffer bufferData = ByteBuffer.wrap(data);
        while (start < dataSize) {
            if (start == 0) {
                int length = Math.min(packageSize, (dataSize - start) + 11);
                byte[] pack = new byte[length];
                //请求号
                pack[0] = writeIndex;
                //起始包标识
                pack[1] = 110;
                pack[2] = 100;
                //数据长度
                ByteBuffer bufferPack = ByteBuffer.wrap(pack);
                bufferPack.order(ByteOrder.BIG_ENDIAN);
                bufferPack.position(3);
                bufferPack.putInt(dataSize);
                bufferPack.position(3);
                bufferPack.get(pack, 3, 4);
                //包个数，先填写空白
                pack[7] = 0;
                pack[8] = 0;
                pack[9] = 0;
                pack[10] = 0;
                //包数据
                bufferData.position(start);
                int end = Math.min(packageSize - 11, dataSize);
                bufferData.get(pack, 11, end - start);
                start = end;
                packages.add(pack);
            } else {
                int length = Math.min(packageSize, (dataSize - start) + 5);
                byte[] pack = new byte[length];
                //请求号
                pack[0] = writeIndex;
                //包索引数
                ByteBuffer bufferPack = ByteBuffer.wrap(data);
                bufferPack.order(ByteOrder.BIG_ENDIAN);
                bufferPack.position(1);
                bufferPack.putInt(index);
                bufferPack.position(1);
                bufferPack.get(pack, 1, 4);

                //包数据
                bufferData.position(start);
                int end = Math.min(start + packageSize - 5, dataSize);
                bufferData.get(pack, 5, end - start);
                start = end;
                packages.add(pack);
            }
            index++;
        }
        //填补包个数
        byte[] firstPack = packages.get(0);
        ByteBuffer buffer = ByteBuffer.wrap(firstPack);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.position(7);
        buffer.putInt(packages.size());
        buffer.position(7);
        buffer.get(firstPack, 7, 4);

        sendTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (packages.size() > 0 && !completed) {
                    byte[] pack = packages.remove(0);
                    characteristic.setValue(pack);
                    bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                } else {
                    sendTimer.cancel();
                    sendTimer = null;
                    initTimeout();
                }
            }
        };
        sendTimer.schedule(task, 0, 30);
    }

    private Timer timeoutTimer;

    private void initTimeout() {
        long initTime = System.currentTimeMillis();
        timeoutTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - initTime >= 2000) {
                    cancelTimer();
                    completed = true;
                    if (callback != null) {
                        callback.onTimeout();
                        callback.onFinish(key);
                    }
                    clear();
                }
            }
        };
        timeoutTimer.schedule(task, 0, 100);
    }

    /**
     * 主设备写入了内容
     *
     * @param data
     */
    public void onCharacteristicWriteRequest(byte[] data) {
        BleLogger.log(TAG, "Received the result:" + data.toString() + " of sending long bytes");
        if (data.length >= 4) {
            byte result = data[3];
            if (result == 0) {
                cancelTimer();
                completed = true;
                if (callback != null) {
                    callback.onSent();
                    callback.onFinish(key);
                }
                clear();
            } else if (result == 1) {
                cancelTimer();
                completed = true;
                if (callback != null) {
                    callback.onError();
                    callback.onFinish(key);
                }
                clear();
            } else if (result == 2) {
                cancelTimer();
                completed = true;
                if (callback != null) {
                    callback.onTimeout();
                    callback.onFinish(key);
                }
                clear();
            }
        }
    }


    private void cancelTimer() {
        if (sendTimer != null) {
            sendTimer.cancel();
        }
        sendTimer = null;
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
        }
        timeoutTimer = null;
    }

    private void clear() {
        this.cancelTimer();
        this.device = null;
        this.bluetoothGattServer = null;
        this.characteristic = null;
        this.callback = null;
    }


}
