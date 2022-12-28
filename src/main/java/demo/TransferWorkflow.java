package demo;

import com.maestroworkflow.OperationProxyFactory;
import com.maestroworkflow.api.*;
import com.maestroworkflow.api.annotations.SignalConsumer;
import com.maestroworkflow.api.annotations.StateField;
import com.maestroworkflow.api.annotations.WorkflowMethod;
import com.maestroworkflow.models.FixedRetryStrategy;
import lombok.NonNull;
import lombok.val;

import java.time.Duration;

public class TransferWorkflow implements MaestroWorkflow {
    private final Operations operations = OperationProxyFactory.create(Operations.class,
            OperationConfig.builder()
                    .retryStrategy(FixedRetryStrategy.builder()
                            .retryDelay(Duration.ofSeconds(2))
                            .maxRetries(2)
                            .build())
                    .build());
    public final ApprovalWorkflow approvalWorkflow = OperationProxyFactory.create(ApprovalWorkflow.class);

    @StateField
    public Boolean isApproved = false;
    @StateField
    public Integer counterValue = 1;

    @WorkflowMethod
    public String transfer(String from, String to, Integer amount) {
        counterValue = 1;
        if (this.operations.debit(from, amount)) {
            if (this.operations.credit(to, amount)) {
                try {
                    counterValue = 2;
                    waitUntil(() -> isApproved, Duration.ofSeconds(30));
                    counterValue = 3;
                    this.approvalWorkflow.startApproval(to);
                    String approvalReason = this.approvalWorkflow.startApproval(to);
                    return "transfer completed with approval signature: " + approvalReason;
                } catch (WaitTimeout ignored) {
                }
            }
        }
        return "transfer did not complete";
    }

    @SignalConsumer
    public void approveTransfer(@NonNull Boolean approved) {
        isApproved = approved;
    }
}
