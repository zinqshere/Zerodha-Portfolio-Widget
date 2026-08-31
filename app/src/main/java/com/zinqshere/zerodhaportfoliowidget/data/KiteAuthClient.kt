package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import android.content.Intent
import android.net.Uri

object KiteAuthClient {
    fun openLogin(context: Context, backendUrl: String) {
        val base = backendUrl.trim().trimEnd('/')
        require(base.startsWith("https://")) { "Backend URL must use HTTPS" }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$base/api/kite/login"))
        context.startActivity(intent)
    }
}
