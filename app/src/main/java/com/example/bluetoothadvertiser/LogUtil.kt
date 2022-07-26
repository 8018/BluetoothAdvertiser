package com.example.bluetoothadvertiser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.*


object LogUtil {

    fun readLog(context: Context) {
        var path = "${context.filesDir}/log/${getCurrentDate()}.txt"
        var file = File(path);

        if (!file.exists()) {
            return
        }

        val lines: List<String> = File(path).readLines()
        lines.forEach { line -> println("xflyme the log is $line") }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun writeMessage(context: Context,message: String) {
        sendMessageToActivity(context,message)
        CoroutineScope(Dispatchers.IO).launch {
            writeLog(message,context)
        }
    }

    private fun sendMessageToActivity(context:Context,msg:String){
        var intent = Intent("me.xfly.message")
        intent.putExtra("message",msg)
        context.sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun writeLog(message: String, context: Context){
        var path = "${context.filesDir}/log"
        var dir = File(path)
        var file = File(path, "${getCurrentDate()}.txt")

        if (!dir.exists()) {
            dir.mkdir()
        }

        if (!file.exists()) {
            println("xflyme file not exist")
            file.createNewFile()
        }

        var newMessage = "${getCurrentTime()} $message \n"

        Files.write(file.toPath(), newMessage.toByteArray(), StandardOpenOption.APPEND)
    }

    private fun getCurrentTime(): String {
        val calendar: Calendar = Calendar.getInstance()
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(calendar.getTime())
    }

    private fun getCurrentDate(): String {
        val calendar: Calendar = Calendar.getInstance()
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd")
        return sdf.format(calendar.getTime())
    }
}