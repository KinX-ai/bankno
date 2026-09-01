package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionEntity
import com.example.service.BankNotificationListenerService
import com.example.viewmodel.MainViewModel
import com.example.util.StatsUtil
import com.example.util.StatsPeriod
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val monitoredAppsOnly by viewModel.monitoredAppsOnly.collectAsStateWithLifecycle()
    val apiUrl by viewModel.apiUrl.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.listenerPermissionGranted.collectAsStateWithLifecycle()
    val batteryOptimizationIgnored by viewModel.batteryOptimizationIgnored.collectAsStateWithLifecycle()
    val batteryPromptDismissed by viewModel.batteryPromptDismissed.collectAsStateWithLifecycle()
    val isTestingApi by viewModel.isTestingApi.collectAsStateWithLifecycle()
    val testApiResult by viewModel.testApiResult.collectAsStateWithLifecycle()
    val deviceId = viewModel.deviceId

    var customUrlText by remember(apiUrl) { mutableStateOf(apiUrl) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Transactions, 1: Stats
    var showDevDialog by remember { mutableStateOf(false) }

    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Layout wrapped in the beautiful Vibrant Palette theme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF)) // Vibrant Palette background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // HEADER BANNER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Bank Balance Notifier",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20), // Dark purple/obsidian text
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Real-time Transaction Bridge",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF49454F) // Medium gray-purple text
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Listening Pulse status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isListening && permissionGranted) Color(0xFF6750A4) // Brand vibrant purple for active
                                else Color(0xFFDC2626).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isListening && permissionGranted) Color.White else Color(0xFFDC2626),
                                    shape = CircleShape
                                )
                        ) {
                            if (isListening && permissionGranted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = pulseScale),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListening && permissionGranted) "Đang hoạt động" else "Tạm dừng",
                            color = if (isListening && permissionGranted) Color.White else Color(0xFFDC2626),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Advanced Settings / Developer Section Gear
                    FilledTonalIconButton(
                        onClick = { showDevDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("settings_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cấu hình nâng cao",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // PERMISSION ALERT CARD (Shown when notification access is missing)
            if (!permissionGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("permission_alert_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E6)), // Light red error container
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Cảnh báo",
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Yêu Cầu Quyền Truy Cập Thông Báo",
                                color = Color(0xFF1D1B20),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Để ứng dụng có thể đọc được biến động số dư từ thông báo các ngân hàng, bạn cần cấp quyền 'Truy cập thông báo' trong cài đặt hệ thống.",
                            color = Color(0xFF49454F),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_permission_button")
                        ) {
                            Text(text = "Cấp quyền truy cập ngay", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // BATTERY OPTIMIZATION ALERT CARD
            if (permissionGranted && !batteryOptimizationIgnored && !batteryPromptDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), // Light yellow/orange warning container
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDBA74))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Cảnh báo pin",
                                    tint = Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Cho Phép Chạy Ngầm Không Giới Hạn",
                                    color = Color(0xFF1D1B20),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hệ thống Android có thể tự động ngắt kết nối dịch vụ khi bạn đóng ứng dụng. Hãy tắt tối ưu hoá pin để dịch vụ nhận thông báo ổn định.",
                            color = Color(0xFF49454F),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.dismissBatteryPrompt()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Đã hiểu / Bỏ qua", color = Color(0xFF78350F))
                            }
                            Button(
                                onClick = {
                                    viewModel.requestIgnoreBatteryOptimization(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(text = "Tắt tối ưu hóa", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // DASHBOARD METRICS SUMMARY
            DashboardMetrics(transactions = transactions)

            // NAVIGATION TAB BAR
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF3EDF7), // Light lavender gray
                contentColor = Color(0xFF6750A4), // Brand vibrant purple
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF6750A4)
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lịch sử", maxLines = 1)
                        }
                    },
                    selectedContentColor = Color(0xFF6750A4),
                    unselectedContentColor = Color(0xFF49454F)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thống kê", maxLines = 1)
                        }
                    },
                    selectedContentColor = Color(0xFF6750A4),
                    unselectedContentColor = Color(0xFF49454F)
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cài đặt", maxLines = 1)
                        }
                    },
                    selectedContentColor = Color(0xFF6750A4),
                    unselectedContentColor = Color(0xFF49454F)
                )
            }

            // CONTENT DECIDER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> TransactionListSection(
                        transactions = transactions,
                        onDelete = { viewModel.deleteTransaction(it) },
                        onRetry = { viewModel.retryTransaction(it) },
                        onClearAll = { viewModel.clearTransactions() }
                    )
                    1 -> StatisticsSection(
                        transactions = transactions,
                        deviceId = deviceId
                    )
                    2 -> SettingsTabSection(
                        viewModel = viewModel,
                        deviceId = deviceId,
                        monitoredAppsOnly = monitoredAppsOnly,
                        apiUrl = apiUrl,
                        isTestingApi = isTestingApi,
                        testApiResult = testApiResult,
                        context = context
                    )
                }
            }
        }

        // ADVANCED SETTINGS & SIMULATOR DIALOG
        if (showDevDialog) {
            AlertDialog(
                onDismissRequest = { showDevDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDevDialog = false }) {
                        Text("Đóng", fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("Cấu hình nâng cao & Giả lập", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                    ) {
                        // Device ID info with Copy
                        item {
                            val clipboardManager = LocalClipboardManager.current
                            Column {
                                Text("Mã thiết bị hiện tại (Uniq ID)", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF49454F)))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = deviceId,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6750A4),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(deviceId))
                                            Toast.makeText(context, "Đã sao chép mã thiết bị!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Monitored Apps toggle
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Chỉ quét app ngân hàng", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Bỏ qua thông báo từ các app không phải ngân hàng/ví điện tử.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF49454F)))
                                }
                                Switch(
                                    checked = monitoredAppsOnly,
                                    onCheckedChange = { viewModel.toggleMonitoredAppsOnly(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6750A4))
                                )
                            }
                        }

                        // SettingsSection (API URL Configuration)
                        item {
                            Column {
                                Text("Địa chỉ API Nhận Dữ Liệu (POST)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customUrlText,
                                    onValueChange = { customUrlText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("https://example.com/api") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6750A4),
                                        unfocusedBorderColor = Color(0xFFCAC4D0)
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.updateApiUrl(customUrlText) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Lưu URL")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val defaultUrl = "https://tainguyenweb.com/apiveo/api_auto.php?action=receive"
                                            customUrlText = defaultUrl
                                            viewModel.updateApiUrl(defaultUrl)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Khôi phục")
                                    }
                                }
                            }
                        }

                        // TransactionSimulatorSection
                        item {
                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Trình Giả Lập Biến Động Số Dư (API Test)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var simBank by remember { mutableStateOf("Techcombank") }
                            var simAmount by remember { mutableStateOf("500000") }
                            var simType by remember { mutableStateOf("IN") } // "IN" or "OUT"
                            var simAccount by remember { mutableStateOf("19035678912345") }
                            var simBalance by remember { mutableStateOf("2500000") }
                            var simCustomContent by remember { mutableStateOf("") }
                            
                            val bankOptions = listOf("Techcombank", "Vietcombank", "MB Bank", "TPBank", "MoMo")

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Bank selector
                                Text("1. Chọn Ngân Hàng", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    bankOptions.forEach { bank ->
                                        val isSelected = simBank == bank
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) Color(0xFF6750A4) else Color(0xFFF3EDF7))
                                                .clickable { simBank = bank }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = bank,
                                                color = if (isSelected) Color.White else Color(0xFF49454F),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Type switcher
                                Text("2. Loại Giao Dịch", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { simType = "IN" },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (simType == "IN") Color(0xFF059669) else Color(0xFFF3EDF7),
                                            contentColor = if (simType == "IN") Color.White else Color(0xFF49454F)
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("IN (+)", fontSize = 11.sp)
                                        }
                                    }
                                    Button(
                                        onClick = { simType = "OUT" },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (simType == "OUT") Color(0xFFDC2626) else Color(0xFFF3EDF7),
                                            contentColor = if (simType == "OUT") Color.White else Color(0xFF49454F)
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("OUT (-)", fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Amount Input
                                Text("3. Số Tiền Giao Dịch (VND)", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = simAmount,
                                    onValueChange = { simAmount = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6750A4)),
                                    singleLine = true
                                )

                                // Account Number Input
                                Text("4. Số Tài Khoản", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = simAccount,
                                    onValueChange = { simAccount = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6750A4)),
                                    singleLine = true
                                )

                                // Balance Input
                                Text("5. Số Dư Còn Lại (VND)", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = simBalance,
                                    onValueChange = { simBalance = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6750A4)),
                                    singleLine = true
                                )

                                // Custom raw content text box
                                Text("6. Nội Dung Thông Báo Tùy Biến (Tùy chọn)", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(
                                    value = simCustomContent,
                                    onValueChange = { simCustomContent = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Để trống để dùng tin mẫu SMS chuẩn", color = Color(0xFF7A757F)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6750A4)),
                                    maxLines = 2
                                )

                                // Trigger Simulator
                                Button(
                                    onClick = {
                                        val amt = simAmount.toDoubleOrNull() ?: 0.0
                                        val bal = simBalance.toDoubleOrNull() ?: 0.0
                                        val content = if (simCustomContent.trim().isNotEmpty()) simCustomContent else null
                                        viewModel.triggerMockTransaction(simBank, amt, simType, simAccount, bal, content)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text("Gửi test giao dịch giả lập")
                                }

                                // Testing feedback
                                if (isTestingApi || testApiResult != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isTestingApi) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = testApiResult ?: "Đang gửi...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF6750A4)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Troubleshooting guide item
                        item {
                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Troubleshoot",
                                            tint = Color(0xFFEA580C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Xử lý lỗi không nhận/gửi thông báo",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFC2410C)
                                        )
                                    }
                                    Text(
                                        "Mặc dù dịch vụ đã được chuyển thành Dịch vụ chạy ngầm (Foreground Service) với thông báo trạng thái liên tục để tránh bị đóng, một số dòng máy (như Xiaomi, Oppo, Vivo, Realme, Samsung) vẫn áp dụng chính sách tắt ứng dụng rất nghiêm ngặt khi bạn vuốt tắt app khỏi màn hình đa nhiệm hoặc tắt màn hình. Để khắc phục triệt để, bạn hãy làm theo các bước sau:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7C2D12)
                                    )
                                    Text(
                                        "1. Bật Tự Khởi Chạy (Auto-start): Nhấn giữ biểu tượng ứng dụng ngoài màn hình chính -> Thông tin ứng dụng -> Bật mục 'Tự khởi chạy' (Auto-start).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7C2D12)
                                    )
                                    Text(
                                        "2. Tắt Tối Ưu Hóa Pin: Tại mục 'Thông tin ứng dụng' -> 'Tiết kiệm pin' (Battery saver) -> Chọn chế độ 'Không giới hạn' (No restrictions) thay vì 'Mặc định/Tiết kiệm pin thông minh'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7C2D12)
                                    )
                                    Text(
                                        "3. Khóa Ứng Dụng Trong Đa Nhiệm: Mở màn hình đa nhiệm (Recent apps) -> Nhấn giữ ứng dụng này hoặc vuốt nhẹ để hiển thị menu -> Chọn biểu tượng ổ khóa để khóa ứng dụng không bị đóng khi bạn nhấn 'Xóa tất cả'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7C2D12)
                                    )
                                    Text(
                                        "4. Tắt và bật lại quyền 'Truy cập thông báo': Trong cài đặt điện thoại, tắt rồi bật lại quyền của app này để làm mới kết nối dịch vụ.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7C2D12)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

// ----------------------------------------
// SUB-COMPONENTS & SECTIONS
// ----------------------------------------

@Composable
fun DashboardMetrics(transactions: List<TransactionEntity>) {
    val totalIn = transactions.filter { it.type == "IN" }.sumOf { it.amount }
    val totalOut = transactions.filter { it.type == "OUT" }.sumOf { it.amount }
    
    val totalSent = transactions.size
    val successfulSent = transactions.count { it.apiStatus == "SUCCESS" }
    val successRate = if (totalSent > 0) (successfulSent.toFloat() / totalSent * 100).toInt() else 100

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Metric card 1: Money In
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF059669), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tổng nhận", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+${String.format("%,.0f", totalIn)} đ",
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Metric card 2: Money Out
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFFDC2626), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tổng chuyển", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "-${String.format("%,.0f", totalOut)} đ",
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Metric card 3: API Success Ratio
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tỉ lệ API", color = Color(0xFF49454F), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$successRate% ($successfulSent/$totalSent)",
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TransactionListSection(
    transactions: List<TransactionEntity>,
    onDelete: (Int) -> Unit,
    onRetry: (TransactionEntity) -> Unit,
    onClearAll: () -> Unit
) {
    if (transactions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "No data",
                tint = Color(0xFFCAC4D0),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Chưa có giao dịch nào được lưu trữ",
                color = Color(0xFF1D1B20),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hãy cấp quyền, kích hoạt và đợi các thông báo biến động số dư ngân hàng đến, hoặc chuyển sang tab 'Giả lập / Test' để thử nghiệm.",
                color = Color(0xFF49454F),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giao dịch đã chụp (${transactions.size})",
                    color = Color(0xFF49454F),
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa tất cả")
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(transactions, key = { it.id }) { item ->
                    TransactionItemCard(
                        transaction = item,
                        onDelete = { onDelete(item.id) },
                        onRetry = { onRetry(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val formattedTime = sdf.format(Date(transaction.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandedDetails = !expandedDetails }
            .border(
                1.dp,
                if (transaction.apiStatus == "SUCCESS") Color(0xFF059669).copy(alpha = 0.3f)
                else if (transaction.apiStatus == "FAILED") Color(0xFFDC2626).copy(alpha = 0.3f)
                else Color(0xFFCAC4D0).copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bank Name and Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (transaction.type == "IN") Color(0xFFE2F9E9)
                                else Color(0xFFFCE8E6),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.type == "IN") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (transaction.type == "IN") Color(0xFF059669) else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = transaction.bankName,
                            color = Color(0xFF1D1B20),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "TK: ${transaction.accountNumber}",
                            color = Color(0xFF49454F),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    val color = if (transaction.type == "IN") Color(0xFF059669) else Color(0xFFDC2626)
                    val prefix = if (transaction.type == "IN") "+" else "-"
                    Text(
                        text = "$prefix${String.format("%,.0f", transaction.amount)} đ",
                        color = color,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formattedTime,
                        color = Color(0xFF49454F),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Raw message body
            Text(
                text = transaction.content,
                color = Color(0xFF1D1B20),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer bar showing API status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // API Status Badge
                    val (badgeBg, badgeText, statusLabel, icon) = when (transaction.apiStatus) {
                        "SUCCESS" -> Quadruple(
                            Color(0xFFE2F9E9),
                            Color(0xFF059669),
                            "Đã gửi backend",
                            Icons.Default.CloudDone
                        )
                        "FAILED" -> Quadruple(
                            Color(0xFFFCE8E6),
                            Color(0xFFDC2626),
                            "Gửi lỗi",
                            Icons.Default.CloudOff
                        )
                        else -> Quadruple(
                            Color(0xFFF3EDF7),
                            Color(0xFF49454F),
                            "Đang chờ gửi",
                            Icons.Default.CloudQueue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = badgeText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLabel,
                                color = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Remaining balance display
                    if (transaction.balance > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dư: ${String.format("%,.0f", transaction.balance)}đ",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (transaction.apiStatus == "FAILED") {
                        IconButton(
                            onClick = { onRetry() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Thử lại",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Xóa",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded technical details (API Response log)
            AnimatedVisibility(
                visible = expandedDetails,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color(0xFFFEF7FF), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Chi tiết phản hồi từ máy chủ:",
                        color = Color(0xFF6750A4),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = transaction.apiResponse ?: "Chưa có phản hồi hoặc chưa được gửi đi.",
                            color = Color(0xFF49454F),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Simple Helper Quadruple class
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun StatisticsSection(
    transactions: List<TransactionEntity>,
    deviceId: String
) {
    var statsType by remember { mutableStateOf(0) } // 0: Month, 1: Quarter
    
    val monthlyStats = remember(transactions) { StatsUtil.getMonthlyStats(transactions) }
    val quarterlyStats = remember(transactions) { StatsUtil.getQuarterlyStats(transactions) }
    
    val currentStats = if (statsType == 0) monthlyStats else quarterlyStats

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // DEVICE ID INFOBAR
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mã thiết bị của bạn:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    )
                }
                
                Text(
                    text = deviceId,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                )
            }
        }

        // STATS TYPE SELECTOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3EDF7))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { statsType = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (statsType == 0) Color.White else Color.Transparent,
                    contentColor = if (statsType == 0) Color(0xFF6750A4) else Color(0xFF49454F)
                ),
                elevation = if (statsType == 0) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("Theo Tháng", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = { statsType = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (statsType == 1) Color.White else Color.Transparent,
                    contentColor = if (statsType == 1) Color(0xFF6750A4) else Color(0xFF49454F)
                ),
                elevation = if (statsType == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("Theo Quý", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (currentStats.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color(0xFFCAC4D0),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chưa có đủ dữ liệu thống kê",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1D1B20),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Dữ liệu thống kê nhận và chuyển tiền sẽ xuất hiện tại đây khi thiết bị chụp được các giao dịch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentStats) { stat ->
                    StatsCard(stat = stat)
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatsCard(stat: StatsPeriod) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stat.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3EDF7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${stat.transactionCount} GD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF49454F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Money In (Received) Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tiền nhận",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF49454F)
                    )
                }
                Text(
                    text = "+${String.format("%,.0f", stat.totalIn)} đ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Money Out (Transferred) Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tiền chuyển",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF49454F)
                    )
                }
                Text(
                    text = "-${String.format("%,.0f", stat.totalOut)} đ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Net Difference Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chênh lệch thu chi",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1B20)
                )
                
                val netColor = if (stat.net >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                val netSign = if (stat.net >= 0) "+" else ""
                Text(
                    text = "$netSign${String.format("%,.0f", stat.net)} đ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = netColor
                )
            }
        }
    }
}

// Simple Selection Container workaround
@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}

@Composable
fun SettingsTabSection(
    viewModel: MainViewModel,
    deviceId: String,
    monitoredAppsOnly: Boolean,
    apiUrl: String,
    isTestingApi: Boolean,
    testApiResult: String?,
    context: android.content.Context
) {
    var customUrlText by remember(apiUrl) { mutableStateOf(apiUrl) }
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // Section 1: Device ID Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PermIdentity,
                            contentDescription = null,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mã Thiết Bị (Device ID)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1D1B20)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mỗi thiết bị có một mã định danh duy nhất để gửi kèm theo payload webhook sang server của bạn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = deviceId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(deviceId))
                                Toast.makeText(context, "Đã sao chép mã thiết bị!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sao chép", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Filter Toggle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Chỉ quét app ngân hàng",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1D1B20)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tự động lọc và bỏ qua tin nhắn từ Zalo cá nhân, tin nhắn người thân, Shopee, TikTok để tránh nhận sai.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F)
                        )
                    }
                    Switch(
                        checked = monitoredAppsOnly,
                        onCheckedChange = { viewModel.toggleMonitoredAppsOnly(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6750A4)
                        )
                    )
                }
            }
        }

        // Section 3: Webhook API URL Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Địa Chỉ API Webhook (POST)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1D1B20)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Dữ liệu biến động số dư sẽ được gửi tức thì (POST JSON) đến URL này ngay khi phát hiện.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customUrlText,
                        onValueChange = { customUrlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://your-server.com/api/webhook") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateApiUrl(customUrlText)
                                viewModel.triggerMockTransaction("Vietcombank", 50000.0, "IN", "9988776655", 2500000.0, "Test API từ cài đặt")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isTestingApi
                        ) {
                            Text("Test gửi API")
                        }
                        Button(
                            onClick = {
                                viewModel.updateApiUrl(customUrlText)
                                Toast.makeText(context, "Đã lưu địa chỉ API thành công!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lưu URL", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isTestingApi || testApiResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isTestingApi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = testApiResult ?: "Đang gửi tín hiệu kiểm tra kết nối...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6750A4)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: System Permissions & Battery Optimization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Quyền Hệ Thống & Chạy Nền",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1D1B20)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mở Cài Đặt Quyền Đọc Thông Báo")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.requestIgnoreBatteryOptimization(context)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tắt Tối Ưu Hóa Pin (Không giới hạn)")
                        }
                    }
                }
            }
        }

        // Section 5: Troubleshooting Guide Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Troubleshoot",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hướng dẫn giữ dịch vụ chạy 24/7 ổn định",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFC2410C)
                        )
                    }
                    Text(
                        "Một số hãng máy (Xiaomi, Oppo, Vivo, Realme, Samsung) thường tự động tắt ứng dụng khi đóng đa nhiệm. Hãy thiết lập:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7C2D12)
                    )
                    Text(
                        "• Bật Tự khởi chạy (Auto-start) trong Thông tin ứng dụng.\n• Khóa app trong danh sách Đa nhiệm (Recent Apps).\n• Đặt Tiết kiệm pin thành 'Không giới hạn'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7C2D12)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


