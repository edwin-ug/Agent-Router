package com.example.agentroutermobile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TradeViewModel : ViewModel() {

    private val _intents = MutableStateFlow<List<TradeIntent>>(emptyList())
    val intents: StateFlow<List<TradeIntent>> = _intents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentEthPrice = MutableStateFlow(0.0)
    val currentEthPrice: StateFlow<Double> = _currentEthPrice.asStateFlow()

    init {
        // The Live Heartbeat Loop
        viewModelScope.launch {
            while (true) {
                fetchPendingIntents()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun fetchPendingIntents() {
        viewModelScope.launch {
            try {
                // 1. Fetch live ETH price
                val priceResponse = RetrofitClient.apiService.getLiveEthPrice()
                _currentEthPrice.value = priceResponse["ethereum"]?.get("usd") ?: 0.0

                // 2. Fetch ALL trades from Spring Boot
                val response = RetrofitClient.apiService.getPendingIntents()

                // 🚀 FIXED: Removed the .filter block!
                // We now pass EVERYTHING to the UI, so the tabs can sort them correctly.
                _intents.value = response.reversed()

            } catch (e: Exception) {
                Log.e("TradeViewModel", "Heartbeat failed. Check server/network.", e)
            }
        }
    }

    fun approveTrade(id: String, amount: Double) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.approveIntent(id, amount)
                fetchPendingIntents()
            } catch (e: Exception) {
                Log.e("TradeViewModel", "Error approving trade", e)
            }
        }
    }

    fun rejectTrade(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.rejectIntent(id)
                fetchPendingIntents()
            } catch (e: Exception) {
                Log.e("TradeViewModel", "Error rejecting trade", e)
            }
        }
    }

    fun resetDemo() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.resetAllTrades()
                fetchPendingIntents()
            } catch (e: Exception) {
                Log.e("TradeViewModel", "Failed to reset demo", e)
            }
        }
    }
}