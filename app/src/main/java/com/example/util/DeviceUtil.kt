package com.example.util

import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceUtil {
    fun getOrCreateDeviceId(context: Context): String {
        val sharedPref = context.getSharedPreferences("bank_reader_settings", Context.MODE_PRIVATE)
        var deviceId = sharedPref.getString("device_id", null)
        if (deviceId.isNullOrEmpty()) {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val cleanId = if (androidId.isNullOrEmpty() || androidId == "9774d56d682e549c") {
                UUID.randomUUID().toString().substring(0, 8).uppercase()
            } else {
                if (androidId.length > 8) {
                    androidId.substring(androidId.length - 8).uppercase()
                } else {
                    androidId.uppercase()
                }
            }
            deviceId = "DEV-$cleanId"
            sharedPref.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }
}
