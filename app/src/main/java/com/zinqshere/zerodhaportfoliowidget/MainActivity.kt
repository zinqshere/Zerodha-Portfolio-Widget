package com.zinqshere.zerodhaportfoliowidget

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PortfolioScreen(PortfolioStore(this)) }
    }
}

@Composable
private fun PortfolioScreen(store: PortfolioStore) {
    var apiKey by remember { mutableStateOf(store.apiKey()) }
    var accessToken by remember { mutableStateOf(store.accessToken()) }
    var coinInvested by remember { mutableStateOf(store.coinInvested().toString().removeSuffix(".0")) }
    var coinValue by remember { mutableStateOf(store.coinValue().toString().removeSuffix(".0")) }
    var snapshot by remember { mutableStateOf(store.cachedSnapshot()) }
    var status by remember { mutableStateOf(if (snapshot.updatedAt > 0) "Last cached portfolio loaded" else "Not connected") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { parseCoinCsv(context, uri) }
                }.onSuccess { totals ->
                    store.saveCoin(totals.first, totals.second)
                    coinInvested = totals.first.toString()
                    coinValue = totals.second.toString()
                    snapshot = store.cachedSnapshot().copy(
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
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Zerodha Portfolio Widget", style = MaterialTheme.typography.headlineSmall)
            Text("Kite equity + Coin mutual funds", style = MaterialTheme.typography.bodyMedium)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kite", style = MaterialTheme.typography.titleMedium)
                    Text("Kite access tokens expire at 6 AM the next day. For a production mobile app, Zerodha requires the token-exchange secret to stay on a backend; this MVP therefore accepts the generated access token directly.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("API key") }, singleLine = true)
                    OutlinedTextField(accessToken, { accessToken = it }, Modifier.fillMaxWidth(), label = { Text("Access token") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    Button(onClick = {
                        store.saveKite(apiKey.trim(), accessToken.trim())
                        status = "Kite credentials saved"
                    }) { Text("Save Kite") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Coin", style = MaterialTheme.typography.titleMedium)
                    Text("Zerodha currently documents Coin portfolio viewing in the Coin app/web rather than a public Coin portfolio API. The app supports importing a CSV containing invested/current totals so Coin can still be included without scraping your account.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(coinInvested, { coinInvested = it }, Modifier.weight(1f), label = { Text("Invested") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(coinValue, { coinValue = it }, Modifier.weight(1f), label = { Text("Current value") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val invested = coinInvested.toDoubleOrNull() ?: 0.0
                            val value = coinValue.toDoubleOrNull() ?: 0.0
                            store.saveCoin(invested, value)
                            snapshot = snapshot.copy(coinInvested = invested, coinValue = value, coinPnl = value - invested, updatedAt = System.currentTimeMillis())
                            store.saveSnapshot(snapshot)
                            status = "Coin totals saved"
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
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: error("Could not read file")
    val rows = text.lineSequence().filter { it.isNotBlank() }.map { parseCsvLine(it) }.toList()
    if (rows.size < 2) error("CSV has no data rows")
    val headers = rows.first().map { it.trim().lowercase(Locale.ROOT) }
    fun find(vararg names: String): Int = names.firstNotNullOfOrNull { name -> headers.indexOf(name).takeIf { it >= 0 } }
        ?: error("Missing column: ${names.first()}")
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
