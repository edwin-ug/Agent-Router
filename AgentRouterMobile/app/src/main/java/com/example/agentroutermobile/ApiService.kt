package com.example.agentroutermobile

import retrofit2.http.*

interface ApiService {

    // 1. Fetch live ETH price from CoinGecko
    // URL: https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd
    @GET("https://api.coingecko.com/api/v3/simple/price")
    suspend fun getLiveEthPrice(
        @Query("ids") ids: String = "ethereum",
        @Query("vs_currencies") vsCurrencies: String = "usd"
    ): Map<String, Map<String, Double>>

    // 2. Fetch all trade intents from your Spring Boot Backend
    @GET("api/v1/intents")
    suspend fun getPendingIntents(): List<TradeIntent>

    // 3. Approve a trade intent
    // This updates the status to "APPROVED" in the database
    @POST("api/v1/intents/{id}/approve")
    suspend fun approveIntent(
        @Path("id") id: String,
        @Query("overrideAmount") amount: Double
    ): TradeIntent

    // 4. Reject a trade intent
    // This updates the status to "REJECTED" in the database
    @POST("api/v1/intents/{id}/reject")
    suspend fun rejectIntent(
        @Path("id") id: String
    ): TradeIntent

    // 5. Reset all trades (for demo purposes)
    @DELETE("api/v1/intents/reset")
    suspend fun resetAllTrades(): Unit
}