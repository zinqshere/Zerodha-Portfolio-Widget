package com.zinqshere.zerodhaportfoliowidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = PortfolioStore(this)
        setContent { PortfolioScreen(store) }
    }
}

@Composable
private fun PortfolioScreen(store: PortfolioStore) {
    var apiKey by remember { mutableStateOf(store.apiKey()) }
    var accessToken by remember { mutableStateOf(store.accessToken()) }
    var coinInvested by remember { mutableStateOf(store.coinInvested().toString().removeSuffix(".0")) }
    var coinValue by remember { mutableStateOf(store.coinValue().toString().removeSuffix(".0")) }
    var status by remember { mutableStateOf("Add your Kite credentials to begin") }
    var snapshot by remember { mutableStateOf(PortfolioSnapshot()) }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Zerodha Portfolio Widget", style = MaterialTheme.typography.headlineSmall)
            Text("Kite equity + Coin mutual-fund summary", style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kite", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("API key") }, singleLine = true)
                    OutlinedTextField(accessToken, { accessToken = it }, Modifier.fillMaxWidth(), label = { Text("Access token") }, singleLine = true)
                    Button(onClick = {
                        store.saveKite(apiKey.trim(), accessToken.trim())
                        status = "Credentials saved."
                    }) { Text("Save Kite") }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Coin", style = MaterialTheme.typography.titleMedium)
                    Text("Coin currently uses a manual total in this MVP because Zerodha does not expose a supported public Coin portfolio API equivalent to Kite Connect.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(coinInvested, { coinInvested = it }, Modifier.weight(1f), label = { Text("Invested") }, singleLine = true)
                        OutlinedTextField(coinValue, { coinValue = it }, Modifier.weight(1f), label = { Text("Current value") }, singleLine = true)
                    }
                    Button(onClick = {
                        store.saveCoin(coinInvested.toDoubleOrNull() ?: 0.0, coinValue.toDoubleOrNull() ?: 0.0)
                        status = "Coin totals saved"
                    }) { Text("Save Coin") }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Portfolio", style = MaterialTheme.typography.titleMedium)
                    Text(money(snapshot.totalValue), style = MaterialTheme.typography.headlineMedium)
                    Text("P&L ${money(snapshot.totalPnl)}")
                    Text("Day P&L ${money(snapshot.equityDayPnl)}")
                    Text(status, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        scope.launch {
                            status = "Refreshing…"
                            runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                                .onSuccess { snapshot = it; status = "Updated just now" }
                                .onFailure { status = it.message ?: "Refresh failed" }
                        }
                    }) { Text("Refresh Kite") }
                }
            }
        }
    }
}

private fun money(v: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(v)
