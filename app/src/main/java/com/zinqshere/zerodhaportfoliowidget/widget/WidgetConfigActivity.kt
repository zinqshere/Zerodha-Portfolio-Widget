package com.zinqshere.zerodhaportfoliowidget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zinqshere.zerodhaportfoliowidget.MainActivity
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { setResult(Activity.RESULT_CANCELED); finish(); return }

        val store = PortfolioStore(this)
        val accounts = store.accounts()
        val initialAccount = WidgetAppearance.accountId(this, appWidgetId).takeIf { it == WidgetAppearance.ALL_ACCOUNTS || accounts.any { a -> a.id == it } } ?: WidgetAppearance.ALL_ACCOUNTS
        val initialTheme = WidgetAppearance.theme(this, appWidgetId)
        val initialLayout = WidgetAppearance.layout(this, appWidgetId)
        val initialToday = WidgetAppearance.showToday(this, appWidgetId)
        val initialBreakdown = WidgetAppearance.showBreakdown(this, appWidgetId)

        setContent {
            var accountId by remember { mutableStateOf(initialAccount) }
            var theme by remember { mutableStateOf(initialTheme) }
            var layout by remember { mutableStateOf(initialLayout) }
            var showToday by remember { mutableStateOf(initialToday) }
            var showBreakdown by remember { mutableStateOf(initialBreakdown) }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Widget settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Choose which Zerodha portfolio this widget displays. You can use a different account for every widget.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Zerodha account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                AccountChoice("All accounts — combined portfolio", WidgetAppearance.ALL_ACCOUNTS, accountId) { accountId = it }
                                accounts.forEach { account ->
                                    AccountChoice("${account.label} — ${if (account.connected) "connected" else "needs login"}", account.id, accountId) { accountId = it }
                                }
                                OutlinedButton(onClick = { startActivity(Intent(this@WidgetConfigActivity, MainActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Manage / add Zerodha accounts")
                                }
                            }
                        }

                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                AccountChoice("Compact — value + returns", WidgetAppearance.COMPACT, layout) { layout = it }
                                AccountChoice("Standard — breakdown", WidgetAppearance.STANDARD, layout) { layout = it }
                                AccountChoice("Dashboard", WidgetAppearance.DASHBOARD, layout) { layout = it }
                            }
                        }

                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                AccountChoice("Light / Monet", WidgetAppearance.LIGHT, theme) { theme = it }
                                AccountChoice("Dark Monet", WidgetAppearance.DARK, theme) { theme = it }
                                AccountChoice("Pitch black", WidgetAppearance.PITCH_BLACK, theme) { theme = it }
                                SwitchRow("Today's P&L", showToday) { showToday = it }
                                SwitchRow("Equity + mutual fund breakdown", showBreakdown) { showBreakdown = it }
                            }
                        }

                        Button(onClick = {
                            WidgetAppearance.save(this@WidgetConfigActivity, appWidgetId, theme, WidgetAppearance.opacity(this@WidgetConfigActivity, appWidgetId), layout, showToday, showBreakdown, false, accountId)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            PortfolioWidgetReceiver.refresh(this@WidgetConfigActivity)
                            finish()
                        }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50)) { Text("Save widget") }
                    }
                }
            }
        }
    }
}

@Composable private fun AccountChoice(label: String, value: String, selected: String, onSelected: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == selected, onClick = { onSelected(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChecked)
    }
}
