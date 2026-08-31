package com.zinqshere.zerodhaportfoliowidget.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class KiteClient(private val apiKey: String, private val accessToken: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun holdings(): List<Holding> {
        val root = get("https://api.kite.trade/portfolio/holdings")
        return root["data"]?.jsonArray?.mapNotNull { item ->
            val o = item.jsonObject
            val qty = (o["quantity"]?.jsonPrimitive?.doubleOrNull ?: 0.0) +
                (o["t1_quantity"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
            val avg = o["average_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val last = o["last_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            Holding(
                symbol = o["tradingsymbol"]?.jsonPrimitive?.content ?: "",
                quantity = qty,
                averagePrice = avg,
                lastPrice = last,
                pnl = (last - avg) * qty,
                dayPnl = o["day_change"]?.jsonPrimitive?.doubleOrNull?.times(qty) ?: 0.0
            )
        } ?: emptyList()
    }

    /**
     * Coin mutual-fund holdings are available through the official Kite Connect
     * mutual-fund endpoint. This keeps Coin data automatically in sync without
     * requiring the user to export/upload a CSV or provide Coin credentials.
     */
    fun mutualFundHoldings(): List<FundHolding> {
        val root = get("https://api.kite.trade/mf/holdings")
        return root["data"]?.jsonArray?.mapNotNull { item ->
            val o = item.jsonObject
            val units = o["quantity"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val avg = o["average_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val last = o["last_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            if (units <= 0.0 || last <= 0.0) return@mapNotNull null
            val invested = units * avg
            val value = units * last
            FundHolding(
                name = o["fund"]?.jsonPrimitive?.content ?: o["tradingsymbol"]?.jsonPrimitive?.content ?: "Mutual fund",
                units = units,
                invested = invested,
                value = value,
                pnl = value - invested
            )
        } ?: emptyList()
    }

    private fun get(url: String) = run {
        val request = Request.Builder()
            .url(url)
            .header("X-Kite-Version", "3")
            .header("Authorization", "token $apiKey:$accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Empty Kite response")
            if (!response.isSuccessful) {
                val message = runCatching { json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
                error(message ?: "Kite request failed: ${response.code}")
            }
            json.parseToJsonElement(body).jsonObject
        }
    }
}
