package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

object KiteAuthClient {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun openLogin(context: Context, backendUrl: String, accountId: String? = null) {
        val base = requireBase(backendUrl)
        val uri = Uri.parse("$base/api/kite/login").buildUpon().apply {
            if (!accountId.isNullOrBlank()) appendQueryParameter("state", accountId)
        }.build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun exchangeCode(backendUrl: String, code: String): Triple<String, String, String?> {
        val base = requireBase(backendUrl)
        val request = Request.Builder().url("$base/api/kite/exchange?code=${Uri.encode(code)}").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Kite login exchange failed: HTTP ${response.code}")
            val body = response.body?.string() ?: error("Empty login response")
            val data = json.parseToJsonElement(body).jsonObject
            val apiKey = data["api_key"]?.jsonPrimitive?.content ?: error("Missing API key")
            val accessToken = data["access_token"]?.jsonPrimitive?.content ?: error("Missing access token")
            return Triple(apiKey, accessToken, data["state"]?.jsonPrimitive?.content)
        }
    }

    private fun requireBase(url: String): String {
        val base = url.trim().trimEnd('/')
        require(base.startsWith("https://")) { "Backend URL must use HTTPS" }
        return base
    }
}
