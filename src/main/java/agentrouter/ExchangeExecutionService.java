package agentrouter;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.UUID;

@Service
public class ExchangeExecutionService {

    @Async
    public void executeOrder(TradeIntent intent) {
        System.out.println("⏳ Initiating secure connection to Crypto Exchange...");

        try {
            // Simulate the network delay of hitting a real exchange API
            Thread.sleep(1500);

            // Generate a fake "Blockchain/Exchange" Transaction ID
            String mockTxHash = "0x" + UUID.randomUUID().toString().replace("-", "");

            System.out.println(" =========================================");
            System.out.println(" TRADE EXECUTED SUCCESSFULLY ON EXCHANGE");
            System.out.println(" Asset: " + intent.getAssetPair());
            System.out.println(" Action: " + intent.getAction());
            System.out.println(" Amount: $" + intent.getAmount());
            System.out.println(" Receipt Hash: " + mockTxHash);
            System.out.println(" =========================================");

        } catch (InterruptedException e) {
            System.err.println(" Execution interrupted!");
        }
    }
}