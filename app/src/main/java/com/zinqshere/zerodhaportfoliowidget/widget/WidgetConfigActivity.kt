package com.zinqshere.zerodhaportfoliowidget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val initialTheme = WidgetAppearance.theme(this, appWidgetId)
        val initialOpacity = WidgetAppearance.opacity(this, appWidgetId)
        val initialLayout = WidgetAppearance.layout(this, appWidgetId)
        val initialToday = WidgetAppearance.showToday(this, appWidgetId)
        val initialBreakdown = WidgetAppearance.showBreakdown(this, appWidgetId)
        val initialChart = WidgetAppearance.showChart(this, appWidgetId)

        setContent {
            var theme by remember { mutableStateOf(initialTheme) }
            var opacity by remember { mutableFloatStateOf(initialOpacity.toFloat()) }
            var layout by remember { mutableStateOf(initialLayout) }
            var showToday by remember { mutableStateOf(initialToday) }
            var showBreakdown by remember { mutableStateOf(initialBreakdown) }
            var showChart by remember { mutableStateOf(initialChart) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Widget settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Each widget on your home screen can have its own layout, theme and metrics.", style = MaterialTheme.typography.bodyMedium)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Layout", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                LayoutChoice("Auto — adapts to widget size", WidgetAppearance.AUTO, layout) { layout = it }
                                LayoutChoice("Compact — value + return", WidgetAppearance.COMPACT, layout) { layout = it }
                                LayoutChoice("Standard — breakdown", WidgetAppearance.STANDARD, layout) { layout = it }
                                LayoutChoice("Dashboard — breakdown + chart", WidgetAppearance.DASHBOARD, layout) { layout = it }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                LayoutChoice("Light / Monet", WidgetAppearance.LIGHT, theme) { theme = it }
                                LayoutChoice("Dark Monet", WidgetAppearance.DARK, theme) { theme = it }
                                LayoutChoice("Pitch black", WidgetAppearance.PITCH_BLACK, theme) { theme = it }
                                Spacer(Modifier.height(4.dp))
                                Text("Opacity ${opacity.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 20f..100f, steps = 15)
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Information", style = MaterialTheme.typography.titleMedium)
                                SettingSwitch("Today's P&L", showToday) { showToday = it }
                                SettingSwitch("Equity + mutual fund breakdown", showBreakdown) { showBreakdown = it }
                                SettingSwitch("Performance chart (dashboard)", showChart) { showChart = it }
                            }
                        }

                        Preview(theme, opacity, layout, showToday, showBreakdown, showChart)
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                WidgetAppearance.save(this@WidgetConfigActivity, appWidgetId, theme, opacity.toInt(), layout, showToday, showBreakdown, showChart)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                PortfolioWidgetReceiver.refresh(this@WidgetConfigActivity)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save widget") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutChoice(label: String, value: String, selected: String, onSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == value, onClick = { onSelected(value) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun Preview(theme: String, opacity: Float, layout: String, showToday: Boolean, showBreakdown: Boolean, showChart: Boolean) {
    val background = when (theme) {
        WidgetAppearance.LIGHT -> Color(0xFFF7F7F9)
        WidgetAppearance.PITCH_BLACK -> Color.Black
        else -> Color(0xFF252329)
    }
    val content = if (theme == WidgetAppearance.LIGHT) Color(0xFF18171B) else Color.White
    val alpha = opacity / 100f
    val dashboard = layout == WidgetAppearance.DASHBOARD
    val standard = layout == WidgetAppearance.STANDARD || layout == WidgetAppearance.AUTO

    Column {
        Text("Preview", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier.fillMaxWidth().height(if (dashboard) 160.dp else 120.dp)
                .clip(RoundedCornerShape(28.dp)).background(background.copy(alpha = alpha)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("Zerodha Portfolio", color = content, fontWeight = FontWeight.SemiBold)
            Text("₹12,45,820", color = content, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("+₹1,84,230  +17.35%", color = Color(0xFF7EDC96), fontWeight = FontWeight.Bold)
            if (showToday && (standard || dashboard)) Text("Today  +₹4,280  +0.34%", color = Color(0xFF7EDC96), style = MaterialTheme.typography.bodySmall)
            if (showChart && dashboard) Text("╱╲╱╲╱╲╱╱", color = Color(0xFF9C8CFF), style = MaterialTheme.typography.titleMedium)
            if (showBreakdown && (standard || dashboard)) Text("Equity ₹8.72L     MF ₹3.73L", color = content, style = MaterialTheme.typography.bodySmall)
            Text("Updated 2 min ago", color = content.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
