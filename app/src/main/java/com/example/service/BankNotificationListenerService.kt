package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BankApplication
import com.example.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val NOTIFICATION_ID = 888
    private val CHANNEL_ID = "bank_listener_foreground_channel"

    override fun onCreate() {
        super.onCreate()
        Log.d("BankNotification", "Service onCreate")
        startForegroundService()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("BankNotification", "Service onListenerConnected")
        startForegroundService()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("BankNotification", "Service onListenerDisconnected! Requesting automatic rebind...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(ComponentName(this, BankNotificationListenerService::class.java))
            }
        } catch (e: Exception) {
            Log.e("BankNotification", "Failed to request rebind", e)
        }
    }

    private fun startForegroundService() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Dịch vụ chạy ngầm đọc số dư",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Đảm bảo dịch vụ đọc thông báo ngân hàng hoạt động liên tục trong nền"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val pm = packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                null
            }

            val iconRes = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                if (appInfo.icon != 0) appInfo.icon else android.R.drawable.stat_notify_sync
            } catch (e: Exception) {
                android.R.drawable.stat_notify_sync
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle("Trình đọc biến động số dư")
                .setContentText("Dịch vụ đang chạy ngầm và sẵn sàng nhận thông báo 24/7")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d("BankNotification", "Service successfully running in foreground")
        } catch (e: Exception) {
            Log.e("BankNotification", "Failed to start foreground service", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BankNotification", "Service onStartCommand")
        startForegroundService()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("BankNotification", "onTaskRemoved - App swiped away. Broadcasting keep-alive...")
        
        try {
            val broadcastIntent = Intent(applicationContext, BackgroundKeepAliveReceiver::class.java).apply {
                action = "android.intent.action.USER_PRESENT"
            }
            sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            Log.e("BankNotification", "Failed to send keep-alive broadcast onTaskRemoved", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val sharedPref = getSharedPreferences("bank_reader_settings", Context.MODE_PRIVATE)
        val isListening = sharedPref.getBoolean("is_listening", true)
        if (!isListening) return

        val packageName = sbn.packageName ?: ""
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").trim()
        val titleBig = (extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: "").trim()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").trim()
        val bigText = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "").trim()
        val subText = (extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").trim()
        val summaryText = (extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: "").trim()
        val infoText = (extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: "").trim()
        val ticker = (notification.tickerText?.toString() ?: "").trim()

        // Extract lines for InboxStyle / grouped notifications
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val linesText = lines?.joinToString(" | ") { it.toString().trim() } ?: ""

        val effectiveTitle = if (title.isNotEmpty()) title else titleBig

        // Combine non-empty body fragments so we don't miss text split between fields
        val contentParts = linkedSetOf<String>()
        if (text.isNotEmpty()) contentParts.add(text)
        if (bigText.isNotEmpty() && bigText != text) contentParts.add(bigText)
        if (linesText.isNotEmpty() && linesText != text) contentParts.add(linesText)
        if (subText.isNotEmpty() && subText != text && subText != effectiveTitle) contentParts.add(subText)
        if (summaryText.isNotEmpty() && summaryText != text) contentParts.add(summaryText)
        if (infoText.isNotEmpty() && infoText != text) contentParts.add(infoText)
        if (ticker.isNotEmpty() && ticker != text && ticker != effectiveTitle) contentParts.add(ticker)

        val body = if (contentParts.isNotEmpty()) contentParts.joinToString(" | ") else text
        if (body.isEmpty() && effectiveTitle.isEmpty()) return

        // Default monitored_apps_only to true to block non-banking apps from triggering false demo data
        val monitoredAppsOnly = sharedPref.getBoolean("monitored_apps_only", true)
        if (monitoredAppsOnly && !isBankingApp(packageName)) {
            return
        }

        // Process transaction
        val transaction = parseNotification(effectiveTitle, body, packageName)
        if (transaction != null) {
            val now = System.currentTimeMillis()
            val dedupeKey = "$packageName|${transaction.bankName}|${transaction.amount}|${transaction.type}|${transaction.accountNumber}|${transaction.balance}"
            
            // Check if processed within last 4 seconds
            val lastProcessed = recentProcessedMap[dedupeKey]
            if (lastProcessed != null && (now - lastProcessed) < 4000) {
                Log.d("BankNotification", "Skipping rapid duplicate notification for: $dedupeKey")
                return
            }
            recentProcessedMap[dedupeKey] = now

            val app = application as BankApplication
            val repository = app.repository
            val apiUrl = sharedPref.getString(
                "api_url",
                "https://tainguyenweb.com/apiveo/api_auto.php?action=receive"
            ) ?: "https://tainguyenweb.com/apiveo/api_auto.php?action=receive"

            val deviceId = com.example.util.DeviceUtil.getOrCreateDeviceId(this@BankNotificationListenerService)
            scope.launch {
                // Save locally
                val id = repository.insert(transaction)
                val savedTransaction = transaction.copy(id = id.toInt())

                // Send to backend API
                repository.sendTransactionToApi(savedTransaction, apiUrl, deviceId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        // Cache to deduplicate rapid multiple callbacks from the same notification
        private val recentProcessedMap = java.util.Collections.synchronizedMap(
            object : java.util.LinkedHashMap<String, Long>(50, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                    return size > 100
                }
            }
        )

        fun isSmsApp(packageName: String): Boolean {
            val lower = packageName.lowercase()
            val smsPackages = listOf(
                "messaging", "mms", "sms", "telephony", "message", "inbox",
                "com.google.android.apps.messaging", "com.samsung.android.messaging",
                "com.android.mms", "com.sonyericsson.conversations", "com.miui.sms",
                "com.xiaomi.mms", "com.oppo.mms", "com.coloros.mms", "com.vivo.mms"
            )
            return smsPackages.any { lower.contains(it) }
        }

        fun isBankingApp(packageName: String): Boolean {
            val knownPackages = listOf(
                "vcb", "vietcombank", "techcombank", "tcb", "mbbank", "mbcorp", "mb.android",
                "tpb", "tpbank", "bidv", "agribank", "vpbank", "cake", "timo", "acb",
                "sacombank", "vietinbank", "ctg", "shb", "vib", "msb", "ocb", "scb",
                "hdbank", "hdb", "seabank", "eximbank", "shinhan", "kbank", "hsbc", "lpbank",
                "lienvietpostbank", "kienlongbank", "baovietbank", "bacabank", "namabank", "vietabank",
                "pvcombank", "wooribank", "uob", "cimb", "publicbank", "vietcapitalbank", "bvb", "ncb",
                "momo", "mservice", "zalopay", "viettelpay", "viettelmoney", "vnpay", "vnptpay",
                "shopeepay", "airpay", "payoo"
            )
            val lower = packageName.lowercase()
            return knownPackages.any { lower.contains(it) } || isSmsApp(packageName)
        }

        fun detectBankName(packageName: String, title: String, body: String): String {
            val combined = "$packageName $title $body".lowercase()
            val pLower = packageName.lowercase()
            val tLower = title.lowercase()

            return when {
                // MB Bank
                pLower.contains("mbmobile") || pLower.contains("mb.android") || pLower.contains("mbbank") ||
                        tLower.contains("mbbank") || tLower.contains("mb bank") ||
                        Regex("""\b(mbbank|mb bank|ngân hàng quân đội|ngan hang quan doi)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "MB Bank"

                // Vietcombank
                pLower.contains("vcb") || pLower.contains("vietcombank") ||
                        tLower.contains("vcb") || tLower.contains("vietcombank") ||
                        Regex("""\b(vietcombank|vcb|vcb digibank)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Vietcombank"

                // Techcombank
                pLower.contains("techcombank") || pLower.contains("tcb") ||
                        tLower.contains("techcombank") || tLower.contains("tcb") ||
                        Regex("""\b(techcombank|tcb)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Techcombank"

                // VPBank
                pLower.contains("vpbank") ||
                        tLower.contains("vpbank") || tLower.contains("vpb") ||
                        Regex("""\b(vpbank|vpbank neo|vpb)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "VPBank"

                // TPBank
                pLower.contains("tpb") || pLower.contains("tpbank") ||
                        tLower.contains("tpbank") || tLower.contains("tpb") ||
                        Regex("""\b(tpbank|tpb|tiên phong bank|tien phong bank)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "TPBank"

                // ACB
                pLower.contains("acb") ||
                        tLower.contains("acb") ||
                        Regex("""\b(acb|acb one|á châu)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "ACB"

                // BIDV
                pLower.contains("bidv") ||
                        tLower.contains("bidv") ||
                        Regex("""\b(bidv|smartbanking)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "BIDV"

                // Agribank
                pLower.contains("agribank") ||
                        tLower.contains("agribank") ||
                        Regex("""\b(agribank|vba)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Agribank"

                // VietinBank
                pLower.contains("vietinbank") || pLower.contains("ctg") ||
                        tLower.contains("vietinbank") || tLower.contains("ipay") ||
                        Regex("""\b(vietinbank|vietinbank ipay|ipay)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Vietinbank"

                // Sacombank
                pLower.contains("sacombank") ||
                        tLower.contains("sacombank") ||
                        Regex("""\b(sacombank|sacombank pay)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Sacombank"

                // VIB
                pLower.contains("vib") ||
                        tLower.contains("vib") || tLower.contains("myvib") ||
                        Regex("""\b(vib|myvib)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "VIB"

                // SHB
                pLower.contains("shb") ||
                        tLower.contains("shb") ||
                        Regex("""\b(shb|shb mobile)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "SHB"

                // MSB
                pLower.contains("msb") ||
                        tLower.contains("msb") ||
                        Regex("""\b(msb|maritime bank)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "MSB"

                // OCB
                pLower.contains("ocb") ||
                        tLower.contains("ocb") ||
                        Regex("""\b(ocb|ocb omni)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "OCB"

                // SCB
                pLower.contains("scb") ||
                        tLower.contains("scb") ||
                        Regex("""\b(scb|scb mobile)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "SCB"

                // HDBank
                pLower.contains("hdbank") || pLower.contains("hdb") ||
                        tLower.contains("hdbank") ||
                        Regex("""\b(hdbank|hdb)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "HDBank"

                // SeABank
                pLower.contains("seabank") ||
                        tLower.contains("seabank") ||
                        Regex("""\b(seabank|seamobile)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "SeABank"

                // LPBank
                pLower.contains("lpbank") || pLower.contains("lienvietpostbank") ||
                        tLower.contains("lpbank") || tLower.contains("lienviet") ||
                        Regex("""\b(lpbank|lienvietpostbank)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "LPBank"

                // Cake by VPBank
                pLower.contains("cake") ||
                        tLower.contains("cake") ||
                        Regex("""\b(cake by vpbank|app cake)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Cake"

                // Timo
                pLower.contains("timo") ||
                        tLower.contains("timo") ||
                        Regex("""\b(timo digital bank|timo)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Timo"

                // MoMo
                pLower.contains("momo") || pLower.contains("mservice") ||
                        tLower.contains("momo") ||
                        Regex("""\b(momo|ví momo|vi momo)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "MoMo"

                // ZaloPay
                pLower.contains("zalopay") ||
                        tLower.contains("zalopay") ||
                        Regex("""\b(zalopay|ví zalopay|vi zalopay)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "ZaloPay"

                // Viettel Money
                pLower.contains("viettel") ||
                        tLower.contains("viettelpay") || tLower.contains("viettel money") ||
                        Regex("""\b(viettelpay|viettel money|viettelmoney)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "Viettel Money"

                // VNPay
                pLower.contains("vnpay") ||
                        tLower.contains("vnpay") ||
                        Regex("""\b(vnpay|ví vnpay)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "VNPay"

                // ShopeePay
                pLower.contains("shopeepay") || pLower.contains("airpay") ||
                        tLower.contains("shopeepay") ||
                        Regex("""\b(shopeepay|airpay)\b""", RegexOption.IGNORE_CASE).containsMatchIn(combined) -> "ShopeePay"

                else -> {
                    if (title.isNotEmpty() && title.length < 25 && !isSmsApp(packageName)) {
                        title
                    } else {
                        "Bank"
                    }
                }
            }
        }

        fun parseNotification(title: String, body: String, packageName: String): TransactionEntity? {
            // Clean non-breaking spaces and normalize
            val normTitle = title.replace('\u00A0', ' ').trim()
            val normBody = body.replace('\u00A0', ' ').trim()
            val fullText = "$normTitle $normBody".trim()
            val lower = fullText.lowercase()

            // 1. Strict security / OTP exclusion (these are never balance change transactions)
            val otpKeywords = listOf(
                "mã otp", " mã xác thực", "smart otp", "ma otp", "khoá thẻ", "khoa the",
                "đăng nhập trên thiết bị", "dang nhap tren thiet bi", "cảnh báo đăng nhập",
                "đổi mật khẩu thành công", "doi mat khau thanh cong"
            )
            if (otpKeywords.any { lower.contains(it) }) {
                return null
            }

            // 2. Identify the bank
            val bankName = detectBankName(packageName, normTitle, normBody)
            val isSms = isSmsApp(packageName)

            // If it's an SMS app, it MUST have a recognized bank sender/name.
            // Personal SMS from contacts (e.g. "Nguyen Van A", "+849123...") must NOT be parsed as bank transactions!
            if (isSms && bankName == "Bank") {
                return null
            }

            // If it's a non-banking app and no bank recognized, reject
            if (!isBankingApp(packageName) && bankName == "Bank") {
                return null
            }

            // 3. Must have standard banking / balance change indicators
            val hasBankingContext = isBankingApp(packageName) || bankName != "Bank" ||
                    lower.contains("tk") || lower.contains("tài khoản") || lower.contains("tai khoan") ||
                    lower.contains("số dư") || lower.contains("so du") || lower.contains("sd:") || lower.contains("sd ") ||
                    lower.contains("biến động") || lower.contains("bien dong") || lower.contains("giao dịch") || lower.contains("giao dich") ||
                    lower.contains("gd:") || lower.contains("ps:") || lower.contains("vnd") || lower.contains("vnđ") || lower.contains("đ")

            if (!hasBankingContext) {
                return null
            }

            var amount = 0.0
            var type = "UNKNOWN"

            // --- MULTI-TIER AMOUNT EXTRACTION (Evaluated against fullText = title + body) ---

            // Tier 1: Match signed amounts: e.g., +50,000 VND, -100.000đ, + 50.000, +50,000.00
            val signedAmountRegex = """([+-])\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]{4,15})\s*(?:vnd|vnđ|đ|d|dong|đồng)?""".toRegex(RegexOption.IGNORE_CASE)
            val signedMatch = signedAmountRegex.find(fullText)
            if (signedMatch != null) {
                val sign = signedMatch.groupValues[1]
                var rawNum = signedMatch.groupValues[2]
                // Handle optional decimal cents (e.g. 50,000.00 -> 50,000)
                if (rawNum.endsWith(".00") || rawNum.endsWith(",00")) {
                    rawNum = rawNum.substring(0, rawNum.length - 3)
                }
                val amountStr = rawNum.replace(".", "").replace(",", "")
                amount = amountStr.toDoubleOrNull() ?: 0.0
                type = if (sign == "+") "IN" else "OUT"
            }

            // Tier 2: Match action-prefixed amount: e.g., "Cộng: 50,000 VND", "Trừ: 100.000đ", "GD: +50.000", "Biến động: +50.000"
            if (amount <= 0.0) {
                val actionRegex = """(?:cộng|cong|trừ|tru|nợ|có|no|co|tăng|tang|giảm|giam|nhận|nhan|thanh toán|thanh toan|chuyển|chuyen|rút|rut|nạp|nap|gd|ps|biến động|bien dong|giao dịch|giao dich)\s*:?\s*([+-]?)\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]{4,15})\s*(?:vnd|vnđ|đ|d|dong|đồng)?""".toRegex(RegexOption.IGNORE_CASE)
                val actionMatch = actionRegex.find(fullText)
                if (actionMatch != null) {
                    val fullMatchStr = actionMatch.value.lowercase()
                    val sign = actionMatch.groupValues[1]
                    var rawNum = actionMatch.groupValues[2]
                    if (rawNum.endsWith(".00") || rawNum.endsWith(",00")) {
                        rawNum = rawNum.substring(0, rawNum.length - 3)
                    }
                    val amountStr = rawNum.replace(".", "").replace(",", "")
                    amount = amountStr.toDoubleOrNull() ?: 0.0

                    type = when {
                        sign == "+" || fullMatchStr.contains("cộng") || fullMatchStr.contains("cong") || fullMatchStr.contains("có") || fullMatchStr.contains("co") || fullMatchStr.contains("tăng") || fullMatchStr.contains("tang") || fullMatchStr.contains("nhận") || fullMatchStr.contains("nhan") || fullMatchStr.contains("nạp") || fullMatchStr.contains("nap") -> "IN"
                        sign == "-" || fullMatchStr.contains("trừ") || fullMatchStr.contains("tru") || fullMatchStr.contains("nợ") || fullMatchStr.contains("no") || fullMatchStr.contains("giảm") || fullMatchStr.contains("giam") || fullMatchStr.contains("thanh toán") || fullMatchStr.contains("thanh toan") || fullMatchStr.contains("chuyển") || fullMatchStr.contains("chuyen") || fullMatchStr.contains("rút") || fullMatchStr.contains("rut") -> "OUT"
                        else -> "UNKNOWN"
                    }
                }
            }

            // Tier 3: Match "Số tiền: 50.000 VND", "Số tiền GD: 50,000", "STGD: 50.000"
            if (amount <= 0.0) {
                val amountLabelRegex = """(?:số tiền|so tien|amount|st gd|stgd|st)\s*:?\s*([+-]?)\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]{4,15})""".toRegex(RegexOption.IGNORE_CASE)
                val labelMatch = amountLabelRegex.find(fullText)
                if (labelMatch != null) {
                    val sign = labelMatch.groupValues[1]
                    var rawNum = labelMatch.groupValues[2]
                    if (rawNum.endsWith(".00") || rawNum.endsWith(",00")) {
                        rawNum = rawNum.substring(0, rawNum.length - 3)
                    }
                    val amountStr = rawNum.replace(".", "").replace(",", "")
                    amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (sign == "+") type = "IN" else if (sign == "-") type = "OUT"
                }
            }

            // Tier 4: Match currency suffixes in context: e.g. "50,000 VND", "100.000 đ", "50000 vnđ"
            if (amount <= 0.0) {
                val currencyRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,15})\s*(?:vnd|vnđ|đ|dong|đồng)\b""".toRegex(RegexOption.IGNORE_CASE)
                val curMatch = currencyRegex.find(fullText)
                if (curMatch != null) {
                    val amountStr = curMatch.groupValues[1].replace(".", "").replace(",", "")
                    amount = amountStr.toDoubleOrNull() ?: 0.0
                }
            }

            // Refine Transaction Type if still UNKNOWN
            if (type == "UNKNOWN") {
                type = when {
                    lower.contains("nhận") || lower.contains("nhan") || lower.contains("cộng") || lower.contains("cong") || lower.contains("có") || lower.contains("co") || lower.contains("tăng") || lower.contains("tang") || lower.contains("received") || lower.contains("+") || lower.contains("hoàn tiền") || lower.contains("nạp tiền") -> "IN"
                    lower.contains("trừ") || lower.contains("tru") || lower.contains("nợ") || lower.contains("no") || lower.contains("giảm") || lower.contains("giam") || lower.contains("chuyển") || lower.contains("chuyen") || lower.contains("thanh toán") || lower.contains("thanh toan") || lower.contains("rút") || lower.contains("rut") || lower.contains("phí") || lower.contains("-") -> "OUT"
                    else -> "IN" // Default positive if unrecognized
                }
            }

            // If no valid positive amount found, not a money transaction
            if (amount <= 0.0) {
                return null
            }

            // Parse Account Number
            val accountRegex = """(?:tk|tài khoản|tai khoan|account|TK|số TK|so tk|thẻ|the)\s*:?\s*([0-9a-zA-Z*_\-\.]+)""".toRegex(RegexOption.IGNORE_CASE)
            val accountMatch = accountRegex.find(fullText)
            val account = accountMatch?.groupValues?.get(1) ?: "Unknown"

            // Parse Remaining Balance
            val balanceRegex = """(?:số dư|so du|SD|balance|bal|sodu|khả dụng|kha dung|cuối|cuoi)\s*:?\s*(?:là:?)?\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,15})""".toRegex(RegexOption.IGNORE_CASE)
            val balanceMatch = balanceRegex.find(fullText)
            val balance = balanceMatch?.groupValues?.get(1)?.replace(".", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0

            return TransactionEntity(
                bankName = bankName,
                title = normTitle.ifEmpty { bankName },
                content = normBody.ifEmpty { normTitle },
                amount = amount,
                type = type,
                accountNumber = account,
                balance = balance,
                timestamp = System.currentTimeMillis(),
                apiStatus = "PENDING"
            )
        }
    }
}

