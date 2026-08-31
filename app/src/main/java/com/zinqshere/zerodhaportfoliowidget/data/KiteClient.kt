package com.zinqshere.zerodhaportfoliowidget.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class KiteClient(private val apiKey: String, private val accessToken: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun holdings(): List<Holding> {
        val request = Request.Builder()
            .url("https://api.kite.trade/portfolio/holdings")
            .header("X-Kite-Version", "3")
            .header("Authorization", "token $apiKey:$accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Kite request failed: ${response.code}")
            val body = response.body?.string() ?: error("Empty Kite response")
            val root = json.parseToJsonElement(body).jsonObject
            return root["data"]?.jsonArray?.mapNotNull { item ->
                val o = item.jsonObject
                val qty = (o["quantity"]?.jsonPrimitive?.doubleOrNull ?: 0.0) +
                    (o["t1_quantity"]?.jsonPrimitive?.doubleOrNull ?: 0.0) +
                    (o["mtf"]?.jsonObject?.get("quantity")?.jsonPrimitive?.doubleOrNull ?: 0.0)
                val avg = o["average_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val last = o["last_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                Holding(o["tradingsymbol"]?.jsonPrimitive?.content ?: "", qty, avg, last,
                    (last - avg) * qty,
                    o["day_change"]?.jsonPrimitive?.doubleOrNull?.times(qty) ?: 0.0)
            } ?: emptyList()
        }
    }
}
