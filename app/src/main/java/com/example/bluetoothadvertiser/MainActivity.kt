package com.example.bluetoothadvertiser

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothadvertiser.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    /**
     * 需要获取的权限列表
     */
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_ADVERTISE
    )


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fab.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                LogUtil.readLog(this@MainActivity)
            }
        }

        if(hasPermissions()){
            startBluetoothService()
            registerMessageReceiver()
        }

    }

    /**
     * 动态申请权限
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasPermissions(): Boolean {
        if (!isHasPermission()) {
            //动态申请权限 , 第二参数是请求吗
            requestPermissions(permissions, REQUEST_CODE)
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private

            /**
             * 判断是否有 permissions 中的权限
             *
             * @return
             */
    fun isHasPermission(): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                println("xflyme the permission is ${permission.toString()}")
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被用户同意,做相应的事情
                startBluetoothService();
                registerMessageReceiver()
            } else {
                //权限被用户拒绝，做相应的事情
                Toast.makeText(this, "您没有授予权限，无法使用 App", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBluetoothService() {
        Toast.makeText(this, "开启蓝牙功能", Toast.LENGTH_LONG).show()
        startService(Intent(this, BluetoothService::class.java))
    }


    companion object {
        private const val REQUEST_CODE = 102;
    }


    private fun registerMessageReceiver() {
        val filter = IntentFilter()
        filter.addAction("me.xfly.message")
        registerReceiver(messageReceiver, filter)
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.w("xflyme", "onReceive and message is ${intent?.getStringExtra("message")}")
            var message = "${binding.tv.text} ${intent?.getStringExtra("message")} \n";
            if (message.length >= 1000) {
                message = message.substring(message.length - 1000, message.length)
            }
            binding.tv.text = message
        }
    }
}