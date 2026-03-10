package com.example.agentroutermobile

data class TradeIntent(
    val id: String,
    val assetPair: String,
    val action: String,
    val amount: Double,
    val reasoning: String,
    val priceAtEntry: Double,
    val riskStatus: String = "PENDING" // 🚀 CRITICAL for the tabs to work!
)
