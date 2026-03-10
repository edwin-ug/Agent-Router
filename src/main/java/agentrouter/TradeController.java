package agentrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/intents")
@CrossOrigin(origins = "*")
public class TradeController {

    private final RiskManagementService riskManagementService;
    private final TradeIntentRepository tradeIntentRepository;
    private final ExchangeExecutionService executionService;

    @Autowired
    public TradeController(
            RiskManagementService riskManagementService,
            TradeIntentRepository tradeIntentRepository,
            ExchangeExecutionService executionService) {
        this.riskManagementService = riskManagementService;
        this.tradeIntentRepository = tradeIntentRepository;
        this.executionService = executionService;
    }

    @PostMapping("/submit")
    public ResponseEntity<TradeIntent> submitIntent(@RequestBody TradeIntent intent) {
        // Safety check for entry price
        if (intent.getPriceAtEntry() == null) {
            intent.setPriceAtEntry(BigDecimal.ZERO);
        }
        System.out.println("AI Proposal: " + intent.getAction() + " " + intent.getAssetPair() + " @ $"
                + intent.getPriceAtEntry());

        TradeIntent processedIntent = riskManagementService.evaluateAndSaveIntent(intent);
        return ResponseEntity.ok(processedIntent);
    }

    @GetMapping
    public ResponseEntity<List<TradeIntent>> getAllIntents() {
        List<TradeIntent> allTrades = tradeIntentRepository.findAll();
        return ResponseEntity.ok(allTrades);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TradeIntent> approveIntent(
            @PathVariable("id") UUID id,
            @RequestParam("overrideAmount") BigDecimal overrideAmount) {

        System.out.println("🗳️ Human Approval Received for ID: " + id + " with Amount: $" + overrideAmount);

        // 1. Update the record in DB (This must return the COMPLETE object)
        TradeIntent approvedIntent = riskManagementService.approveOverride(id, overrideAmount);

        // 2. Execute the order using the freshly updated data
        try {
            executionService.executeOrder(approvedIntent);
        } catch (Exception e) {
            System.err.println("Execution Failed: " + e.getMessage());
            approvedIntent.setRiskStatus("EXECUTION_FAILED");
            tradeIntentRepository.save(approvedIntent);
        }

        return ResponseEntity.ok(approvedIntent);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TradeIntent> rejectIntent(@PathVariable("id") UUID id) {
        TradeIntent intent = tradeIntentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trade Intent not found: " + id));

        intent.setRiskStatus("REJECTED");
        TradeIntent rejectedIntent = tradeIntentRepository.save(intent);
        System.out.println("Trade Rejected by Human: " + id);

        return ResponseEntity.ok(rejectedIntent);
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Void> resetDemo() {
        tradeIntentRepository.deleteAll();
        System.out.println("Demo Reset: All trades cleared from database.");
        return ResponseEntity.ok().build();
    }
}