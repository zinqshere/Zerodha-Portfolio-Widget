package com.zinqshere.zerodhaportfoliowidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
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
    var value by remember { mutableStateOf(0.0) }
    var pnl by remember { mutableStateOf(0.0) }

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
                        status = "Credentials saved. Refreshing…"
                    }) { Text("Save Kite") }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Coin", style = MaterialTheme.typography.titleMedium)
                    Text("Coin does not expose the same supported public portfolio API as Kite. For now, enter the latest Coin totals; the widget combines them with live Kite data.", style = MaterialTheme.typography.bodySmall)
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
                    Text("Widget preview", style = MaterialTheme.typography.titleMedium)
                    Text(money(value), style = MaterialTheme.typography.headlineMedium)
                    Text("P&L ${money(pnl)}")
                    Text(status, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        status = "Refreshing…"
                    }) { Text("Refresh") }
                }
            }
        }
    }
}

private fun money(v: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(v)
