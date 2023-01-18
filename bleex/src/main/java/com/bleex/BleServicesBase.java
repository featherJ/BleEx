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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import com.bleex.consts.DataTags;
import com.bleex.helpers.BytesReceiver;
import com.bleex.helpers.BytesWriter;
import com.bleex.utils.BytesUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * BleEx服务基类
 *
 * @author Agua.L
 */
@SuppressLint("MissingPermission")
public class BleServicesBase<T extends BleCentralDeviceBase> {
    private static final String TAG = "BleServicesBase";

    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeAdvertiser bluetoothLeAdvertiser; // BLE广播
    protected final Context context;

    protected BleServicesBase self;

    private int packageSize = 20;

    /**
     * 可发送的包大小
     */
    public int getPackageSize() {
        return packageSize;
    }

    public BleServicesBase(Context context) {
        this.self = this;
        this.context = context;

        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        BroadcastReceiver broadcastReceiver = new BluetoothStateBroadcastReceive();
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, intent);
    }

    class BluetoothStateBroadcastReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (blueState == BluetoothAdapter.STATE_OFF) {
                    BleLogger.log(TAG, "Bluetooth disabled");
                    onBluetoothChanged(false);
                } else if (blueState == BluetoothAdapter.STATE_ON) {
                    BleLogger.log(TAG, "Bluetooth enabled");
                    onBluetoothChanged(true);
                }
            }
        }
    }

    public boolean isBluetoothEnable() {
        return this.bluetoothAdapter.isEnabled();
    }

    private final List<BluetoothStateChangedCallback> bluetoothStateChangedCallbacks = new ArrayList<>();

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

    private void onBluetoothChanged(boolean enable) {
        for (BluetoothStateChangedCallback callback : bluetoothStateChangedCallbacks) {
            callback.onStateChanged(enable);
        }
    }


    /* ------------------------------ 服务相关 ------------------------------ */


    private HashMap<String, BluetoothGattServer> serverMap = null;
    private final HashMap<String, ServicesCallback> serverCallbackMap = new HashMap<>();
    private final HashMap<String, BluetoothGattService> serviceMap = new HashMap<>();
    private final List<UUID> startedServiceUuids = new ArrayList<>();

    private void ensureServer(UUID service) throws Exception {
        if (serverMap == null) {
            throw new Exception("Unable to ensure server while stopping, you need to launch first.");
        }
        if (!this.serverMap.containsKey(service.toString())) {
            ServicesCallback callback = new ServicesCallback(service);
            BluetoothGattServer server = bluetoothManager.openGattServer(context, callback);
            callback.initServer(server);
            this.serverMap.put(service.toString(), server);
            this.serverCallbackMap.put(service.toString(), callback);
            ensureMainServerCallback();
        }
    }

    private BluetoothGattServer getServer(UUID service) throws Exception {
        if (serverMap == null) {
            throw new Exception("Unable to get server while stopping, you need to launch first.");
        }
        BluetoothGattServer server = this.serverMap.get(service.toString());
        return server;
    }

    private void removeServer(UUID service) throws Exception {
        if (serverMap == null) {
            throw new Exception("Unable to get server while stopping, you need to launch first.");
        }
        this.serverMap.remove(service.toString());
    }

    private void ensureMainServerCallback() {
        boolean hasMain = false;
        Iterator<ServicesCallback> iterator = serverCallbackMap.values().iterator();
        while (iterator.hasNext()) {
            ServicesCallback value = iterator.next();
            if (value.isMain) {
                hasMain = true;
                break;
            }
        }
        if (hasMain) {
            return;
        }
        if (serverCallbackMap.size() == 0) {
            return;
        }
        ServicesCallback firsrCallback = serverCallbackMap.values().iterator().next();
        firsrCallback.isMain = true;
    }

    /**
     * 启动该管理器，用于检测到蓝牙
     */
    public void launch() throws Exception {
        if (isBluetoothEnable()) {
            throw new Exception("Cannot launch while Bluetooth is disable");
        }
        serverMap = new HashMap<>();
        for (int i = 0; i < startedServiceUuids.size(); i++) {
            UUID service = startedServiceUuids.get(i);
            this.doStartService(service);
        }
    }

    /**
     * 停止该管理器
     */
    public void stop() {
        disconnectAll();
        for (int i = 0; i < startedServiceUuids.size(); i++) {
            UUID service = startedServiceUuids.get(i);
            try {
                this.doCloseService(service);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        serverMap = null;
    }

    /**
     * 是否在运行中
     *
     * @return
     */
    public boolean getIsRunning() {
        return serverMap != null;
    }


    private List<UUID> services = new ArrayList<>();
    /**
     * 添加一个服务（不可与主服务相同）
     *
     * @param service
     */
    public void addService(UUID service) throws Exception {
        this.addService(service,false);
    }

    /**
     * 添加一个服务（不可与主服务相同）
     * @param service
     * @param start
     * @throws Exception
     */
    public void addService(UUID service, boolean start) throws Exception {
        if (this.serviceMap.containsKey(service.toString())) {
            throw new Exception("Service " + service + " already exists.");
        }
        services.add(service);
        this.serviceMap.put(service.toString(), new BluetoothGattService(service, BluetoothGattService.SERVICE_TYPE_PRIMARY));
        if (start) {
            this.startService(service);
        }
    }

    /**
     * 启动指定服务
     *
     * @param service
     */
    public void startService(UUID service) throws Exception {
        if (!this.serviceMap.containsKey(service.toString())) {
            throw new Exception("Service " + service + " does not exist. You need to addService first");
        }
        this.startedServiceUuids.add(service);
        this.doStartService(service);
    }

    private void doStartService(UUID service) throws Exception {
        if (this.getIsRunning()) {
            BluetoothGattService serviceTarget = this.serviceMap.get(service.toString());
            ensureServer(service);
            getServer(service).addService(serviceTarget);
            BleLogger.log(TAG, "Service " + service + " added");
        }
    }

    /**
     * 移除一个服务（不可以移除主服务）
     */
    public void removeService(UUID service) throws Exception {
        if (!this.serviceMap.containsKey(service.toString())) {
            throw new Exception("Service " + service + " does not exist.");
        }
        services.remove(service);
        closeService(service);
        this.serviceMap.remove(service.toString());
    }

    /**
     * 关闭指定服务
     *
     * @param service
     */
    public void closeService(UUID service) throws Exception {
        this.startedServiceUuids.remove(service);
        doCloseService(service);
    }

    private void doCloseService(UUID service) throws Exception {
        if (this.getIsRunning()) {
            BluetoothGattService serviceTarget = this.serviceMap.get(service.toString());
            BluetoothGattServer server = getServer(service);
            if (server != null) {
                if (serviceTarget != null) {
                    server.removeService(serviceTarget);
                }
                server.close();
            }
            removeServer(service);
        }
    }

    /* ------------------------------ 广播相关 ------------------------------ */

    private boolean isAdvertising = false;

    /**
     * 是否正在广播
     */
    public boolean getIsAdvertising() {
        return isAdvertising;
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
     * 启动或更新BLE蓝牙广播(广告)
     *
     * @param includeName         是否包含蓝牙名称
     * @param includeFirstService 是否包含第一个服务的uuid
     * @param manufacturerData    包含的设备信息
     * @param connectable         是否可以连接
     */
    public void startAdvertising(boolean includeName, boolean includeFirstService, byte[] manufacturerData, boolean connectable) {
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
        AdvertiseData.Builder advertiseBuider = new AdvertiseData.Builder()
                .setIncludeDeviceName(includeName) //包含蓝牙名称，有的设备蓝牙名特别长，直接关了得了
                .setIncludeTxPowerLevel(false); //包含发射功率级别
        if (services.size() > 0 && includeFirstService) {
            //多个uuid会导致长度过长，广播失败
            advertiseBuider.addServiceUuid(new ParcelUuid(this.services.get(0)));
        }
        AdvertiseData advertiseData = advertiseBuider.build();

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

    /* ------------------------------ 设备相关 ------------------------------ */

    private final HashMap<String, T> deviceMap = new HashMap<>();

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
        return new BleCentralDeviceBase(device, self, self.context);
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
     * 所有使用设备得地方都会先调用这个方法
     *
     * @param device
     * @param service
     * @param characteristic
     * @return
     */
    protected boolean doFilterDevice(BluetoothDevice device, UUID service, UUID characteristic) {
        return true;
    }

    /**
     * 取消连接某个设备
     *
     * @param device
     */
    public void disconnect(BluetoothDevice device) {
        if (getIsRunning()) {
            this.serverMap.forEach((service, server) -> {
                server.cancelConnection(device);
            });
        }
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

    /* ------------------------------ 特征相关 ------------------------------ */

    /**
     * 添加一个 Characteristic
     *
     * @param characteristic
     * @param properties
     * @param permissions
     * @param service
     */
    public BluetoothGattCharacteristic addCharacteristic(UUID service, UUID characteristic, int properties, int permissions) throws Exception {
        BluetoothGattService serverTarget = serviceMap.get(service.toString());
        if (serverTarget == null) {
            throw new Exception("Service " + service + " dose not exist");
        }
        BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(characteristic,
                properties, permissions);
        //如果包含通知，则需要加通知描述，不然苹果监听通知会报错
        if (
                (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                        || (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        ) {
            //TODO 测试加密情况下的传输
            BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                configDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            } else if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                configDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            characteristicRead.addDescriptor(configDescriptor);
        }
        serverTarget.addCharacteristic(characteristicRead);
        BleLogger.log(TAG, "Characteristic " + characteristic + " added to " + service);
        return characteristicRead;
    }

    private HashMap<String, HashMap<String, ArrayList<BluetoothGattCharacteristic>>> characteristicsRecordMap = new HashMap<>();
    private static String REQUEST_C_KEY = "requestCKey"; //中心设备向外围设备请求一个MTU以内的包
    private static String REQUEST_LARGE_C_KEY = "requestBytesCKey"; //中心设备向外围设备请求一个大数据包
    private static String WRITE_LARGE_C_KEY = "writeBytesCKey"; //中心设备向外围设备写入一个大的数据包
    private static String INDICATE_LARGE_C_KEY = "indicateBytesCKey"; //外围设备向中心设备指示一个大的数据包

    private ArrayList<BluetoothGattCharacteristic> getRecordCharacteristics(String key, UUID service) {
        HashMap<String, ArrayList<BluetoothGattCharacteristic>> characteristicsMap = characteristicsRecordMap.get(key);
        if (characteristicsMap == null) {
            characteristicsMap = new HashMap<>();
            characteristicsRecordMap.put(key, characteristicsMap);
        }
        ArrayList<BluetoothGattCharacteristic> recordCharacteristics = characteristicsMap.get(service.toString());
        if (recordCharacteristics == null) {
            recordCharacteristics = new ArrayList<>();
            characteristicsMap.put(service.toString(), recordCharacteristics);
        }
        return recordCharacteristics;
    }

    private boolean isBelongCharacteristic(String key, UUID service, UUID characteristic) {
        ArrayList<BluetoothGattCharacteristic> requestCharacteristics = getRecordCharacteristics(key, service);
        for (int i = 0; i < requestCharacteristics.size(); i++) {
            if (requestCharacteristics.get(i).getUuid().equals(characteristic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 添加一个中心设备向外围设备请求一个MTU以内数据的特征
     *
     * @param characteristic
     * @param service
     * @return
     * @throws Exception
     */
    public BluetoothGattCharacteristic addRequestCharacteristic(UUID service, UUID characteristic) throws Exception {
        BluetoothGattCharacteristic requestCharacteristic = this.addCharacteristic(service, characteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        getRecordCharacteristics(REQUEST_C_KEY, service).add(requestCharacteristic);
        return requestCharacteristic;
    }

    /**
     * 添加一个中心设备向外围设备请求一个长数据的特征
     *
     * @param characteristic
     * @param service
     * @return
     * @throws Exception
     */
    public BluetoothGattCharacteristic addRequestLargeCharacteristic(UUID service, UUID characteristic) throws Exception {
        BluetoothGattCharacteristic requestBytesCharacteristic = this.addCharacteristic(service, characteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        getRecordCharacteristics(REQUEST_LARGE_C_KEY, service).add(requestBytesCharacteristic);
        return requestBytesCharacteristic;
    }

    /**
     * 添加一个中心设备向外围设备写入一个长数据的特征
     *
     * @param characteristic
     * @param service
     * @return
     * @throws Exception
     */
    public BluetoothGattCharacteristic addWriteLargeCharacteristic(UUID service, UUID characteristic) throws Exception {
        BluetoothGattCharacteristic receiveBytesCharacteristic = this.addCharacteristic(service, characteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        getRecordCharacteristics(WRITE_LARGE_C_KEY, service).add(receiveBytesCharacteristic);
        return receiveBytesCharacteristic;
    }

    /**
     * 添加一个外围设备向中心设备指示一个长数据的特征
     *
     * @param characteristic
     * @param service
     * @return
     * @throws Exception
     */
    public BluetoothGattCharacteristic addIndicateLargeCharacteristic(UUID service, UUID characteristic) throws Exception {
        BluetoothGattCharacteristic writeBytesCharacteristic = this.addCharacteristic(service, characteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        getRecordCharacteristics(INDICATE_LARGE_C_KEY, service).add(writeBytesCharacteristic);
        return writeBytesCharacteristic;
    }


    /* ------------------------------ 数据相关 ------------------------------ */

    /**
     * 接收到特征的读请求（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     * @param characteristic
     * @param service
     * @return
     */
    protected byte[] onCharacteristicReadRequest(BluetoothDevice device, UUID service, UUID characteristic) {
        BleCentralDeviceBase centralDevice = getDevice(device);
        byte[] responseData = null;
        if (centralDevice != null) {
            responseData = centralDevice.onRead(service, characteristic);
        }
        return responseData;
    }

    /**
     * 接收到特征的写请求（子类可以重写，可以自己实现校验设备的方法）
     *
     * @param device
     * @param characteristic
     * @param service
     * @param value
     */
    protected void onCharacteristicWriteRequest(BluetoothDevice device, UUID service, UUID characteristic, byte[] value) {
        if (isBelongCharacteristic(REQUEST_C_KEY, service, characteristic)) {
            if (value.length > 0) {
                //接受数据格式:第一个字节为请求id，后续为数据内容
                //返回数据：第一个字节为请求id，后续为数据内容
                byte requestId = value[0];
                byte[] requestingData = new byte[value.length - 1];
                for (int i = 1; i < value.length; i++) {
                    requestingData[i - 1] = value[i];
                }
                byte[] response = onRequest(device, service, characteristic, requestingData);
                byte[] finalResponse = new byte[response.length + 1];
                finalResponse[0] = requestId;
                for (int i = 0; i < response.length; i++) {
                    finalResponse[i + 1] = response[i];
                }
                //将请求结果发送给主设备
                try {
                    this.notifyCharacteristicChanged(device, service, characteristic, finalResponse, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        if (isBelongCharacteristic(WRITE_LARGE_C_KEY, service, characteristic) || isBelongCharacteristic(REQUEST_LARGE_C_KEY, service, characteristic)) {
            receivingDataPacket(device, service, characteristic, value);
            return;
        }
        BleCentralDeviceBase centralDevice = getDevice(device);
        if (centralDevice != null) {
            centralDevice.onWrite(service, characteristic, value);
        }
    }

    private final HashMap<String, BytesReceiver> bytesReceivers = new HashMap<>();

    private void receivingDataPacket(BluetoothDevice device, UUID service, UUID characteristic, byte[] pack) {
        byte requestIndex = -1;
        if (pack.length >= 1) {
            requestIndex = pack[0];
        } else {
            return;
        }
        String key = BytesReceiver.createKey(device, service, characteristic, requestIndex);
        BytesReceiver receiver = null;
        if (bytesReceivers.containsKey(key)) {
            receiver = bytesReceivers.get(key);
        } else {
            //没有这个接收器，证明原则上应该是首包才对，如果不是首包还没找到接收器，则直接忽视这个包，应该是之前包的遗漏部分。
            if (pack.length >= 3 && pack[1] == DataTags.MS_WRITE_LARGE[0] && pack[2] == DataTags.MS_WRITE_LARGE[1]) {
                BytesReceiver newReceiver = new BytesReceiver(key, requestIndex, device, service, characteristic);
                newReceiver.setCallback(new BytesReceiver.BytesReceiveCallback() {
                    @Override
                    public void onReceive(BluetoothDevice device, UUID service, UUID characteristic, byte requestIndex, byte[] data) {
                        BleLogger.log(TAG, "Received long bytes(length: " + data.length + ") with index: " + requestIndex + " from {device: " + device.getAddress() + ", service: " + service + ", characteristic: " + characteristic + "}.");
                        receivedData(device, service, characteristic, data);
                    }

                    @Override
                    public void onError(BluetoothDevice device, UUID service, UUID characteristic, byte requestIndex) {
                        BleLogger.log(TAG, "Receive long bytes error with index: " + requestIndex + " from {device: " + device.getAddress() + ", service: " + service + ", characteristic: " + characteristic + "}.");
                    }

                    @Override
                    public void onTimeout(BluetoothDevice device, UUID service, UUID characteristic, byte requestIndex) {
                        BleLogger.log(TAG, "Receive long bytes timeout with index: " + requestIndex + " from {device: " + device.getAddress() + ", service: " + service + ", characteristic: " + characteristic + "}.");
                    }

                    @Override
                    public void onFinish(BluetoothDevice device, UUID service, UUID characteristic, String key) {
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

    private void receivedData(BluetoothDevice device, UUID service, UUID characteristic, byte[] data) {
        //收到的长数据包，要么就是长请求，要么就是长写入，第一种情况符合长请求
        if (isBelongCharacteristic(REQUEST_LARGE_C_KEY, service, characteristic)) {
            if (data.length >= 3 && DataTags.MS_REQUEST_LARGE[0] == 88 && DataTags.MS_REQUEST_LARGE[1] == 99) {
                byte reqeustId = data[2];
                byte[] requestingData = new byte[data.length - 3];
                ByteBuffer buffer = ByteBuffer.wrap(data);
                buffer.position(3);
                buffer.get(requestingData, 0, data.length - 3);
                byte[] response = onRequestLarge(device, service, characteristic, requestingData);
                byte[] finalResponse = new byte[response.length + 3];
                finalResponse[0] = DataTags.SM_RESPONSE_LARGE[0];
                finalResponse[1] = DataTags.SM_RESPONSE_LARGE[1];
                finalResponse[2] = reqeustId;
                buffer = ByteBuffer.wrap(response);
                buffer.position(0);
                buffer.get(finalResponse, 3, response.length);
                //将请求结果发送给主设备
                try {
                    indicateLarge(device, service, characteristic, finalResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        BleCentralDeviceBase centralDevice = getDevice(device);
        if (centralDevice != null) {
            centralDevice.onWriteLarge(service, characteristic, data);
        }
    }


    /**
     * 收到了有应答的请求
     *
     * @param device
     * @param characteristic
     * @param service
     * @param value
     * @return
     */
    protected byte[] onRequest(BluetoothDevice device, UUID service, UUID characteristic, byte[] value) {
        BleCentralDeviceBase centralDevice = getDevice(device);
        if (centralDevice != null) {
            return centralDevice.onRequest(service, characteristic, value);
        }
        return new byte[]{0};
    }

    /**
     * 收到了有应答的长数据请求
     *
     * @param device
     * @param service
     * @param characteristic
     * @param value
     * @return
     */
    protected byte[] onRequestLarge(BluetoothDevice device, UUID service, UUID characteristic, byte[] value) {
        BleCentralDeviceBase centralDevice = getDevice(device);
        if (centralDevice != null) {
            return centralDevice.onRequestLarge(service, characteristic, value);
        }
        return new byte[]{0};
    }


    /**
     * 派发通知
     *
     * @param device
     * @param characteristic
     * @param service
     * @param value
     * @param confirm
     * @throws Exception
     */
    public void notifyCharacteristicChanged(BluetoothDevice device, UUID service, UUID characteristic, byte[] value, boolean confirm) throws Exception {
        BluetoothGattServer serverTarget = this.getServer(service);
        BluetoothGattService serviceTarget = this.serviceMap.get(service);
        BluetoothGattCharacteristic characteristicTarget = serviceTarget.getCharacteristic(characteristic);
        characteristicTarget.setValue(value);
        serverTarget.notifyCharacteristicChanged(device, characteristicTarget, confirm);
        BleLogger.log(TAG, String.format("notifyCharacteristicChanged:%s,%s,%s,%s", device.getName(), device.getAddress(), characteristic, BytesUtil.bytesToString(value, false)));
    }

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
     * 指示长数据(有反馈的长数据通知)
     *
     * @param device
     * @param service
     * @param characteristic
     * @param data
     */
    public void indicateLarge(BluetoothDevice device, UUID service, UUID characteristic, byte[] data) throws Exception {
        if (isBelongCharacteristic(REQUEST_LARGE_C_KEY, service, characteristic) || isBelongCharacteristic(INDICATE_LARGE_C_KEY, service, characteristic)) {
            byte writeIndex = getIndex("writeBytes");
            String key = BytesWriter.createKey(device, service, characteristic, writeIndex);
            BluetoothGattService serviceTarget = this.serviceMap.get(service);
            BluetoothGattCharacteristic characteristicTarget = serviceTarget.getCharacteristic(characteristic);
            BytesWriter writer = new BytesWriter(writeIndex, getPackageSize(), key, device, this, service, characteristic);
            writer.writeBytes(data);
        }
    }

    class ServicesCallback extends BluetoothGattServerCallback {
        final UUID service;
        BluetoothGattServer server;

        public boolean isMain = false;

        ServicesCallback(UUID service) {
            this.service = service;
        }

        public void initServer(BluetoothGattServer server) {
            this.server = server;
        }


        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            BleLogger.log(TAG, status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED ? "Connected to " + device.getAddress() : "Disconnected from " + device.getAddress());
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // 建立双向连接，这样如果服务端断开，客户端也是能收到事件更新的
                server.connect(device, false);
                onConnected(device);
            } else {
                // 如果连接失败就取消连接
                server.cancelConnection(device);
                onDisconnected(device);
            }
        }

        protected void onConnected(BluetoothDevice device) {
            if (!isMain) {
                return;
            }
            //TODO 这里还要再看下 因为会被调用很多次
            if (deviceMap.containsKey(device.getAddress())) {
                onUpdateDevice(device);
            } else {
                onAddDevice(device);
            }
        }

        protected void onDisconnected(BluetoothDevice device) {
            if (!isMain) {
                return;
            }
            //TODO 这里还要再看下 因为会被调用很多次
            if (deviceMap.containsKey(device.getAddress())) {
                onRemoveDevice(device);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (!isMain) {
                return;
            }
            BleLogger.log(TAG, status == 0 ? "Add service " + service.getUuid() + " succeeded." : "Add service " + service.getUuid() + " failed with status: " + status + ".");
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (!doFilterDevice(device, service, characteristic.getUuid())) {
                disconnect(device);
                return;
            }
            BleLogger.log(TAG, String.format("onCharacteristicReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, characteristic.getUuid()));
            byte[] responseData = self.onCharacteristicReadRequest(device, service, characteristic.getUuid());
            server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, responseData);// 响应客户端
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (!doFilterDevice(device, service, characteristic.getUuid())) {
                disconnect(device);
                return;
            }
            // 获取客户端发过来的数据
            BleLogger.log(TAG, String.format("onCharacteristicWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
                    preparedWrite, responseNeeded, offset, BytesUtil.bytesToString(value, false)));
            if (responseNeeded) {
                server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            }
            self.onCharacteristicWriteRequest(device, service, characteristic.getUuid(), value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            BleLogger.log(TAG, String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
            server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null); // 响应客户端
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            BleLogger.log(TAG, String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, descriptor.getUuid(),
                    preparedWrite, responseNeeded, offset, BytesUtil.bytesToString(value, false)));
            server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);// 响应客户端
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
    }
}
