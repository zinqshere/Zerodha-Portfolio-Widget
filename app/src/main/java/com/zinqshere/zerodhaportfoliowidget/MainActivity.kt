package com.zinqshere.zerodhaportfoliowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zinqshere.zerodhaportfoliowidget.data.KiteAuthClient
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRefreshWorker
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import com.zinqshere.zerodhaportfoliowidget.widget.PortfolioWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class PortfolioTheme(val key: String, val label: String) {
    PITCH_BLACK("pitch_black", "Pitch Black"),
    MONET("monet", "Monet"),
    DARK_MONET("dark_monet", "Dark Monet")
}

class MainActivity : ComponentActivity() {
    private lateinit var store: PortfolioStore
    private var authVersion by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.configure(this)
        enableEdgeToEdge()
        store = PortfolioStore(this)
        handleAuthIntent(intent)
        scheduleRefresh(this)
        setContent { PortfolioScreen(store, authVersion) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "zerodhaportfolio" || uri.host != "oauth") return
        val code = uri.getQueryParameter("code") ?: return
        val backend = store.backendUrl()
        if (backend.isBlank()) return
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { KiteAuthClient.exchangeCode(backend, code) }
            }.onSuccess { (apiKey, accessToken) ->
                store.saveKite(apiKey, accessToken)
                authVersion++
                runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                PortfolioWidgetReceiver.refresh(this@MainActivity)
            }
        }
    }

    companion object {
        fun scheduleRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<PortfolioRefreshWorker>(30, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "portfolio-refresh", ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }
    }
}

private object WindowCompat {
    fun configure(activity: ComponentActivity) {
        activity.window.statusBarColor = Color.Transparent.value.toInt()
        activity.window.navigationBarColor = Color.Transparent.value.toInt()
    }
}

