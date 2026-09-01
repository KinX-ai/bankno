package com.example.viewmodel

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: TransactionRepository, context: Context) : ViewModel() {

    private val sharedPref = context.getSharedPreferences("bank_reader_settings", Context.MODE_PRIVATE)

    val deviceId: String = com.example.util.DeviceUtil.getOrCreateDeviceId(context)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isListening = MutableStateFlow(sharedPref.getBoolean("is_listening", true))
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _monitoredAppsOnly = MutableStateFlow(sharedPref.getBoolean("monitored_apps_only", true))
    val monitoredAppsOnly: StateFlow<Boolean> = _monitoredAppsOnly.asStateFlow()

    private val _apiUrl = MutableStateFlow(
        sharedPref.getString("api_url", "https://tainguyenweb.com/apiveo/api_auto.php?action=receive")
            ?: "https://tainguyenweb.com/apiveo/api_auto.php?action=receive"
    )
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _listenerPermissionGranted = MutableStateFlow(false)
    val listenerPermissionGranted: StateFlow<Boolean> = _listenerPermissionGranted.asStateFlow()

    private val _batteryOptimizationIgnored = MutableStateFlow(true)
    val batteryOptimizationIgnored: StateFlow<Boolean> = _batteryOptimizationIgnored.asStateFlow()

    private val _batteryPromptDismissed = MutableStateFlow(
        sharedPref.getBoolean("battery_prompt_dismissed", false)
    )
    val batteryPromptDismissed: StateFlow<Boolean> = _batteryPromptDismissed.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _testApiResult = MutableStateFlow<String?>(null)
    val testApiResult: StateFlow<String?> = _testApiResult.asStateFlow()

    init {
        checkPermission(context)
    }

    fun checkPermission(context: Context) {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        var enabled = false
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    enabled = true
                    break
                }
            }
        }
        _listenerPermissionGranted.value = enabled

        // Check battery optimization status
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        _batteryOptimizationIgnored.value = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(pkgName) ?: true
        } else {
            true
        }
    }

    fun dismissBatteryPrompt() {
        _batteryPromptDismissed.value = true
        sharedPref.edit().putBoolean("battery_prompt_dismissed", true).apply()
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        // Also automatically mark dismissed so it won't repeatedly nag on return
        dismissBatteryPrompt()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val intent = android.content.Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = android.content.Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    android.widget.Toast.makeText(context, "Không thể mở cài đặt pin. Hãy tắt thủ công trong Cài đặt điện thoại.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun toggleListening(enabled: Boolean) {
        _isListening.value = enabled
        sharedPref.edit().putBoolean("is_listening", enabled).apply()
    }

    fun toggleMonitoredAppsOnly(enabled: Boolean) {
        _monitoredAppsOnly.value = enabled
        sharedPref.edit().putBoolean("monitored_apps_only", enabled).apply()
    }

    fun updateApiUrl(url: String) {
        _apiUrl.value = url
        sharedPref.edit().putString("api_url", url).apply()
    }

    fun clearTransactions() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun triggerMockTransaction(
        bankName: String,
        amount: Double,
        type: String,
        account: String,
        balance: Double,
        customContent: String? = null
    ) {
        val sign = if (type == "IN") "+" else "-"
        val contentText = customContent ?: "GD: $sign${String.format("%,.0f", amount)} VND vao TK $account. So du: ${String.format("%,.0f", balance)} VND. ND: Test giao dich tu app"
        
        val transaction = TransactionEntity(
            bankName = bankName,
            title = bankName,
            content = contentText,
            amount = amount,
            type = type,
            accountNumber = account,
            balance = balance,
            timestamp = System.currentTimeMillis(),
            apiStatus = "PENDING"
        )

        viewModelScope.launch {
            val id = repository.insert(transaction)
            val saved = transaction.copy(id = id.toInt())
            _isTestingApi.value = true
            _testApiResult.value = "Đang gửi API..."
            
            val success = repository.sendTransactionToApi(saved, _apiUrl.value, deviceId)
            _isTestingApi.value = false
            if (success) {
                _testApiResult.value = "Gửi API thành công cho giao dịch #$id!"
            } else {
                _testApiResult.value = "Gửi API thất bại. Xem log chi tiết ở danh sách."
            }
        }
    }

    fun rebindListenerService(context: Context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(
                    ComponentName(context, com.example.service.BankNotificationListenerService::class.java)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            val keepAliveIntent = android.content.Intent(context, com.example.service.BackgroundKeepAliveReceiver::class.java).apply {
                action = "android.intent.action.USER_PRESENT"
            }
            context.sendBroadcast(keepAliveIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        checkPermission(context)
    }

    fun simulateNotificationParse(title: String, body: String, packageName: String): TransactionEntity? {
        return com.example.service.BankNotificationListenerService.parseNotification(title, body, packageName)
    }

    fun retryTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            // Set status to pending first for visual feedback
            val pending = transaction.copy(apiStatus = "PENDING", apiResponse = "Đang gửi lại...")
            repository.update(pending)
            repository.sendTransactionToApi(pending, _apiUrl.value, deviceId)
        }
    }

    fun clearTestResult() {
        _testApiResult.value = null
    }

    class Factory(private val repository: TransactionRepository, private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
