package com.example.agentroutermobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TradeDashboard()
                }
            }
        }
    }
}

@Composable
fun TradeDashboard(viewModel: TradeViewModel = viewModel()) {
    val intents by viewModel.intents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentEthPrice by viewModel.currentEthPrice.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("AI Signals (Inbox)", "My Portfolio (Active)")

    // Filter logic: Only APPROVED trades move to the Portfolio tab
    val inboxTrades = intents.filter { it.riskStatus.uppercase().trim() != "APPROVED" }
    val approvedTrades = intents.filter { it.riskStatus.uppercase().trim() == "APPROVED" }

    val displayList = if (selectedTabIndex == 0) inboxTrades else approvedTrades
    // Stats calculation: Tab 0 shows AI performance, Tab 1 shows YOUR performance
    val statsList = if (selectedTabIndex == 0) intents else approvedTrades

    val totalTrades = statsList.size
    val totalProfit = statsList.sumOf { calculateProfitAmount(it.priceAtEntry, currentEthPrice, it.action, it.amount) }
    val winCount = statsList.count { calculateProfitAmount(it.priceAtEntry, currentEthPrice, it.action, it.amount) > 0 }
    val lossCount = statsList.count { calculateProfitAmount(it.priceAtEntry, currentEthPrice, it.action, it.amount) < 0 }

    val accuracy = if (totalTrades > 0) (winCount.toFloat() / totalTrades.toFloat()) * 100f else 0f
    val avgProfit = if (totalTrades > 0) totalProfit / totalTrades else 0.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AI Risk Router",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { viewModel.resetDemo() }) {
                Text("♻️", style = MaterialTheme.typography.headlineSmall)
            }

            Text(
                text = "ETH: $${String.format("%.2f", currentEthPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedTabIndex == 0) "AI System Analytics" else "My Realized Portfolio",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Win Rate", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.1f", accuracy)}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Net P&L", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${if (totalProfit >= 0) "+$" else "-$"}${String.format("%.2f", abs(totalProfit))}",
                            color = if (totalProfit >= 0) Color(0xFF2E7D32) else Color.Red,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = if (totalTrades > 0) accuracy / 100f else 0f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFFFEBEE)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("Trades", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("$totalTrades", fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Wins", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                            Text("$winCount", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Losses", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                            Text("$lossCount", fontWeight = FontWeight.SemiBold, color = Color.Red)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Avg / Trade", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "${if (avgProfit >= 0) "+$" else "-$"}${String.format("%.2f", abs(avgProfit))}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (avgProfit >= 0) Color(0xFF2E7D32) else Color.Red
                        )
                    }
                }
            }
        }

        if (isLoading && displayList.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (displayList.isEmpty()) {
            val msg = if (selectedTabIndex == 0) "Waiting for AI signals..." else "No active trades in portfolio."
            Text(msg, modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = displayList, key = { it.id }) { intent ->
                    TradeCard(intent, currentEthPrice, viewModel)
                }
            }
        }
    }
}

@Composable
fun TradeCard(intent: TradeIntent, currentPrice: Double, viewModel: TradeViewModel) {
    val context = LocalContext.current
    val pnlPercent = calculatePnL(intent.priceAtEntry, currentPrice, intent.action)
    val pnlDollar = calculateProfitAmount(intent.priceAtEntry, currentPrice, intent.action, intent.amount)

    // Safety check for HOLD action colors
    val actionColor = when(intent.action) {
        "BUY" -> Color(0xFF4CAF50)
        "SELL" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    val pnlColor = if (pnlDollar > 0) Color(0xFF4CAF50) else if (pnlDollar < 0) Color(0xFFF44336) else Color.Gray

    val isStopLossHit = if (intent.action == "BUY") {
        currentPrice <= (intent.priceAtEntry * 0.98)
    } else if (intent.action == "SELL") {
        currentPrice >= (intent.priceAtEntry * 1.02)
    } else false

    val currentStatus = intent.riskStatus.uppercase().trim()
    val isRejected = currentStatus == "REJECTED"
    val isApproved = currentStatus == "APPROVED"

    // 🚀 CRITICAL FIX: Tie the loading state to the riskStatus.
    // If the server fails to respond, it resets itself.
    var isUpdating by remember(currentStatus) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRejected) Color(0xFFEEEEEE)
            else if (isStopLossHit) Color(0xFFFFEBEE)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isStopLossHit && !isRejected) {
                Text("⚠️ STOP LOSS HIT", color = Color.Red, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = "${intent.action} ${intent.assetPair}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isRejected) Color.Gray else actionColor
                    )
                    Text(
                        text = "Live P&L: ${if (pnlPercent >= 0) "+" else ""}${String.format("%.2f", pnlPercent)}% (${if (pnlDollar >= 0) "+$" else "-$"}${String.format("%.2f", abs(pnlDollar))})",
                        color = if (isRejected) Color.Gray else pnlColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("$${intent.amount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = if (isRejected) Color.Gray else Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val slPrice = if (intent.action == "BUY") intent.priceAtEntry * 0.98 else if (intent.action == "SELL") intent.priceAtEntry * 1.02 else 0.0
            val slText = if (slPrice > 0.0) " | SL: $${String.format("%.2f", slPrice)}" else ""
            Text("Entry: $${intent.priceAtEntry}$slText", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(4.dp))
            Text("Reasoning: ${intent.reasoning}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isRejected -> {
                    Text("REJECTED", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
                }
                isApproved -> {
                    Text("LIVE POSITION", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
                }
                else -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = {
                                isUpdating = true
                                viewModel.rejectTrade(intent.id)
                            },
                            enabled = !isUpdating
                        ) {
                            Text(if (isUpdating) "..." else "REJECT", color = if (isUpdating) Color.Gray else Color.Red)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isUpdating = true
                                viewModel.approveTrade(intent.id, intent.amount)
                                Toast.makeText(context, "Approving...", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isUpdating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(if (isUpdating) "WAIT..." else "APPROVE")
                        }
                    }
                }
            }
        }
    }
}

fun calculatePnL(priceAtEntry: Double, currentPrice: Double, action: String): Double {
    // 🚀 FIXED: Ignore HOLD math to prevent random +/- values
    if (priceAtEntry == 0.0 || currentPrice == 0.0 || action.uppercase() == "HOLD") return 0.0
    return if (action == "BUY") ((currentPrice - priceAtEntry) / priceAtEntry) * 100
    else ((priceAtEntry - currentPrice) / priceAtEntry) * 100
}

fun calculateProfitAmount(pEntry: Double, pCurrent: Double, action: String, amount: Double): Double {
    // 🚀 FIXED: Ignore HOLD math
    if (pEntry == 0.0 || pCurrent == 0.0 || action.uppercase() == "HOLD") return 0.0
    val delta = if (action == "BUY") (pCurrent - pEntry) / pEntry else (pEntry - pCurrent) / pEntry
    return amount * delta
}