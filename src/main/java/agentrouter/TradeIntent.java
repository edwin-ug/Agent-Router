package agentrouter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_intent")
public class TradeIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_pair", nullable = false)
    private String assetPair;

    @Column(nullable = false)
    private String action; // BUY or SELL

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "risk_status", nullable = false)
    private String riskStatus; // PENDING, BLOCKED_LIMIT_EXCEEDED, APPROVED, REJECTED

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private BigDecimal priceAtEntry;

    // Stop Loss to catch the AI's risk management data
    private BigDecimal stopLoss;

    private BigDecimal currentPerformance; // Percentage gain or loss

    public TradeIntent() {
        this.timestamp = LocalDateTime.now();
        this.riskStatus = "PENDING";
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAssetPair() {
        return assetPair;
    }

    public void setAssetPair(String assetPair) {
        this.assetPair = assetPair;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getRiskStatus() {
        return riskStatus;
    }

    public void setRiskStatus(String riskStatus) {
        this.riskStatus = riskStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getPriceAtEntry() {
        return priceAtEntry;
    }

    public void setPriceAtEntry(BigDecimal priceAtEntry) {
        this.priceAtEntry = priceAtEntry;
    }

    // Getters and Setters for Stop Loss
    public BigDecimal getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(BigDecimal stopLoss) {
        this.stopLoss = stopLoss;
    }

    public BigDecimal getCurrentPerformance() {
        return currentPerformance;
    }

    public void setCurrentPerformance(BigDecimal currentPerformance) {
        this.currentPerformance = currentPerformance;
    }
}