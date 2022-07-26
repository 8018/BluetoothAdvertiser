package com.example.bluetoothadvertiser

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.*
import java.util.concurrent.TimeUnit


@SuppressLint("MissingPermission")
class BluetoothService : Service() {

    private val FOREGROUND_NOTIFICATION_ID = 1

    var running = false
    val ADVERTISENG_FILED = "com.examle.lihong.bluetoothadvertisement.advertising_failed"
    val ADVERTISING_FAILED_EXTRA_CODE = "failureCode"
    val ADVERTISING_TIMED_OUT = 6

    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var mHandler: Handler? = null
    private val TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)

    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var characteristicRead: BluetoothGattCharacteristic? = null
    var mBluetoothManager: BluetoothManager? = null

    override fun onCreate() {
        super.onCreate()
        goForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        initialize()
        startAdvertising()
        setTimeout()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        stopAdvertising()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initialize() {
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager != null) {
                val bluetoothAdapter = mBluetoothManager!!.adapter
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
                } else {
                    Toast.makeText(this, "设备不支持蓝牙广播", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setTimeout() {
        /*mHandler = Handler()
        timeoutRunnable = Runnable {
            Log.d(Companion.TAG, "广播服务已经运行" + TIMEOUT + "秒，停止停止广播")
            sendFailureIntent(ADVERTISING_TIMED_OUT)
            stopSelf()
        }
        mHandler!!.postDelayed(timeoutRunnable, TIMEOUT)*/
    }

    private fun startAdvertising() {
        Log.d(TAG, "服务开始广播")
        val settings = buildAdvertiseSettings()
        val data = buildAdvertiseData()
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser!!.startAdvertising(settings, data,buildScanResponse(), mAdertiseCallback)
        }
    }

    private fun goForeground() {
        Log.d(TAG, "goForegroud运行过了")
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        }
    }

    private fun stopAdvertising() {
        Log.d(TAG, "服务停止广播")
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdertiseCallback)
        }
    }

    private fun buildAdvertiseData(): AdvertiseData {

        //广播数据(必须，广播启动就会发送)
        //广播数据(必须，广播启动就会发送)
        return AdvertiseData.Builder()
            .setIncludeDeviceName(true) //包含蓝牙名称
            .setIncludeTxPowerLevel(true) //包含发射功率级别
            .addManufacturerData(1, byteArrayOf(23, 33)) //设备厂商数据，自定义
            .build()

    }

    private fun buildScanResponse(): AdvertiseData {

        //扫描响应数据(可选，当客户端扫描时才发送)
        return AdvertiseData.Builder()
            .addManufacturerData(2, byteArrayOf(66, 66)) //设备厂商数据，自定义
            .addServiceUuid(Constants.Service_UUID) //服务UUID
            //                .addServiceData(new ParcelUuid(UUID_SERVICE), new byte[]{2}) //服务数据，自定义
            .build()
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        val settingsBuilder = AdvertiseSettings.Builder()
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        settingsBuilder.setConnectable(true)
        settingsBuilder.setTimeout(0)
        return settingsBuilder.build()
    }

    private var mAdertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d(TAG, "广播失败")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "服务端的广播成功开启")
            Log.d(
                TAG,
                "BLE服务的广播启动成功后：TxPowerLv=" + settingsInEffect.txPowerLevel + "；mode=" + settingsInEffect.mode + "；timeout=" + settingsInEffect.timeout
            )
            initServices() //该方法是添加一个服务，在此处调用即将服务广播出去
        }
    }

    private fun sendFailureIntent(errorCode: Int) {
        val failureIntent = Intent()
        failureIntent.action = ADVERTISENG_FILED
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode)
        sendBroadcast(failureIntent)
    }

    //添加一个服务，该服务有一个读特征、该特征有一个描述；一个写特征。
    //用BluetoothGattServer添加服务，并实现该类的回调接口
    private fun initServices() {
        var context = this
        mBluetoothGattServer =
            mBluetoothManager!!.openGattServer(context, bluetoothGattServerCallback)
        val service = BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        characteristicRead = BluetoothGattCharacteristic(
            UUID_CHARREAD,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val descriptor =
            BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE)
        characteristicRead!!.addDescriptor(descriptor)
        service.addCharacteristic(characteristicRead)
        val characteristicWrite = BluetoothGattCharacteristic(
            UUID_CHARWRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicWrite)
        mBluetoothGattServer?.addService(service)
        LogUtil.writeMessage(context, "初始化服务成功：initServices ok")
    }

    //服务事件的回调
    private val bluetoothGattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            //1、首先是连接状态的回调
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                when(newState){
                    BluetoothProfile.STATE_CONNECTED->{
                        LogUtil.writeMessage(this@BluetoothService,
                            "外设已连接 device is $device "
                        )
                    }
                    BluetoothProfile.STATE_DISCONNECTED->{
                        LogUtil.writeMessage(this@BluetoothService,
                            "外设已断开 device "
                        )
                    }
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                LogUtil.writeMessage(this@BluetoothService, "客户端有读的请求，安卓系统回调该onCharacteristicReadRequest()方法")
                mBluetoothGattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    characteristic.value
                )
            }

            //接受具体字节，当有特征被写入时，回调该方法，写入的数据为参数中的value
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                LogUtil.writeMessage(this@BluetoothService, "客户端有写的请求，安卓系统回调该onCharacteristicWriteRequest()方法")

                //特征被读取，在该回调方法中回复客户端响应成功
                mBluetoothGattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )

                //处理响应内容
                //value:客户端发送过来的数据
                onResponseToClient(value, device, requestId, characteristic)
            }

            //特征被读取。当回复相应成功后，客户端胡读取然后触发本方法
            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)
                mBluetoothGattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    null
                )
            }

            //2、其次，当有描述请求被写入时，回调该方法，
            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                mBluetoothGattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
                // onResponseToClient(value,device,requestId,descriptor.getCharacteristic());
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                super.onServiceAdded(status, service)
                LogUtil.writeMessage(this@BluetoothService, "添加服务成功，安卓系统回调该onServiceAdded()方法")
            }
        }

    //4.处理相应内容,requestBytes是客户端发送过来的数据
    private fun onResponseToClient(
        requestBytes: ByteArray,
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
        //在服务端接受数据
        val msg = OutputStringUtil.transferForPrint(*requestBytes)
        LogUtil.writeMessage(this, "收到：$msg")
        //响应客户端
        val str = String(requestBytes) + "hello>"
        characteristicRead!!.value = str.toByteArray()
        mBluetoothGattServer!!.notifyCharacteristicChanged(
            device,
            characteristicRead,
            false
        ) //用该方法通知characteristicRead数据发生改变
    }

    private fun getContext(): Context? {
        return this
    }

    companion object {
        private const val TAG = "xflyme"
        private val UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARREAD = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        private val UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

}