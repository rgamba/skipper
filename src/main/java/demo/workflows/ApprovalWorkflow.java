package demo.workflows;

import demo.operations.Operations;
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

public class ApprovalWorkflow implements MaestroWorkflow {
  private final Operations operations =
      OperationProxyFactory.create(
          Operations.class,
          OperationConfig.builder()
              .retryStrategy(
                  FixedRetryStrategy.builder()
                      .retryDelay(Duration.ofSeconds(2))
                      .maxRetries(3)
                      .build())
              .build());
  @StateField public Boolean isApproved = null;

  @WorkflowMethod
  public boolean getApproval(@NonNull String user, @NonNull Integer amount) {
    operations.notifyApprovalRequest(user, amount);
    try {
      waitUntil(() -> isApproved != null, Duration.ofMinutes(1));
      return isApproved;
    } catch (WaitTimeout t) {
      return false;
    }
  }

  @SignalConsumer
  public void approveTransfer(@NonNull Boolean approved) {
    isApproved = approved;
  }
}
