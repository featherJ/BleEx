package com.bleex.helpers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.bleex.BleServicesBase;
import com.bleex.consts.DataTags;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 长数据发送器
 *
 * @author Agua.L
 */
@SuppressLint("MissingPermission")
public class BytesWriter {
    private static String TAG = "BytesWriter";

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

    byte writeIndex;
    int packageSize;
    String key;
    BluetoothDevice device;
    BleServicesBase services;
    UUID service;
    UUID characteristic;

    public BytesWriter(byte writeIndex, int packageSize, String key, BluetoothDevice device, BleServicesBase services, UUID service, UUID characteristic) {
        this.writeIndex = writeIndex;
        this.packageSize = packageSize;
        this.key = key;
        this.device = device;
        this.services = services;
        this.service = service;
        this.characteristic = characteristic;
    }

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
                pack[1] = DataTags.SM_INDICATE_LARGE[0];
                pack[2] = DataTags.SM_INDICATE_LARGE[1];
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
        try {
            for (int i = 0; i < packages.size(); i++) {
                byte[] pack = packages.get(i);
                services.notifyCharacteristicChanged(device, service, characteristic, pack, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.clear();
    }


    private void clear() {
        this.device = null;
        this.services = null;
        this.service = null;
        this.characteristic = null;
    }
}
