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
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val initialTheme = WidgetAppearance.theme(this, appWidgetId)
        val initialOpacity = WidgetAppearance.opacity(this, appWidgetId)

        setContent {
            var theme by remember { mutableStateOf(initialTheme) }
            var opacity by remember { mutableFloatStateOf(initialOpacity.toFloat()) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Widget appearance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personalise the portfolio widget. These settings apply only to this widget instance.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Background", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                ThemeChoice("Light", WidgetAppearance.LIGHT, theme) { theme = it }
                                ThemeChoice("Dark", WidgetAppearance.DARK, theme) { theme = it }
                                ThemeChoice("Pitch black", WidgetAppearance.PITCH_BLACK, theme) { theme = it }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Opacity", style = MaterialTheme.typography.titleMedium)
                                Text("${opacity.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                                Slider(
                                    value = opacity,
                                    onValueChange = { opacity = it },
                                    valueRange = 20f..100f,
                                    steps = 15
                                )
                            }
                        }

                        Preview(theme, opacity)

                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                WidgetAppearance.save(this@WidgetConfigActivity, appWidgetId, theme, opacity.toInt())
                                val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                setResult(Activity.RESULT_OK, result)
                                PortfolioWidgetReceiver.refresh(this@WidgetConfigActivity)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save widget")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    label: String,
    value: String,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == value, onClick = { onSelected(value) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun Preview(theme: String, opacity: Float) {
    val background = when (theme) {
        WidgetAppearance.LIGHT -> Color(0xFFF7F7F9)
        WidgetAppearance.PITCH_BLACK -> Color.Black
        else -> Color(0xFF252329)
    }
    val content = if (theme == WidgetAppearance.LIGHT) Color(0xFF18171B) else Color.White
    val alpha = opacity / 100f

    Column {
        Text("Preview", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(background.copy(alpha = alpha))
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Portfolio", color = content.copy(alpha = alpha.coerceAtLeast(0.65f)), fontWeight = FontWeight.SemiBold)
            Text("₹1,24,850", color = content, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("P&L +₹3,420  •  just now", color = content.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
