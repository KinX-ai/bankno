package com.example.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log

class BackgroundKeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return
        Log.d("KeepAliveReceiver", "Received action: $action. Triggering keep-alive...")

        try {
            val pm = context.packageManager
            val componentName = ComponentName(context, BankNotificationListenerService::class.java)
            
            // We only keep the service alive if the user has enabled listening
            val sharedPref = context.getSharedPreferences("bank_reader_settings", Context.MODE_PRIVATE)
            val isListening = sharedPref.getBoolean("is_listening", true)
            
            if (isListening) {
                // Request OS to rebind to notification listener
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        NotificationListenerService.requestRebind(componentName)
                    } catch (e: Exception) {
                        Log.w("KeepAliveReceiver", "requestRebind failed: ${e.message}")
                    }
                }

                // Component toggle fallback if unbind occurred
                try {
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    Log.w("KeepAliveReceiver", "Component toggle failed: ${e.message}")
                }
                
                // Explicitly start foreground service
                val serviceIntent = Intent(context, BankNotificationListenerService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (se: Exception) {
                    Log.w("KeepAliveReceiver", "Failed to start service directly: ${se.message}")
                }
                Log.d("KeepAliveReceiver", "Keep-alive routine completed successfully")
            }
        } catch (e: Exception) {
            Log.e("KeepAliveReceiver", "Critical error in onReceive keep-alive", e)
        }
    }
}

