package com.zinqshere.zerodhaportfoliowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zinqshere.zerodhaportfoliowidget.data.KiteAuthClient
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRefreshWorker
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import com.zinqshere.zerodhaportfoliowidget.data.ZerodhaAccount
import com.zinqshere.zerodhaportfoliowidget.widget.PortfolioWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class PortfolioTheme(val key: String, val label: String) {
    PITCH_BLACK("pitch_black", "Pitch Black"), MONET("monet", "Monet"), DARK_MONET("dark_monet", "Dark Monet")
}

private enum class RefreshInterval(val minutes: Long, val label: String) {
    FIFTEEN(15, "Every 15 minutes"), THIRTY(30, "Every 30 minutes"), SIXTY(60, "Every 1 hour"),
    ONE_TWENTY(120, "Every 2 hours"), TWO_FORTY(240, "Every 4 hours")
}

class MainActivity : ComponentActivity() {
    private lateinit var store: PortfolioStore
    private var authVersion by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            runCatching { withContext(Dispatchers.IO) { KiteAuthClient.exchangeCode(backend, code) } }
                .onSuccess { (apiKey, accessToken, state) ->
                    val target = state ?: store.pendingAccountId() ?: store.accounts().firstOrNull()?.id
                    if (target != null) store.saveKiteForAccount(target, apiKey, accessToken)
                    store.clearPendingAccountId()
                    authVersion++
                    runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                    PortfolioWidgetReceiver.refresh(this@MainActivity)
                }
        }
    }

    companion object {
        fun scheduleRefresh(context: Context) {
            val minutes = PortfolioStore(context).refreshIntervalMinutes().coerceIn(15L, 1440L)
            val request = PeriodicWorkRequestBuilder<PortfolioRefreshWorker>(minutes, TimeUnit.MINUTES)
                .setInitialDelay(minutes, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("portfolio-refresh", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}

@Composable
private fun PortfolioScreen(store: PortfolioStore, authVersion: Int) {
    val context = LocalContext.current
    var accounts by remember(authVersion) { mutableStateOf(store.accounts()) }
    var snapshot by remember(authVersion) { mutableStateOf(store.combinedSnapshot()) }
    var backendUrl by remember { mutableStateOf(store.backendUrl()) }
    var status by remember(authVersion) { mutableStateOf(if (accounts.any { it.connected }) "Zerodha connected" else "Add a Zerodha account") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAccounts by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var renameAccount by remember { mutableStateOf<ZerodhaAccount?>(null) }
    var selectedTheme by remember { mutableStateOf(PortfolioTheme.entries.firstOrNull { it.key == store.theme() } ?: PortfolioTheme.DARK_MONET) }
    var refreshInterval by remember { mutableStateOf(RefreshInterval.entries.firstOrNull { it.minutes == store.refreshIntervalMinutes() } ?: RefreshInterval.THIRTY) }
    val scope = rememberCoroutineScope()
    val pinSupported = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }

    val scheme = when (selectedTheme) {
        PortfolioTheme.PITCH_BLACK -> darkColorScheme(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF161616))
        PortfolioTheme.MONET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else lightColorScheme()
        PortfolioTheme.DARK_MONET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else darkColorScheme()
    }

    fun refresh() {
        scope.launch {
            status = "Refreshing Zerodha accounts…"
            runCatching { withContext(Dispatchers.IO) { PortfolioRepository(store).refresh() } }
                .onSuccess { snapshot = it; accounts = store.accounts(); status = "${accounts.count { a -> a.connected }} account(s) connected"; error = null; PortfolioWidgetReceiver.refresh(context) }
                .onFailure { error = it.message; status = "Some accounts could not be refreshed" }
        }
    }

    MaterialTheme(colorScheme = scheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Zerodha Portfolio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("Multiple accounts • Equity + Mutual Funds", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                }

                ElevatedCard(shape = RoundedCornerShape(30.dp)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("All accounts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(snapshot.totalValue), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Metric("Invested", money(snapshot.totalInvested))
                            Metric("Returns", "${signedMoney(snapshot.totalPnl)}  ${signedPercent(percent(snapshot.totalPnl, snapshot.totalInvested))}")
                        }
                    }
                }

                SectionCard("Zerodha accounts", Icons.Outlined.AccountBalance) {
                    if (accounts.isEmpty()) Text("No accounts yet. Add your first Zerodha account below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    accounts.forEach { account ->
                        AccountRow(
                            account = account,
                            onReconnect = {
                                if (backendUrl.isBlank()) { showSettings = true; error = "Enter your backend URL first" }
                                else { store.setPendingAccountId(account.id); KiteAuthClient.openLogin(context, backendUrl, account.id); status = "Opening ${account.label} login…" }
                            },
                            onRename = { renameAccount = account },
                            onDelete = { store.removeAccount(account.id); accounts = store.accounts(); snapshot = store.combinedSnapshot(); PortfolioWidgetReceiver.refresh(context) }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (backendUrl.isBlank()) { showSettings = true; error = "Enter your backend URL first" }
                            else {
                                val account = store.addAccount("Account ${store.accounts().size + 1}")
                                accounts = store.accounts(); store.setPendingAccountId(account.id)
                                KiteAuthClient.openLogin(context, backendUrl, account.id); status = "Opening ${account.label} login…"
                            }
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                    ) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Zerodha account") }
                }

                SectionCard("Portfolio", Icons.Outlined.Refresh) {
                    PortfolioLine("Equity", snapshot.equityValue, snapshot.equityInvested, snapshot.equityPnl)
                    PortfolioLine("Mutual Funds", snapshot.coinValue, snapshot.coinInvested, snapshot.coinPnl)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (snapshot.updatedAt == 0L) "No portfolio refresh yet" else "Updated ${relativeTime(snapshot.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { refresh() }, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Refresh") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("Each widget can be assigned to one account or All accounts. Combined returns are calculated from aggregated invested/current values, not averaged percentages.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showAccounts) { /* account management is on the main screen */ }
        if (showSettings) {
            SettingsDialog(
                theme = selectedTheme, onTheme = { selectedTheme = it; store.saveTheme(it.key) },
                interval = refreshInterval, onInterval = { refreshInterval = it; store.saveRefreshInterval(it.minutes); MainActivity.scheduleRefresh(context) },
                backendUrl = backendUrl, onBackendUrl = { backendUrl = it; store.saveBackendUrl(it); error = null },
                onDismiss = { showSettings = false }, onRefresh = { refresh() }, pinSupported = pinSupported,
                onAddWidget = { AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, PortfolioWidgetReceiver::class.java), null, null) }
            )
        }
        renameAccount?.let { account ->
            var value by remember(account.id) { mutableStateOf(account.label) }
            AlertDialog(
                onDismissRequest = { renameAccount = null }, title = { Text("Rename account") },
                text = { OutlinedTextField(value, { value = it }, label = { Text("Account name") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { store.renameAccount(account.id, value); accounts = store.accounts(); renameAccount = null }) { Text("Save") } },
                dismissButton = { TextButton(onClick = { renameAccount = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable private fun AccountRow(account: ZerodhaAccount, onReconnect: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(account.label, fontWeight = FontWeight.SemiBold)
                Text(if (account.connected) "Connected" else "Needs login", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onReconnect) { Text(if (account.connected) "Reconnect" else "Connect") }
            IconButton(onClick = onRename) { Icon(Icons.Outlined.Edit, "Rename") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Remove") }
        }
    }
}

@Composable private fun PortfolioLine(label: String, value: Double, invested: Double, pnl: Double) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text(money(value), style = MaterialTheme.typography.titleMedium) }
        Column(horizontalAlignment = Alignment.End) { Text("Invested ${money(invested)}", style = MaterialTheme.typography.bodySmall); Text("${signedMoney(pnl)}  ${signedPercent(percent(pnl, invested))}", color = if (pnl >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }; content() } }
}

@Composable private fun Metric(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) } }

@Composable private fun SettingsDialog(
    theme: PortfolioTheme, onTheme: (PortfolioTheme) -> Unit,
    interval: RefreshInterval, onInterval: (RefreshInterval) -> Unit,
    backendUrl: String, onBackendUrl: (String) -> Unit,
    onDismiss: () -> Unit, onRefresh: () -> Unit, pinSupported: Boolean, onAddWidget: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Zerodha backend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(backendUrl, onBackendUrl, label = { Text("HTTPS backend URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("The backend keeps your Kite API secret off the phone. Each Zerodha account gets its own access token stored in encrypted Android storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                PortfolioTheme.entries.forEach { OutlinedButton(onClick = { onTheme(it) }, modifier = Modifier.fillMaxWidth()) { Text(if (it == theme) "✓ ${it.label}" else it.label) } }
                HorizontalDivider()
                Text("Refresh interval", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                RefreshInterval.entries.forEach { OutlinedButton(onClick = { onInterval(it) }, modifier = Modifier.fillMaxWidth()) { Text(if (it == interval) "✓ ${it.label}" else it.label) } }
                HorizontalDivider()
                Text("Widgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (pinSupported) Button(onClick = onAddWidget, modifier = Modifier.fillMaxWidth()) { Text("Add portfolio widget") }
                Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh all accounts now") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

private fun percent(pnl: Double, invested: Double) = if (invested == 0.0) 0.0 else pnl / invested * 100.0
private fun signedPercent(value: Double) = if (value >= 0) "+${"%.2f".format(Locale.US, value)}%" else "${"%.2f".format(Locale.US, value)}%"
private fun money(value: Double) = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(value)
private fun signedMoney(value: Double) = if (value >= 0) "+${money(value)}" else "-${money(-value)}"
private fun relativeTime(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0L)
    return when { minutes < 1 -> "just now"; minutes < 60 -> "${minutes}m ago"; minutes < 1440 -> "${minutes / 60}h ago"; else -> "${minutes / 1440}d ago" }
}
