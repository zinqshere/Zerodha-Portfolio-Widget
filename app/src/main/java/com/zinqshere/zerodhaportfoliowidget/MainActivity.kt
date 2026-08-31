package com.zinqshere.zerodhaportfoliowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            setIntent(Intent(this@MainActivity, MainActivity::class.java))
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
                    coinInvested = totals.first.toString(); coinValue = totals.second.toString()
                    snapshot = snapshot.copy(coinInvested = totals.first, coinValue = totals.second, coinPnl = totals.second - totals.first, updatedAt = System.currentTimeMillis())
                    store.saveSnapshot(snapshot); status = "Coin CSV imported"
                }.onFailure { status = "Coin import failed: ${it.message}" }
        }
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Zerodha Portfolio Widget", style = MaterialTheme.typography.headlineSmall)
            Text("Kite equity + Coin mutual funds", style = MaterialTheme.typography.bodyMedium)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Home-screen widget", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (pinSupported) "Add the portfolio widget directly from this app. Your launcher will ask you to confirm the placement."
                        else "Your current launcher does not support one-tap widget placement. Use Home screen → Widgets → Zerodha Portfolio Widget.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        enabled = pinSupported,
                        onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            val provider = ComponentName(context, PortfolioWidgetReceiver::class.java)
                            manager.requestPinAppWidget(provider, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add widget to home screen") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kite", style = MaterialTheme.typography.titleMedium)
                    Text("Connect through the supported Kite login flow. Your API secret stays on the backend and never enters this app.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(backendUrl, { backendUrl = it; kiteError = null }, Modifier.fillMaxWidth(), label = { Text("Auth backend URL") }, placeholder = { Text("https://your-backend.example.com") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), isError = kiteError != null)
                    Button(
                        enabled = backendUrl.trim().isNotBlank(),
                        onClick = {
                            val value = backendUrl.trim()
                            runCatching {
                                store.saveBackendUrl(value)
                                KiteAuthClient.openLogin(context, value)
                                status = "Opening Kite login…"
                                kiteError = null
                            }.onFailure { kiteError = it.message ?: "Enter a valid HTTPS backend URL" }
                        }
                    ) { Text("Connect Kite") }
                    if (kiteError != null) Text(kiteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else if (backendUrl.isBlank()) Text("Enter your deployed auth backend URL first. The app cannot start the Kite login until the backend is configured.", style = MaterialTheme.typography.bodySmall)
                    if (store.accessToken().isNotBlank()) Text("Kite session saved securely on this device.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Coin", style = MaterialTheme.typography.titleMedium)
                    Text("Coin is included through manual totals or CSV import; we do not scrape your Coin session.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(coinInvested, { coinInvested = it }, Modifier.weight(1f), label = { Text("Invested") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(coinValue, { coinValue = it }, Modifier.weight(1f), label = { Text("Current value") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val invested = coinInvested.toDoubleOrNull() ?: 0.0; val value = coinValue.toDoubleOrNull() ?: 0.0
                            store.saveCoin(invested, value); snapshot = snapshot.copy(coinInvested = invested, coinValue = value, coinPnl = value - invested, updatedAt = System.currentTimeMillis()); store.saveSnapshot(snapshot); status = "Coin totals saved"
                        }) { Text("Save") }
                        OutlinedButton(onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) }) { Text("Import CSV") }
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://coin.zerodha.com"))) }) { Text("Open Coin") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Portfolio", style = MaterialTheme.typography.titleMedium)
                    Text(money(snapshot.totalValue), style = MaterialTheme.typography.headlineMedium)
                    Text("Invested ${money(snapshot.totalInvested)}")
                    Text("P&L ${money(snapshot.totalPnl)}")
                    Text("Day P&L ${money(snapshot.equityDayPnl)}")
                    Text(status, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        scope.launch {
                            status = "Refreshing Kite…"
                            runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                                .onSuccess { snapshot = it; status = "Updated just now" }
                                .onFailure { status = it.message ?: "Refresh failed" }
                        }
                    }) { Text("Refresh now") }
                }
            }
        }
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
    var invested = 0.0; var value = 0.0
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
        }; i++
    }
    result += cell.toString(); return result
}

private fun money(v: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(v)
