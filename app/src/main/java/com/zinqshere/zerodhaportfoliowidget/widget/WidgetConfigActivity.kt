package com.zinqshere.zerodhaportfoliowidget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.view.WindowCompat

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true

        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val initialTheme = WidgetAppearance.theme(this, appWidgetId)
        val initialOpacity = WidgetAppearance.opacity(this, appWidgetId)
        val storedLayout = WidgetAppearance.layout(this, appWidgetId)
        val initialLayout = if (storedLayout == WidgetAppearance.AUTO || storedLayout == WidgetAppearance.DASHBOARD) WidgetAppearance.STANDARD else storedLayout
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            Text("Widget settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Customize this widget. Other widgets can use different settings.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            WidgetPreview(theme, opacity, layout, showToday, showBreakdown, showChart)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("Layout", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    LayoutChoice("Compact — value + return", WidgetAppearance.COMPACT, layout) { layout = it }
                                    LayoutChoice("Standard — breakdown", WidgetAppearance.STANDARD, layout) { layout = it }
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
                                }
                            }
                        }

                        Surface(tonalElevation = 3.dp) {
                            Button(
                                onClick = {
                                    WidgetAppearance.save(this@WidgetConfigActivity, appWidgetId, theme, opacity.toInt(), layout, showToday, showBreakdown, false)
                                    setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                    PortfolioWidgetReceiver.refresh(this@WidgetConfigActivity)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
                            ) { Text("Save widget") }
                        }
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
private fun WidgetPreview(theme: String, opacity: Float, layout: String, showToday: Boolean, showBreakdown: Boolean, showChart: Boolean) {
    val palette = when (theme) {
        WidgetAppearance.LIGHT -> PreviewPalette(Color(0xFFF7F7F9), Color(0xFF18171B), Color(0xFF504E56), Color(0xFF187841))
        WidgetAppearance.PITCH_BLACK -> PreviewPalette(Color.Black, Color.White, Color(0xFFCDC9D3), Color(0xFF91EBA4))
        else -> PreviewPalette(Color(0xFF252329), Color.White, Color(0xFFCDC9D3), Color(0xFF91EBA4))
    }
    val compact = layout == WidgetAppearance.COMPACT
    val cardHeight = if (compact) 112.dp else 182.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Preview", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.background.copy(alpha = opacity / 100f))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Zerodha Portfolio", color = palette.content, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("↻", color = palette.positive, style = MaterialTheme.typography.titleMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("₹12,45,820", color = palette.content, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("+₹1,84,230   +17.35%", color = palette.positive, fontWeight = FontWeight.Bold)
                    }
                    if (!compact && showToday) {
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Today", color = palette.secondary, style = MaterialTheme.typography.labelMedium)
                            Text("+₹4,280", color = palette.positive, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("+0.34%", color = palette.positive, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (!compact && showBreakdown) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.secondary.copy(alpha = 0.28f)))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PreviewMetric("Equity", "₹8.72L", "+17.88%", palette)
                        PreviewMetric("Mutual Funds", "₹3.73L", "+16.13%", palette)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Updated 2 min ago", color = palette.secondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PreviewMetric(label: String, value: String, pnl: String, palette: PreviewPalette) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, color = palette.secondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = palette.content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(pnl, color = palette.positive, style = MaterialTheme.typography.labelMedium)
    }
}

private data class PreviewPalette(val background: Color, val content: Color, val secondary: Color, val positive: Color)
