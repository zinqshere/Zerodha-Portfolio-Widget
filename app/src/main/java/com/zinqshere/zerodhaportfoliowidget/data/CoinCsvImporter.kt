package com.zinqshere.zerodhaportfoliowidget.data

import java.io.InputStream

object CoinCsvImporter {
    fun import(input: InputStream): Pair<Double, Double> {
        val lines = input.bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "CSV is empty" }
        val header = lines.first().split(',').map { it.trim().lowercase() }
        fun index(vararg names: String) = names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } }
            ?: error("Missing column: ${names.joinToString()}")
        val investedIndex = index("invested", "invested amount", "cost", "buy value")
        val valueIndex = index("current value", "current value (₹)", "value", "market value")
        var invested = 0.0
        var value = 0.0
        lines.drop(1).forEach { line ->
            val cols = splitCsv(line)
            invested += money(cols.getOrNull(investedIndex))
            value += money(cols.getOrNull(valueIndex))
        }
        return invested to value
    }

    private fun money(value: String?): Double = value?.replace("₹", "")?.replace(",", "")?.trim()?.toDoubleOrNull() ?: 0.0

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>(); val current = StringBuilder(); var quoted = false
        line.forEach { c ->
            when { c == '"' -> quoted = !quoted; c == ',' && !quoted -> { out += current.toString(); current.clear() }; else -> current.append(c) }
        }
        out += current.toString(); return out
    }
}