@Composable
private fun PortfolioScreen(store: PortfolioStore, authVersion: Int) {
    var backendUrl by remember { mutableStateOf(store.backendUrl()) }
    var snapshot by remember(authVersion) { mutableStateOf(store.cachedSnapshot()) }
    var status by remember(authVersion) {
        mutableStateOf(if (store.accessToken().isNotBlank()) "Zerodha connected" else "Zerodha not connected")
    }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var selectedTheme by remember {
        mutableStateOf(PortfolioTheme.entries.firstOrNull { it.key == store.theme() } ?: PortfolioTheme.DARK_MONET)
    }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pinSupported = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }

    val colorScheme = when (selectedTheme) {
        PortfolioTheme.PITCH_BLACK -> pitchBlackScheme()
        PortfolioTheme.MONET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else lightColorScheme()
        PortfolioTheme.DARK_MONET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else darkMonetFallbackScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Zerodha Portfolio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("Kite equity + Coin mutual funds", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showSettings = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }

                DashboardCard(snapshot)

                SectionCard(title = "Equity", icon = Icons.Outlined.AccountBalance) {
                    HoldingSummary(
                        value = snapshot.equityValue,
                        invested = snapshot.equityInvested,
                        pnl = snapshot.equityPnl,
                        subtitle = "Fetched automatically from Kite"
                    )
                }

                SectionCard(title = "Mutual Funds", icon = Icons.Outlined.CloudSync) {
                    HoldingSummary(
                        value = snapshot.coinValue,
                        invested = snapshot.coinInvested,
                        pnl = snapshot.coinPnl,
                        subtitle = "Fetched automatically from Coin via Kite"
                    )
                }

                ElevatedCard(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.AddHome, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Home-screen widget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text("Place your portfolio card directly on the launcher.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(
                            enabled = pinSupported,
                            onClick = {
                                val manager = AppWidgetManager.getInstance(context)
                                val provider = ComponentName(context, PortfolioWidgetReceiver::class.java)
                                manager.requestPinAppWidget(provider, null, null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Add widget to home screen") }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastUpdated(snapshot.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                status = "Refreshing Zerodha…"
                                runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                                    .onSuccess { snapshot = it; status = "Zerodha connected"; connectionError = null }
                                    .onFailure {
                                        connectionError = it.message
                                        status = refreshError(it.message)
                                    }
                            }
                        },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }

                connectionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (showSettings) {
            SettingsDialog(
                selectedTheme = selectedTheme,
                onThemeSelected = {
                    selectedTheme = it
                    store.saveTheme(it.key)
                },
                backendUrl = backendUrl,
                onBackendUrlChange = { backendUrl = it; connectionError = null },
                connected = store.accessToken().isNotBlank(),
                connectionError = connectionError,
                onConnect = {
                    runCatching {
                        val value = backendUrl.trim()
                        require(value.isNotBlank()) { "Enter a backend URL" }
                        store.saveBackendUrl(value)
                        KiteAuthClient.openLogin(context, value)
                        status = "Opening Zerodha login…"
                        connectionError = null
                    }.onFailure { connectionError = it.message ?: "Enter a valid backend URL" }
                },
                onRefresh = {
                    scope.launch {
                        status = "Refreshing Zerodha…"
                        runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                            .onSuccess { snapshot = it; status = "Zerodha connected"; connectionError = null }
                            .onFailure {
                                connectionError = it.message
                                status = refreshError(it.message)
                            }
                    }
                },
                onAddWidget = {
                    val manager = AppWidgetManager.getInstance(context)
                    val provider = ComponentName(context, PortfolioWidgetReceiver::class.java)
                    manager.requestPinAppWidget(provider, null, null)
                },
                pinSupported = pinSupported,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun DashboardCard(snapshot: com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot) {
    val pnlPercent = percentage(snapshot.totalInvested, snapshot.totalPnl)
    ElevatedCard(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Total Portfolio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(money(snapshot.totalValue), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                MetricWithPercent("P&L", snapshot.totalPnl, pnlPercent)
                Metric("Invested", money(snapshot.totalInvested))
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(money(snapshot.equityDayPnl), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Equity + MF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HoldingSummary(value: Double, invested: Double, pnl: Double, subtitle: String) {
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(money(value), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        Metric("Invested", money(invested))
        MetricWithPercent("P&L", pnl, percentage(invested, pnl))
    }
}

@Composable
private fun MetricWithPercent(label: String, value: Double, percent: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(value), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(formatPercent(percent), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDialog(
    selectedTheme: PortfolioTheme,
    onThemeSelected: (PortfolioTheme) -> Unit,
    backendUrl: String,
    onBackendUrlChange: (String) -> Unit,
    connected: Boolean,
    connectionError: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onAddWidget: () -> Unit,
    pinSupported: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Text("Settings")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Choose the look that fits your device and wallpaper.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PortfolioTheme.entries.forEach { theme ->
                    val selected = selectedTheme == theme
                    OutlinedButton(
                        onClick = { onThemeSelected(theme) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(theme.label, modifier = Modifier.weight(1f))
                            if (selected) Text("✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider()
                Text("Zerodha connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "One secure Kite authorization fetches both your equity holdings and Coin mutual funds. No Coin login or CSV import is required.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConnectionRow("Kite equity", connected)
                        ConnectionRow("Coin mutual funds", connected)
                    }
                }

                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = onBackendUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Auth backend URL") },
                    singleLine = true,
                    isError = connectionError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(18.dp)
                )

                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (connected) "Reconnect Zerodha" else "Connect Zerodha")
                }

                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = connected,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Fetch equity + mutual funds now")
                }

                connectionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                HorizontalDivider()
                Text("Home-screen widget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = onAddWidget,
                    enabled = pinSupported,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Add widget to home screen") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun ConnectionRow(label: String, connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            if (connected) Icons.Outlined.CheckCircle else Icons.Outlined.CloudSync,
            contentDescription = null,
            tint = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(label, modifier = Modifier.weight(1f))
        Text(
            if (connected) "Ready" else "Waiting for connection",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun pitchBlackScheme() = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF2A0059),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF161616),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFC9C9C9)
)

private fun darkMonetFallbackScheme() = darkColorScheme(
    primary = Color(0xFFB9C8FF),
    onPrimary = Color(0xFF102D68),
    secondary = Color(0xFFBFC7E7),
    tertiary = Color(0xFFE5B9D0),
    background = Color(0xFF101318),
    surface = Color(0xFF171A20),
    surfaceVariant = Color(0xFF272B34),
    onBackground = Color(0xFFE2E5EC),
    onSurface = Color(0xFFE2E5EC),
    onSurfaceVariant = Color(0xFFC1C6D0)
)

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null) Icon(icon, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun percentage(invested: Double, pnl: Double): Double =
    if (invested == 0.0) 0.0 else (pnl / invested) * 100.0

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%+.2f%%", value)

private fun lastUpdated(timestamp: Long): String =
    if (timestamp <= 0L) "No portfolio update yet"
    else "Updated ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))}"

private fun refreshError(message: String?): String =
    when {
        message.isNullOrBlank() -> "Refresh failed"
        message.contains("401") || message.contains("403") || message.contains("unauthorized", ignoreCase = true) -> "Zerodha session expired — reconnect in Settings"
        message.contains("timeout", ignoreCase = true) || message.contains("network", ignoreCase = true) -> "Network error — try again"
        else -> "Refresh failed: $message"
    }

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}.format(value)
