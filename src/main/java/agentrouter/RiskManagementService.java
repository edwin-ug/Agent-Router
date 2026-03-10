package agentrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RiskManagementService {

    private final TradeIntentRepository tradeIntentRepository;

    private static final BigDecimal MAX_POSITION_SIZE = new BigDecimal("500.00");

    @Autowired
    public RiskManagementService(TradeIntentRepository tradeIntentRepository) {
        this.tradeIntentRepository = tradeIntentRepository;
    }

    public TradeIntent evaluateAndSaveIntent(TradeIntent incomingIntent) {
        
        if (incomingIntent.getAmount().compareTo(MAX_POSITION_SIZE) > 0) {
            incomingIntent.setRiskStatus("BLOCKED_LIMIT_EXCEEDED");
        } else {
            incomingIntent.setRiskStatus("PENDING");
        }

        return tradeIntentRepository.save(incomingIntent);
    }

    public TradeIntent approveOverride(UUID intentId, BigDecimal overrideAmount) {
        TradeIntent intent = tradeIntentRepository.findById(intentId)
                .orElseThrow(() -> new RuntimeException("Trade Intent not found"));

        intent.setAmount(overrideAmount);
        intent.setRiskStatus("APPROVED");
        
        
        return tradeIntentRepository.save(intent);
    }
}