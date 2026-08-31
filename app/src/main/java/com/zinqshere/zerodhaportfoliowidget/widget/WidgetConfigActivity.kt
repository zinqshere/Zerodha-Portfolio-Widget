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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            Text(
                                "Widget settings",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Customize this widget. Other widgets can use different settings.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(14.dp))
                            WidgetPreview(theme, opacity, layout, showToday, showBreakdown, showChart)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                                    Slider(
                                        value = opacity,
                                        onValueChange = { opacity = it },
                                        valueRange = 20f..100f,
                                        steps = 15
                                    )
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Information", style = MaterialTheme.typography.titleMedium)
                                    SettingSwitch("Today's P&L", showToday) { showToday = it }
                                    SettingSwitch("Equity + mutual fund breakdown", showBreakdown) { showBreakdown = it }
                                    SettingSwitch("Performance chart (dashboard)", showChart) { showChart = it }
                                }
                            }
                        }

                        Surface(tonalElevation = 3.dp) {
                            Button(
                                onClick = {
                                    WidgetAppearance.save(
                                        this@WidgetConfigActivity,
                                        appWidgetId,
                                        theme,
                                        opacity.toInt(),
                                        layout,
                                        showToday,
                                        showBreakdown,
                                        showChart
                                    )
                                    setResult(
                                        Activity.RESULT_OK,
                                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    )
                                    PortfolioWidgetReceiver.refresh(this@WidgetConfigActivity)
                                    finish()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text("Save widget")
                            }
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
private fun WidgetPreview(
    theme: String,
    opacity: Float,
    layout: String,
    showToday: Boolean,
    showBreakdown: Boolean,
    showChart: Boolean
) {
    val background = when (theme) {
        WidgetAppearance.LIGHT -> Color(0xFFF7F7F9)
        WidgetAppearance.PITCH_BLACK -> Color.Black
        else -> Color(0xFF252329)
    }
    val content = if (theme == WidgetAppearance.LIGHT) Color(0xFF18171B) else Color.White
    val secondary = if (theme == WidgetAppearance.LIGHT) Color(0xFF504E56) else Color(0xFFCDC9D3)
    val positive = if (theme == WidgetAppearance.LIGHT) Color(0xFF187841) else Color(0xFF91EBA4)
    val alpha = opacity / 100f
    val compact = layout == WidgetAppearance.COMPACT
    val dashboard = layout == WidgetAppearance.DASHBOARD
    val showDetails = (layout == WidgetAppearance.STANDARD || dashboard || layout == WidgetAppearance.AUTO) && !compact

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Preview", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 116.dp else if (dashboard) 188.dp else 148.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(background.copy(alpha = alpha))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Zerodha Portfolio",
                        color = content,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("↻", color = positive, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "₹12,45,820",
                    color = content,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("+₹1,84,230  +17.35%", color = positive, fontWeight = FontWeight.Bold)

                if (showToday && showDetails) {
                    Text("Today", color = secondary, style = MaterialTheme.typography.labelSmall)
                    Text("+₹4,280  +0.34%", color = positive, style = MaterialTheme.typography.bodySmall)
                }

                if (showChart && dashboard) {
                    Text("╱╲╱╲╱╲╱╱", color = Color(0xFF9C8CFF), style = MaterialTheme.typography.titleMedium)
                }

                if (showBreakdown && showDetails) {
                    Text(
                        "Equity   ₹8.72L   +17.88%     Mutual Funds   ₹3.73L   +16.13%",
                        color = content,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.weight(1f, fill = false))
                Text("Updated 2 min ago", color = secondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
