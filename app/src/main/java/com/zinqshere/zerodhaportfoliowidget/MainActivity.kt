package com.zinqshere.zerodhaportfoliowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.configure(this)
        enableEdgeToEdge()
        store = PortfolioStore(this)
        handleAuthIntent(intent)
        scheduleRefresh(this)
        setContent { PortfolioScreen(store) }
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
            runCatching { withContext(Dispatchers.IO) { KiteAuthClient.exchangeCode(backend, code) } }
                .onSuccess { (apiKey, accessToken) -> store.saveKite(apiKey, accessToken) }
        }
    }

    companion object {
        fun scheduleRefresh(context: android.content.Context) {
            val request = PeriodicWorkRequestBuilder<PortfolioRefreshWorker>(30, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.MINUTES).build()
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
private fun PortfolioScreen(store: PortfolioStore) {
    var backendUrl by remember { mutableStateOf(store.backendUrl()) }
    var coinInvested by remember { mutableStateOf(store.coinInvested().toString().removeSuffix(".0")) }
    var coinValue by remember { mutableStateOf(store.coinValue().toString().removeSuffix(".0")) }
    var snapshot by remember { mutableStateOf(store.cachedSnapshot()) }
    var status by remember { mutableStateOf(if (store.accessToken().isNotBlank()) "Kite connected" else "Kite not connected") }
    var kiteError by remember { mutableStateOf<String?>(null) }
    var selectedTheme by remember {
        mutableStateOf(PortfolioTheme.entries.firstOrNull { it.key == store.theme() } ?: PortfolioTheme.DARK_MONET)
    }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pinSupported = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { parseCoinCsv(context, uri) } }
                .onSuccess { totals ->
                    store.saveCoin(totals.first, totals.second)
                    coinInvested = totals.first.toString()
                    coinValue = totals.second.toString()
                    snapshot = snapshot.copy(
                        coinInvested = totals.first,
                        coinValue = totals.second,
                        coinPnl = totals.second - totals.first,
                        updatedAt = System.currentTimeMillis()
                    )
                    store.saveSnapshot(snapshot)
                    status = "Coin CSV imported"
                }.onFailure { status = "Coin import failed: ${it.message}" }
        }
    }

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
                        subtitle = "Kite holdings"
                    )
                }

                SectionCard(title = "Mutual Funds", icon = Icons.Outlined.UploadFile) {
                    HoldingSummary(
                        value = snapshot.coinValue,
                        invested = snapshot.coinInvested,
                        pnl = snapshot.coinPnl,
                        subtitle = "Coin mutual funds"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) },
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Import CSV") }
                        OutlinedButton(
                            onClick = {
                                val invested = coinInvested.toDoubleOrNull() ?: 0.0
                                val value = coinValue.toDoubleOrNull() ?: 0.0
                                store.saveCoin(invested, value)
                                snapshot = snapshot.copy(
                                    coinInvested = invested,
                                    coinValue = value,
                                    coinPnl = value - invested,
                                    updatedAt = System.currentTimeMillis()
                                )
                                store.saveSnapshot(snapshot)
                                status = "Coin totals saved"
                            },
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Save totals") }
                    }
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
                                status = "Refreshing Kite…"
                                runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                                    .onSuccess { snapshot = it; status = "Kite connected" }
                                    .onFailure { status = refreshError(it.message) }
                            }
                        },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
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
                onBackendUrlChange = { backendUrl = it; kiteError = null },
                kiteConnected = store.accessToken().isNotBlank(),
                kiteError = kiteError,
                onConnectKite = {
                    runCatching {
                        val value = backendUrl.trim()
                        require(value.isNotBlank()) { "Enter a backend URL" }
                        store.saveBackendUrl(value)
                        KiteAuthClient.openLogin(context, value)
                        status = "Opening Kite login…"
                    }.onFailure { kiteError = it.message ?: "Enter a valid backend URL" }
                },
                onImportCoin = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) },
                onOpenCoin = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://coin.zerodha.com"))) },
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
    val dayPnl = snapshot.equityDayPnl
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
                    Text(money(dayPnl), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Equity + MF",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    kiteConnected: Boolean,
    kiteError: String?,
    onConnectKite: () -> Unit,
    onImportCoin: () -> Unit,
    onOpenCoin: () -> Unit,
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
                Text("Kite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (kiteConnected) "Kite session is saved on this device." else "Kite is not connected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = onBackendUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Auth backend URL") },
                    singleLine = true,
                    isError = kiteError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(18.dp)
                )
                Button(onClick = onConnectKite, shape = RoundedCornerShape(18.dp)) { Text("Connect Kite") }
                kiteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                HorizontalDivider()
                Text("Coin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onImportCoin, shape = RoundedCornerShape(18.dp)) { Text("Import CSV") }
                    OutlinedButton(onClick = onOpenCoin, shape = RoundedCornerShape(18.dp)) { Text("Open Coin") }
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
        message.contains("401") || message.contains("403") || message.contains("unauthorized", ignoreCase = true) -> "Kite session expired — reconnect in Settings"
        message.contains("timeout", ignoreCase = true) || message.contains("network", ignoreCase = true) -> "Network error — try again"
        else -> "Refresh failed: $message"
    }

private fun parseCoinCsv(context: android.content.Context, uri: Uri): Pair<Double, Double> {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Could not read file")
    val rows = text.lineSequence().filter { it.isNotBlank() }.map { parseCsvLine(it) }.toList()
    if (rows.size < 2) error("CSV has no data rows")
    val headers = rows.first().map { it.trim().lowercase(Locale.ROOT) }
    fun find(vararg names: String): Int = names.firstNotNullOfOrNull { name -> headers.indexOf(name).takeIf { it >= 0 } } ?: error("Missing column: ${names.first()}")
    val investedIndex = find("invested amount", "invested", "total invested", "investment")
    val valueIndex = find("current amount", "current value", "current", "market value", "value")
    var invested = 0.0
    var value = 0.0
    rows.drop(1).forEach { row ->
        invested += row.getOrNull(investedIndex).orEmpty().replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
        value += row.getOrNull(valueIndex).orEmpty().replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
    }
    return invested to value
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var i = 0
    while (i < line.length) {
        when (val c = line[i]) {
            '"' -> {
                if (quoted && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else quoted = !quoted
            }
            ',' -> if (quoted) current.append(c) else { result += current.toString(); current.setLength(0) }
            else -> current.append(c)
        }
        i++
    }
    result += current.toString()
    return result
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }.format(value)