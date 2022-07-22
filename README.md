# Android BleEx library

服务于 Flutter 的 [ble_ex](https://github.com/featherJ/ble_ex) 库的一个用于建立 Android 外围设备的 BLE 库。可以通过该库轻松搭建 Android 的外围设备，并支持 [ble_ex](https://github.com/featherJ/ble_ex) 中提供的所有特性。

## 功能简介
可以通过简单的参数配置一个 BLE 服务，并监听连入到该服务的中心设备代理。之后对于设备间的通信均可以通过上述得到的中心设备代理进行操作。让开发过程更加清晰，结构分明。

在原生 BLE 功能的基础上，为 Android 和 iOS 作为中心设备可能会表现不一致的地方做了处理。如，使用 iOS 中心设备订阅指定特征通知的之前，该特征必须添加 `UUID` 为 `00002902-0000-1000-8000-00805f9b34fb` 的描述才可以使用，否则会引发报错。但 Android 作为中心设备边无需此描述。该 BleEx 库会对指定的特征权限进行判断，内部自动进行这些配置。

### 功能
- 封装了中心设备。
- 主动断开中心设备的功能。
- 增加了对于带有数据的请求的支持，可以返回给中心设备特定的数据（类似于 Http 的 Post 的服务端功能），数据长度受到 `mtu` 限制。
- 增加了主动写长数据给中心设备的功能，并可以收到写入成功的回馈，数据长度不受 `mtu` 限制。
- 增加了接收长数据的功能，数据长度不受 `mtu` 限制。
- 增加了对于带有长数据的请求的支持，可以返回给中心设备特定的长数据（类似于 Http 的 Post 的服务端功能），数据长度不受 `mtu` 限制。

## 使用
### 初始化
中心设备代理要继承自 `BleCentralDeviceBase` 实现自己的重新设备，如：
```java
public class BleCentralDevice extends BleCentralDeviceBase
```

在设备子类中，可以重写如`onCharacteristicReadRequest`, `onCharacteristicWriteRequest`, `onRequest`, `onRequestBytes`, `onReceiveBytes` 等方法来完成自己的业务逻辑。

服务要继承自 `BleServiceBase` 实现自己的 BLE 服务，并指定对应的中心设备类型，如：
```java
public class BleService extends BleServiceBase<BleCentralDevice>
```

在服务子类中，最重要的是重写 `createCentralDevice` 方法, 实现对于特定中心设备的实例化方法，如：
```java
@Override
protected BleCentralDeviceBase createCentralDevice(BluetoothDevice device) {
    return new BleCentralDevice(device, this);
}
```

### 启动服务
需要先添加好服务所需要的所有特征之后，在启动服务，可以通过如下方法来添加服务所支持的特征：
```java
// 添加一个普通的特征，如果添加的特征为通知，则内部会自动增加通知所需的描述
BluetoothGattCharacteristic addCharacteristic(UUID uuid, int properties, int permissions);
// 添加一个支持数据请求的特征，数据长度受 mtu 限制。
BluetoothGattCharacteristic addRequestCharacteristic(UUID uuid);
// 添加一个接受长数据的特征，数据长度不受 mtu 限制。
BluetoothGattCharacteristic addReceiveBytesCharacteristic(UUID uuid);
// 添加一个写长数据到中心设备的特征，数据长度不受 mtu 限制。
BluetoothGattCharacteristic addWriteBytesCharacteristic(UUID uuid);
// 添加一个支持长数据请求的特征，数据长度不受 mtu 限制。
BluetoothGattCharacteristic addRequestBytesCharacteristic(UUID uuid);
```
当初始化好所有的特征之后，可以调用如下方法启动广播：
```java
void startAdvertising(boolean includeName, byte[] manufacturerData, boolean connectable);
```
以及通过如下方法启动服务：
```java
void startService();
```

### 支持中心设备的数据读取
需要重写 `BleCentralDeviceBase` 的 `onCharacteristicReadRequest` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected byte[] onCharacteristicReadRequest(UUID characteristicUuid) {
    // 判断是指定的特征，该特征需要已经被添加到了服务中
    if (characteristicUuid.equals(XXX)) {
        byte[] data ...
        // 完成自己要返回给中心设备的数据内容
        return data;
    }
    return super.onCharacteristicReadRequest(characteristicUuid);
}
```

### 支持中心设备的数据写入
需要重写 `BleCentralDeviceBase` 的 `onCharacteristicWriteRequest` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected void onCharacteristicWriteRequest(UUID characteristicUuid, byte[] requestingData) {
    // 判断是否是写入到了某个指定特征，该特征需要已经被添加到了服务中
    if (characteristicUuid.equals(XXX)) {
        // requestingData 就是中心设备写入的数据
    }
    super.onCharacteristicWriteRequest(characteristicUuid, requestingData);
}
```

### 支持中心设备的数据请求
需要重写 `BleCentralDeviceBase` 的 `onRequest` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected byte[] onRequest(UUID characteristicUuid, byte[] requestingData) {
    // 判断是否是对某个指定特征的请求，该特征需要已经被添加到了服务中
    if (characteristicUuid.equals(XXX)) {
        // requestingData 是中心设备请求的数据
        byte[] data ...
        // 完成自己要返回给中心设备的数据内容
        return data;
    }
    return super.onRequest(characteristicUuid, requestingData);
}
```

### 支持中心设备的长数据请求
长数据，不受到mtu的限制。需要重写 `BleCentralDeviceBase` 的 `onRequestBytes` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected byte[] onRequestBytes(UUID characteristicUuid, byte[] requestingData) {
    // 判断是否是对某个指定特征的请求，该特征需要已经被添加到了服务中
    if (characteristicUuid.equals(XXX)) {
        // requestingData 是中心设备请求的数据
        byte[] data ...
        // 完成自己要返回给中心设备的数据内容
        return data;
    }
    return super.onRequestBytes(characteristicUuid, requestingData);
}
```

### 支持接受中心设备的长数据
长数据，不受到mtu的限制。需要重写 `BleCentralDeviceBase` 的 `onReceiveBytes` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected void onReceiveBytes(UUID characteristicUuid, byte[] receivedData) {
    // 判断是否是写入到了某个指定特征，该特征需要已经被添加到了服务中
    if (characteristicUuid.equals(XXX)) {
        // receivedData 就是中心设备写入的长数据
    }
    super.onReceiveBytes(characteristicUuid, receivedData);
}
```

### 通知到中心设备数据
需要调用 `BleCentralDeviceBase` 的 `notifyCharacteristic` 方法来发送通知：
```java
void notifyCharacteristic(UUID characteristicUuid, byte[] data);
```

### 向中心设备写入长数据
长数据的写入，不受到 mtu 的限制。需要调用 `BleCentralDeviceBase` 的 `writeBytes` 向中心设备写入数据，并可以监听是数据的写入状态：
```java
writeBytes(UUID characteristicUuid, byte[] data, new BytesWriter.WriteBytesCallback() {
    @Override
    public void onSent() {
        // 写入数据完成
    }
    @Override
    public void onError() {
        // 写入数据错误
    }
    @Override
    public void onTimeout() {
        // 写入数据超时
    }
});
```
