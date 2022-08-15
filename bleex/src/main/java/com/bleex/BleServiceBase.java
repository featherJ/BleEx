package com.bleex;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import com.bleex.helpers.BytesRecevier;
import com.bleex.helpers.BytesWriter;
import com.bleex.utils.BytesUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Ble蓝牙服务基类
 *
 * @author Agua
 */
@SuppressLint("MissingPermission")
public class BleServiceBase<T extends BleCentralDeviceBase> {
    private static final String TAG = "BleServiceBase";

    private final UUID uuidService;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothGattService bleService;
    private BluetoothGattServer bluetoothGattServer; // BLE服务端
    private final BluetoothLeAdvertiser bluetoothLeAdvertiser; // BLE广播
    private final ContextWrapper context;

    private final HashMap<String, T> deviceMap = new HashMap<>();

    protected BleServiceBase self;

    private int packageSize = 20;

    public BleServiceBase(ContextWrapper context, UUID serviceUuid) {
        this.self = this;
        this.context = context;

        this.uuidService = serviceUuid;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        this.bleService = new BluetoothGattService(this.uuidService, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BroadcastReceiver broadcastReceiver = new BluetoothStateBroadcastReceive();
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, intent);
    }

    class BluetoothStateBroadcastReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_OFF:
                            BleLogger.log(TAG,"Bluetooth disabled");
                            onBluetoothChanged(false);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            BleLogger.log(TAG,"Bluetooth enabled");
                            onBluetoothChanged(true);
                            break;
                    }
                    break;
            }
        }
    }

    public boolean isEnable() {
        return this.bluetoothAdapter.isEnabled();
    }

    private void onBluetoothChanged(boolean enable) {
        for (BluetoothStateChangedCallback callback : bluetoothStateChangedCallbacks) {
            callback.onStateChanged(enable);
        }
    }

    /**
     * 可发送的包大小
     */
    public int getPackageSize() {
        return packageSize;
    }

    /**
     * 当前的BLE服务器
     */
    public BluetoothGattServer getBluetoothGattServer() {
        return this.bluetoothGattServer;
    }

    private boolean isAdvertising = false;

    /**
     * 是否正在广播
     */
    public boolean getIsAdvertising() {
        return isAdvertising;
    }

    /**
     * 启动或更新BLE蓝牙广播(广告)
     *
     * @param includeName      是否包含蓝牙名称
     * @param manufacturerData 包含的设备信息
     */
    public void startAdvertising(boolean includeName, byte[] manufacturerData) {
        this.startAdvertising(includeName, manufacturerData, true);
    }

    /**
     * 启动或更新BLE蓝牙广播(广告)
     *
     * @param includeName      是否包含蓝牙名称
     * @param manufacturerData 包含的设备信息
     * @param connectable      是否可以连接
     */
    public void startAdvertising(boolean includeName, byte[] manufacturerData, boolean connectable) {
        if (isAdvertising) {
            stopAdvertising();
        }
        // 注意：必须要开启可连接的BLE广播，其它设备才能发现并连接BLE服务端!
        // 广播设置(必须)
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
                .setConnectable(connectable) //能否连接,广播分为可连接广播和不可连接广播
                .build();
        // 广播数据(必须，广播启动就会发送)
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(includeName) //包含蓝牙名称，有的设备蓝牙名特别长，直接关了得了
                .setIncludeTxPowerLevel(false) //包含发射功率级别
                .addServiceUuid(new ParcelUuid(uuidService))
                .build();
        AdvertiseData scanResponse = null;
        //附加信息
        if (manufacturerData != null) {
            // 扫描响应数据(可选，当客户端扫描时才发送)
            scanResponse = new AdvertiseData.Builder()
                    .addManufacturerData(1, manufacturerData)
                    .build();
        }
        if (scanResponse != null) {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, this.advertiseCallback);
        } else {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, this.advertiseCallback);
        }
        isAdvertising = true;
    }

    /**
     * 停止BLE蓝牙广播(广告)
     */
    public void stopAdvertising() {
        bluetoothLeAdvertiser.stopAdvertising(this.advertiseCallback);
        isAdvertising = false;
    }


    /**
     * 添加一个 Characteristic
     *
     * @param uuid
     * @param properties
     * @param permissions
     */
    public BluetoothGattCharacteristic addCharacteristic(UUID uuid, int properties, int permissions) {
        BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(uuid,
                properties, permissions);
        //如果包含通知，则需要加通知描述，不然苹果监听通知会报错
        if (
                (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                        || (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        ) {
            //TODO 测试加密情况下的传输
            BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            characteristicRead.addDescriptor(configDescriptor);
        }
        this.bleService.addCharacteristic(characteristicRead);
        BleLogger.log(TAG, "Characteristic " + uuid.toString() + " added.");
        return characteristicRead;
    }

    private ArrayList<BluetoothGattCharacteristic> requestCharacteristics = new ArrayList<>();

    /**
     * 添加一个主设备向从设备进行有应答请求的特征
     *
     * @param uuid
     */
    public BluetoothGattCharacteristic addRequestCharacteristic(UUID uuid) {
        BluetoothGattCharacteristic requestCharacteristic = this.addCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        requestCharacteristics.add(requestCharacteristic);
        return requestCharacteristic;
    }

    /**
     * 判断某个特征是否是有应答请求的特征
     *
     * @param uuid
     * @return
     */
    private boolean isRequestCharacteristic(UUID uuid) {
        for (int i = 0; i < requestCharacteristics.size(); i++) {
            if (requestCharacteristics.get(i).getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<BluetoothGattCharacteristic> receiveBytesCharacteristics = new ArrayList<>();

    /**
     * 添加一个可以接收长数据的特征
     *
     * @param uuid
     */
    public BluetoothGattCharacteristic addReceiveBytesCharacteristic(UUID uuid) {
        BluetoothGattCharacteristic receiveBytesCharacteristic = this.addCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        receiveBytesCharacteristics.add(receiveBytesCharacteristic);
        return receiveBytesCharacteristic;
    }

    /**
     * 判断某个特征是否是可以接收长数据的特征
     *
     * @param uuid
     * @return
     */
    private boolean isReceiveBytesCharacteristic(UUID uuid) {
        for (int i = 0; i < receiveBytesCharacteristics.size(); i++) {
            if (receiveBytesCharacteristics.get(i).getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private final ArrayList<BluetoothGattCharacteristic> writeBytesCharacteristics = new ArrayList<>();

    /**
     * 添加一个可以写长数据的特征
     *
     * @param uuid
     */
    public BluetoothGattCharacteristic addWriteBytesCharacteristic(UUID uuid) {
        BluetoothGattCharacteristic writeBytesCharacteristic = this.addCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        writeBytesCharacteristics.add(writeBytesCharacteristic);
        return writeBytesCharacteristic;
    }

    /**
     * 判断某个特征是否是可以写长数据的特征
     *
     * @param uuid
     * @return
     */
    private boolean isWriteBytesCharacteristic(UUID uuid) {
        for (int i = 0; i < writeBytesCharacteristics.size(); i++) {
            if (writeBytesCharacteristics.get(i).getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<BluetoothGattCharacteristic> requestBytesCharacteristics = new ArrayList<>();

    /**
     * 添加一个可以请求长数据的特征
     *
     * @param uuid
     */
    public BluetoothGattCharacteristic addRequestBytesCharacteristic(UUID uuid) {
        BluetoothGattCharacteristic requestBytesCharacteristic = this.addCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        requestBytesCharacteristics.add(requestBytesCharacteristic);
        receiveBytesCharacteristics.add(requestBytesCharacteristic);
        writeBytesCharacteristics.add(requestBytesCharacteristic);
        return requestBytesCharacteristic;
    }

    /**
     * 判断某个特征是否是可以请求长数据的特征
     *
     * @param uuid
     * @return
     */
    private boolean isRequestBytesCharacteristic(UUID uuid) {
        for (int i = 0; i < requestBytesCharacteristics.size(); i++) {
            if (requestBytesCharacteristics.get(i).getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据uuid得到特征
     *
     * @param uuid
     * @return
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        return this.bleService.getCharacteristic(uuid);
    }

    /**
     * 启动服务
     */
    public void startService() {
        this.bluetoothGattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        this.bluetoothGattServer.addService(this.bleService);
    }

    /**
     * 停止服务
     */
    public void stopService() {
        this.bluetoothGattServer.removeService(this.bleService);
        this.bluetoothGattServer.close();
    }


    //BLE广播回调
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            BleLogger.log(TAG, "Ble advertise succeeded.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            BleLogger.log(TAG, "Ble advertise failed with " + errorCode + ".");
        }
    };

    /**
     * 得到一个指定的设备
     *
     * @param device
     * @return
     */
    public T getDevice(BluetoothDevice device) {
        return getDevice(device.getAddress());
    }

    /**
     * 得到一个指定的设备
     *
     * @param address
     * @return
     */
    public T getDevice(String address) {
        if (deviceMap.containsKey(address)) {
            return deviceMap.get(address);
        }
        return null;
    }

    /**
     * 得到所有已连接的设备列表
     *
     * @return
     */
    public List<T> getDevices() {
        return new ArrayList<>(deviceMap.values());
    }


    private List<BleCentralDeviceChangedCallback<T>> deviceChangedCallbacks = new ArrayList<BleCentralDeviceChangedCallback<T>>();

    /**
     * 添加设备改变监听回调
     *
     * @param callback
     */
    public void addDeviceChangedListener(BleCentralDeviceChangedCallback<T> callback) {
        deviceChangedCallbacks.add(callback);
    }

    /**
     * 移除设备改变监听回调
     *
     * @param callback
     */
    public void removeDeviceChangedListener(BleCentralDeviceChangedCallback<T> callback) {
        deviceChangedCallbacks.remove(callback);
    }


    private List<BluetoothStateChangedCallback> bluetoothStateChangedCallbacks = new ArrayList<BluetoothStateChangedCallback>();

    /**
     * 添加蓝牙状态改变回调
     *
     * @param callback
     */
    public void addBluetoothStateChangedListener(BluetoothStateChangedCallback callback) {
        bluetoothStateChangedCallbacks.add(callback);
    }

    /**
     * 添加蓝牙状态改变回调
     *
     * @param callback
     */
    public void removeBluetoothStateChangedListener(BluetoothStateChangedCallback callback) {
        bluetoothStateChangedCallbacks.remove(callback);
    }


    /**
     * 当更新了设备（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     */
    protected void onUpdateDevice(BluetoothDevice device) {
        doUpdateDevice(device);
    }

    /**
     * 执行更新了一个设备
     *
     * @param device
     */
    protected void doUpdateDevice(BluetoothDevice device) {
        T curDevice = deviceMap.get(device.getAddress());
        if (curDevice != null) {
            curDevice.onUpdateDevice(device);
            for (BleCentralDeviceChangedCallback<T> callback : deviceChangedCallbacks) {
                callback.onUpdateDevice(curDevice);
            }
        }
    }

    /**
     * 当添加了设备（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     */
    protected void onAddDevice(BluetoothDevice device) {
        doAddDevice(device);
    }

    /**
     * 执行添加一个设备
     *
     * @param device
     */
    protected void doAddDevice(BluetoothDevice device) {
        if (deviceMap.containsKey(device.getAddress())) {
            //避免重复添加
            return;
        }
        T curDevice = (T) this.createCentralDevice(device);
        deviceMap.put(device.getAddress(), curDevice);
        for (BleCentralDeviceChangedCallback<T> callback : deviceChangedCallbacks) {
            callback.onAddDevice(curDevice);
        }
    }

    /**
     * 创建一个客户端设备，子类可以重写
     *
     * @param device
     * @return
     */
    protected BleCentralDeviceBase createCentralDevice(BluetoothDevice device) {
        return new BleCentralDeviceBase(device, self);
    }

    /**
     * 当移除了设备（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     */
    protected void onRemoveDevice(BluetoothDevice device) {
        this.doRemoveDevice(device);
    }

    /**
     * 当移除了设备（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     */
    protected void doRemoveDevice(BluetoothDevice device) {
        T curDevice = deviceMap.get(device.getAddress());
        if (curDevice != null) {
            curDevice.dispose();
            deviceMap.remove(device.getAddress());
            for (BleCentralDeviceChangedCallback<T> callback : deviceChangedCallbacks) {
                callback.onRemoveDevice(curDevice);
            }
        }
    }

    /**
     * 接收到特征的读请求（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     * @param requestId
     * @param offset
     * @param characteristic
     */
    protected void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        BleCentralDeviceBase clientDevice = getDevice(device);
        byte[] responseData = null;
        if (clientDevice != null) {
            responseData = clientDevice.onCharacteristicReadRequest(characteristic.getUuid());
        }
        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, responseData);// 响应客户端
    }

    /**
     * 接收到特征的写请求（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     * @param requestId
     * @param characteristic
     * @param preparedWrite
     * @param responseNeeded
     * @param offset
     * @param requestingBytes
     */
    protected void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestingBytes) {
        if (responseNeeded) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }
        if (isRequestCharacteristic(characteristic.getUuid())) {
            //有应答请求的写
            if (requestingBytes.length > 0) {
                byte deviceRequestId = requestingBytes[0];
                byte[] requestingData = new byte[requestingBytes.length - 1];
                for (int i = 1; i < requestingBytes.length; i++) {
                    requestingData[i - 1] = requestingBytes[i];
                }
                byte[] response = onRequest(device, characteristic.getUuid(), requestingData);
                byte[] finalResponse = new byte[response.length + 1];
                finalResponse[0] = deviceRequestId;
                for (int i = 0; i < response.length; i++) {
                    finalResponse[i + 1] = response[i];
                }
                BluetoothGattServer bluetoothGattServer = this.getBluetoothGattServer();
                characteristic.setValue(finalResponse);
                //将请求结果发送给主设备
                bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            }
            return;
        } else if (isReceiveBytesCharacteristic(characteristic.getUuid())) {
            receiveBytes(device, characteristic.getUuid(), requestingBytes);
            return;
        } else if (isWriteBytesCharacteristic(characteristic.getUuid())) {
            //如果收到的是写长数据特征的数据，则直接派发给对应的写者
            if (requestingBytes.length >= 4 && requestingBytes[0] == 110 && requestingBytes[1] == 110) {
                byte writeIndex = requestingBytes[2];
                String key = BytesWriter.createKey(device, characteristic.getUuid(), writeIndex);
                if (bytesWriters.containsKey(key)) {
                    BytesWriter writer = bytesWriters.get(key);
                    writer.onCharacteristicWriteRequest(requestingBytes);
                }
            }
            return;
        }
        BleCentralDeviceBase clientDevice = getDevice(device);
        if (clientDevice != null) {
            clientDevice.onCharacteristicWriteRequest(characteristic.getUuid(), requestingBytes);
        }
    }

    /**
     * 收到了有应答的请求
     *
     * @param device
     * @param characteristicUuid
     * @param requestingData
     * @return
     */
    protected byte[] onRequest(BluetoothDevice device, UUID characteristicUuid, byte[] requestingData) {
        BleCentralDeviceBase clientDevice = getDevice(device);
        if (clientDevice != null) {
            return clientDevice.onRequest(characteristicUuid, requestingData);
        }
        return new byte[]{0};
    }

    /**
     * 收到了有应答的请求
     *
     * @param device
     * @param characteristicUuid
     * @param requestingData
     * @return
     */
    private byte[] onRequestBytes(BluetoothDevice device, UUID characteristicUuid, byte[] requestingData) {
        BleCentralDeviceBase clientDevice = getDevice(device);
        if (clientDevice != null) {
            return clientDevice.onRequestBytes(characteristicUuid, requestingData);
        }
        return new byte[]{0};
    }

    private final HashMap<String, BytesRecevier> bytesReceivers = new HashMap<>();

    private void receiveBytes(BluetoothDevice device, UUID characteristicUuid, byte[] pack) {
        byte requestIndex = -1;
        if (pack.length >= 1) {
            requestIndex = pack[0];
        } else {
            return;
        }

        String key = device.getAddress() + "-" + characteristicUuid.toString() + "_" + Integer.toHexString(requestIndex & 0xFF);
        BytesRecevier receiver = null;
        if (bytesReceivers.containsKey(key)) {
            receiver = bytesReceivers.get(key);
        } else {
            //没有这个接收器，证明原则上应该是首包才对，如果不是首包还没找到接收器，则直接忽视这个包，应该是之前包的遗漏部分。
            if (pack.length >= 3 && pack[1] == 120 && pack[2] == 110) {
                BytesRecevier newReceiver = new BytesRecevier(key, requestIndex, device, characteristicUuid);
                newReceiver.setCallback(new BytesRecevier.BytesReceiveCallback() {
                    private void sendResult(BluetoothDevice device, UUID characteristicUuid, byte requestIndex, byte result) {
                        BluetoothGattServer bluetoothGattServer = getBluetoothGattServer();
                        BluetoothGattCharacteristic characteristic = getCharacteristic(characteristicUuid);
                        characteristic.setValue(new byte[]{120, 120, requestIndex, result});
                        bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                    }

                    @Override
                    public void onReceive(BluetoothDevice device, UUID characteristicUuid, byte requestIndex, byte[] data) {
                        BleLogger.log(TAG, "Received long bytes(length: " + data.length + ") with index: " + requestIndex + " from {device: " + device.getAddress().toString() + ", characteristic: " + characteristicUuid.toString() + "}.");
                        sendResult(device, characteristicUuid, requestIndex, (byte) 0);
                        onReceiveBytes(device, characteristicUuid, data);
                    }

                    @Override
                    public void onError(BluetoothDevice device, UUID characteristicUuid, byte requestIndex) {
                        BleLogger.log(TAG, "Receive long bytes error with index: " + requestIndex + " from {device: " + device.getAddress().toString() + ", characteristic: " + characteristicUuid.toString() + "}.");
                        sendResult(device, characteristicUuid, requestIndex, (byte) 1);
                    }

                    @Override
                    public void onTimeout(BluetoothDevice device, UUID characteristicUuid, byte requestIndex) {
                        BleLogger.log(TAG, "Receive long bytes timeout with index: " + requestIndex + " from {device: " + device.getAddress().toString() + ", characteristic: " + characteristicUuid.toString() + "}.");
                        sendResult(device, characteristicUuid, requestIndex, (byte) 2);
                    }

                    @Override
                    public void onFinish(BluetoothDevice device, UUID characteristicUuid, String key) {
                        bytesReceivers.remove(key);
                    }
                });
                bytesReceivers.put(key, newReceiver);
                receiver = newReceiver;
            }
        }
        if (receiver != null) {
            receiver.addPackage(pack);
        }
    }

    /**
     * 接收到了数据
     *
     * @param device
     * @param characteristicUuid
     * @param receivedData
     */
    private void onReceiveBytes(BluetoothDevice device, UUID characteristicUuid, byte[] receivedData) {
        if (isRequestBytesCharacteristic(characteristicUuid)) {
            if (receivedData.length >= 3 && receivedData[0] == 88 && receivedData[1] == 99) {
                byte reqeustId = receivedData[2];
                byte[] requestingData = new byte[receivedData.length - 3];
                ByteBuffer buffer = ByteBuffer.wrap(receivedData);
                buffer.position(3);
                buffer.get(requestingData, 0, receivedData.length - 3);
                byte[] response = onRequestBytes(device, characteristicUuid, requestingData);
                byte[] finalResponse = new byte[response.length + 3];
                finalResponse[0] = 99;
                finalResponse[1] = 88;
                finalResponse[2] = reqeustId;
                buffer = ByteBuffer.wrap(response);
                buffer.position(0);
                buffer.get(finalResponse, 3, response.length);
                //将请求结果发送给主设备
                writeBytes(device, characteristicUuid, finalResponse, null);
            }
        } else {
            BleCentralDeviceBase clientDevice = getDevice(device);
            if (clientDevice != null) {
                clientDevice.onReceiveBytes(characteristicUuid, receivedData);
            }
        }
    }

    private final HashMap<String, BytesWriter> bytesWriters = new HashMap<>();
    private final HashMap<String, Byte> indexMap = new HashMap<>();

    private byte getIndex(String type) {
        Byte existIndex = -128;
        if (indexMap.containsKey(type)) {
            existIndex = indexMap.get(type);
        }
        existIndex++;
        if (existIndex == 127) {
            existIndex = -128;
        }
        indexMap.put(type, existIndex);
        return existIndex;
    }

    /**
     * 写数据
     *
     * @param characteristicUuid
     * @param data
     * @param callback
     */
    public void writeBytes(BluetoothDevice device, UUID characteristicUuid, byte[] data, BytesWriter.WriteBytesCallback callback) {
        if (isWriteBytesCharacteristic(characteristicUuid)) {
            byte writeIndex = getIndex("writeBytes");
            String key = BytesWriter.createKey(device, characteristicUuid, writeIndex);
            BluetoothGattServer bluetoothGattServer = this.getBluetoothGattServer();
            BluetoothGattCharacteristic characteristic = this.getCharacteristic(characteristicUuid);
            BytesWriter writer = new BytesWriter(writeIndex, getPackageSize(), key, device, bluetoothGattServer, characteristic);
            bytesWriters.put(key, writer);
            writer.setCallback(new BytesWriter.WriteBytesCallback() {
                @Override
                public void onSent() {
                    if (callback != null) {
                        callback.onSent();
                    }
                }

                @Override
                public void onError() {
                    if (callback != null) {
                        callback.onError();
                    }
                }

                @Override
                public void onTimeout() {
                    if (callback != null) {
                        callback.onTimeout();
                    }
                }

                @Override
                public void onFinish(String key) {
                    bytesWriters.remove(key);
                    if (callback != null) {
                        callback.onFinish(key);
                    }
                }
            });
            writer.writeBytes(data);
        }
    }

    /**
     * 所有使用设备得地方都会先调用这个方法
     *
     * @param device
     * @param characteristicUuid
     * @return
     */
    protected boolean doFilterDevice(BluetoothDevice device, UUID characteristicUuid) {
        return true;
    }

    /**
     * 取消连接某个设备
     *
     * @param device
     */
    public void disconnect(BluetoothDevice device) {
        BluetoothGattServer bluetoothGattServer = this.getBluetoothGattServer();
        bluetoothGattServer.cancelConnection(device);
        onRemoveDevice(device);
    }

    /**
     * 取消连接某个设备
     *
     * @param device
     */
    public void disconnect(T device) {
        this.disconnect(device.getDevice());
    }

    /**
     * 取消连接所有的蓝牙设备
     */
    public void disconnectAll() {
        List<T> devices = this.getDevices();
        for (T device : devices) {
            this.disconnect(device);
        }
    }


    // BLE服务端Callback
    private final BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            BleLogger.log(TAG, status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED ? "Connected to " + device.getAddress() : "Disconnected from " + device.getAddress());
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // 建立双向连接，这样如果服务端断开，客户端也是能收到事件更新的
                bluetoothGattServer.connect(device, false);
                if (deviceMap.containsKey(device.getAddress())) {
                    onUpdateDevice(device);
                } else {
                    onAddDevice(device);
                }
            } else {
                // 如果连接失败就取消连接
                bluetoothGattServer.cancelConnection(device);
                if (deviceMap.containsKey(device.getAddress())) {
                    onRemoveDevice(device);
                }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            BleLogger.log(TAG, status == 0 ? "Add service " + service.getUuid() + " succeeded." : "Add service " + service.getUuid() + " failed with status: " + status + ".");
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (!doFilterDevice(device, characteristic.getUuid())) {
                disconnect(device);
                return;
            }
            BleLogger.log(TAG, String.format("onCharacteristicReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, characteristic.getUuid()));
            self.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestingData) {
            if (!doFilterDevice(device, characteristic.getUuid())) {
                disconnect(device);
                return;
            }
            // 获取客户端发过来的数据
            BleLogger.log(TAG, String.format("onCharacteristicWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
                    preparedWrite, responseNeeded, offset, BytesUtil.bytesToHex(requestingData)));
            self.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, requestingData);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            BleLogger.log(TAG, String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null); // 响应客户端
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestingData) {
            BleLogger.log(TAG, String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, descriptor.getUuid(),
                    preparedWrite, responseNeeded, offset, BytesUtil.bytesToHex(requestingData)));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestingData);// 响应客户端
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            BleLogger.log(TAG, String.format("onExecuteWrite:%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, execute));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            BleLogger.log(TAG, String.format("onNotificationSent:%s,%s,%s", device.getName(), device.getAddress(), status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            packageSize = mtu - 3;
            BleLogger.log(TAG, String.format("onMtuChanged:%s,%s,%s", device.getName(), device.getAddress(), mtu));
        }
    };
}
