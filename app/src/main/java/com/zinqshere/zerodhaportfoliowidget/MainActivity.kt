package com.zinqshere.zerodhaportfoliowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
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
import androidx.compose.material.icons.outlined.Refresh
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
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                    snapshot = snapshot.copy(coinInvested = totals.first, coinValue = totals.second, coinPnl = totals.second - totals.first, updatedAt = System.currentTimeMillis())
                    store.saveSnapshot(snapshot)
                    status = "Coin CSV imported"
                }.onFailure { status = "Coin import failed: ${it.message}" }
        }
    }

    val darkScheme = darkColorScheme(
        primary = Color(0xFFBCA7FF),
        onPrimary = Color(0xFF2A0059),
        secondary = Color(0xFFD6BEE8),
        tertiary = Color(0xFFFFB0CA),
        background = Color(0xFF0B0B0F),
        surface = Color(0xFF141419),
        surfaceVariant = Color(0xFF202027),
        onBackground = Color(0xFFE7E1E8),
        onSurface = Color(0xFFE7E1E8),
        onSurfaceVariant = Color(0xFFC7BEC9)
    )

    MaterialTheme(colorScheme = darkScheme) {
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Zerodha Portfolio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("Kite equity + Coin mutual funds", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                ElevatedCard(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.AddHome, null)
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

                SectionCard(title = "Kite", icon = Icons.Outlined.AccountBalance) {
                    Text("Connect securely through Kite. Your API secret stays on the backend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it; kiteError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Auth backend URL") },
                        placeholder = { Text("https://your-backend.example.com") },
                        singleLine = true,
                        isError = kiteError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(18.dp)
                    )
                    Button(
                        enabled = backendUrl.trim().isNotBlank(),
                        onClick = {
                            runCatching {
                                val value = backendUrl.trim()
                                store.saveBackendUrl(value)
                                KiteAuthClient.openLogin(context, value)
                                status = "Opening Kite login…"
                            }.onFailure { kiteError = it.message ?: "Enter a valid HTTPS backend URL" }
                        },
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("Connect Kite") }
                    kiteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (store.accessToken().isNotBlank()) Text("Kite session saved securely on this device.", color = Color(0xFF9DDFB2))
                }

                SectionCard(title = "Coin", icon = Icons.Outlined.UploadFile) {
                    Text("Use manual totals or import your Coin CSV.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(coinInvested, { coinInvested = it }, Modifier.weight(1f), label = { Text("Invested") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(18.dp))
                        OutlinedTextField(coinValue, { coinValue = it }, Modifier.weight(1f), label = { Text("Current value") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(18.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            val invested = coinInvested.toDoubleOrNull() ?: 0.0
                            val value = coinValue.toDoubleOrNull() ?: 0.0
                            store.saveCoin(invested, value)
                            snapshot = snapshot.copy(coinInvested = invested, coinValue = value, coinPnl = value - invested, updatedAt = System.currentTimeMillis())
                            store.saveSnapshot(snapshot)
                            status = "Coin totals saved"
                        }, shape = RoundedCornerShape(18.dp)) { Text("Save") }
                        OutlinedButton(onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) }, shape = RoundedCornerShape(18.dp)) { Text("Import CSV") }
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://coin.zerodha.com"))) }, shape = RoundedCornerShape(18.dp)) { Text("Open Coin") }
                }

                SectionCard(title = "Portfolio") {
                    Text(money(snapshot.totalValue), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Metric("Invested", money(snapshot.totalInvested))
                        Metric("P&L", money(snapshot.totalPnl))
                        Metric("Day P&L", money(snapshot.equityDayPnl))
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(status, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = {
                            scope.launch {
                                status = "Refreshing Kite…"
                                runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                                    .onSuccess { snapshot = it; status = "Updated just now" }
                                    .onFailure { status = it.message ?: "Refresh failed" }
                            }
                        }, shape = RoundedCornerShape(18.dp)) {
                            Icon(Icons.Outlined.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(28.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null) Icon(icon, null)
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
    if (invested == 0.0 && value == 0.0) error("No numeric investment totals found")
    return invested to value
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var i = 0
    while (i < line.length) {
        when (val c = line[i]) {
            '"' -> if (quoted && i + 1 < line.length && line[i + 1] == '"') { cell.append('"'); i++ } else quoted = !quoted
            ',' -> if (quoted) cell.append(c) else { result += cell.toString(); cell.clear() }
            else -> cell.append(c)
        }
        i++
    }
    result += cell.toString()
    return result
}

private fun money(v: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(v)
