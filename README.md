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

## 安装
### 第一步
在 `settings.gradle` 文件中的 `dependencyResolutionManagement` 下的 `repositories` 中添加 ` maven { url "https://jitpack.io" }`，如:
```gradle
dependencyResolutionManagement {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
或在根目录的 `build.gradle` 的结尾添加
```gradle
allprojects {
	repositories {
		...
		maven { url 'https://www.jitpack.io' }
	}
}
```
### 第二步
在项目的 `build.gradle` 中添加依赖：
```gradle
dependencies {
    implementation 'com.github.featherJ:BleEx:0.9.8'
}
```

## 使用
### 初始化
中心设备代理要继承自 `BleCentralDeviceBase` 实现自己的重新设备，如：
```java
public class BleCentralDevice extends BleCentralDeviceBase
```

在设备子类中，可以重写如`onRead`, `onWrite`, `onRequest`, `onRequestLarge`, `onWriteLarge` 等方法来完成自己的业务逻辑。

服务要继承自 `BleServicesBase` 实现自己的 BLE 服务，并指定对应的中心设备类型，如：
```java
public class BleServices extends BleServicesBase<BleCentralDevice>
```

在服务子类中，最重要的是重写 `createCentralDevice` 方法, 实现对于特定中心设备的实例化方法，如：
```java
@Override
protected BleCentralDeviceBase createCentralDevice(BluetoothDevice device) {
    return new BleCentralDevice(device, this, this.context);
}
```

### 启动服务
首先需要添加服务
```java
// 添加并启动一个服务
addService(UUID service);
// 添加一个服务
addService(UUID service, boolean start);
```

再先添加好服务所需要的所有特征，可以通过如下方法来添加服务所支持的特征：
```java
// 添加一个普通的特征，如果添加的特征为通知，则内部会自动增加通知所需的描述
BluetoothGattCharacteristic addCharacteristic(UUID service, UUID characteristic, int properties, int permissions);
// 添加一个中心设备向外围设备请求一个MTU以内数据的特征
BluetoothGattCharacteristic addRequestCharacteristic(UUID service, UUID characteristic);
// 添加一个中心设备向外围设备请求一个长数据的特征
BluetoothGattCharacteristic addRequestLargeCharacteristic(UUID service, UUID characteristic);
// 添加一个中心设备向外围设备写入一个长数据的特征
BluetoothGattCharacteristic addWriteLargeCharacteristic(UUID service, UUID characteristic);
// 添加一个外围设备向中心设备指示一个长数据的特征
BluetoothGattCharacteristic addIndicateLargeCharacteristic(UUID service, UUID characteristic);
```
当初始化好所有的特征之后，可以调用如下方法启动或停止广播：
```java
// 启动或更新BLE蓝牙广播(广告)
void startAdvertising(boolean includeName, boolean includeFirstService, byte[] manufacturerData, boolean connectable);
// 停止BLE蓝牙广播(广告)
void stopAdvertising();
```
以及通过如下方法启动或关闭服务：
```java
// 启动该管理器，需要确保蓝牙开启的状态下才能启动
void launch();
// 停止该管理器
void stop()
```
可以通过如下方法监听蓝牙的开启状态变化，并在对应的状态下启动或关闭该蓝牙服务
```java
bleService.addBluetoothStateChangedListener(new BluetoothStateChangedCallback() {
    @Override
    public void onStateChanged(boolean enable) {
        if (enable) {
            try {
                bleService.startAdvertising();
                bleService.launch();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            bleService.stopAdvertising();
            bleService.stop();
        }
    }
});
if (bleService.isBluetoothEnable()) {
    try {
        bleService.startAdvertising();
        bleService.launch();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```
### 支持中心设备的数据读取
需要重写 `BleCentralDeviceBase` 的 `onRead` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected byte[] onRead(UUID service, UUID characteristic) {
    // 判断是指定的特征，该特征需要已经被添加到了服务中
    if (characteristic.equals(XXX)) {
        byte[] data ...
        // 完成自己要返回给中心设备的数据内容
        return data;
    }
    return super.onRead(service, characteristic);
}
```

### 支持中心设备的数据写入
需要重写 `BleCentralDeviceBase` 的 `onWrite` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected void onWrite(UUID service, UUID characteristic, byte[] data) {
    // 判断是否是写入到了某个指定特征，该特征需要已经被添加到了服务中
    if (characteristic.equals(XXX)) {
        // data 就是中心设备写入的数据
    }
    super.onWrite(service, characteristic, data);
}
```

### 支持中心设备的数据请求
需要重写 `BleCentralDeviceBase` 的 `onRequest` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected byte[] onRequest(UUID service, UUID characteristic, byte[] data) {
    // 判断是否是对某个指定特征的请求，该特征需要已经被添加到了服务中
    if (characteristic.equals(XXX)) {
        // data 是中心设备请求的数据
        byte[] data ...
        // 完成自己要返回给中心设备的数据内容
        return data;
    }
    return super.onRequest(service, characteristic, data);
}
```

### 支持中心设备的长数据请求
长数据，不受到mtu的限制。需要重写 `BleCentralDeviceBase` 的 `onRequestLarge` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected byte[] onRequestLarge(UUID service, UUID characteristic, byte[] data) {
    // 判断是否是对某个指定特征的请求，该特征需要已经被添加到了服务中
    if (characteristic.equals(XXX)) {
        // data 是中心设备请求的数据
        byte[] data ...
        // 完成自己要返回给中心设备的数据内容
        return data;
    }
    return super.onRequestLarge(service, characteristic, data);
}
```

### 支持接受中心设备的长数据
长数据，不受到mtu的限制。需要重写 `BleCentralDeviceBase` 的 `onWriteLarge` 方法，在重写的方法中完成自己的业务逻辑。如：
```java
@Override
protected void onWriteLarge(UUID service, UUID characteristic, byte[] data) {
    // 判断是否是写入到了某个指定特征，该特征需要已经被添加到了服务中
    if (characteristic.equals(XXX)) {
        // data 就是中心设备写入的长数据
    }
    super.onWriteLarge(service, characteristic, data);
}
```

### 通知到中心设备数据
需要调用 `BleCentralDeviceBase` 的 `notify`/`indicate` 方法来发送通知/指示：
```java
void notify(UUID service, UUID characteristic, byte[] data);
void indicate(UUID service, UUID characteristic, byte[] data);
```

### 向中心设备写入长数据
长数据的写入，不受到 mtu 的限制。需要调用 `BleCentralDeviceBase` 的 `indicateLarge` 向中心设备指示长数据：
```java
void indicateLarge(UUID service, UUID characteristic, byte[] data);
```
