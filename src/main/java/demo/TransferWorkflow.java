package demo;

import java.time.Duration;
import lombok.NonNull;
import maestro.OperationProxyFactory;
import maestro.api.MaestroWorkflow;
import maestro.api.OperationConfig;
import maestro.api.WaitTimeout;
import maestro.api.annotations.SignalConsumer;
import maestro.api.annotations.StateField;
import maestro.api.annotations.WorkflowMethod;
import maestro.models.FixedRetryStrategy;

public class TransferWorkflow implements MaestroWorkflow {
  private final Operations operations =
      OperationProxyFactory.create(
          Operations.class,
          OperationConfig.builder()
              .retryStrategy(
                  FixedRetryStrategy.builder()
                      .retryDelay(Duration.ofSeconds(2))
                      .maxRetries(2)
                      .build())
              .build());
  public final ApprovalWorkflow approvalWorkflow =
      OperationProxyFactory.create(ApprovalWorkflow.class);

  @StateField public Boolean isApproved = false;
  @StateField public Integer counterValue = 1;

  @WorkflowMethod
  public String transfer(String from, String to, Integer amount) {
    counterValue = 1;
    if (this.operations.debit(from, amount)) {
      if (this.operations.credit(to, amount)) {
        try {
          counterValue = 2;
          waitUntil(() -> isApproved, Duration.ofSeconds(6));
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
