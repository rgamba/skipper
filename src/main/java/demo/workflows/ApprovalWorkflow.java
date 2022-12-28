package demo.workflows;

import demo.operations.Operations;
import java.time.Duration;
import lombok.NonNull;
import skipper.OperationProxyFactory;
import skipper.api.OperationConfig;
import skipper.api.SkipperWorkflow;
import skipper.api.WaitTimeout;
import skipper.api.annotations.SignalConsumer;
import skipper.api.annotations.StateField;
import skipper.api.annotations.WorkflowMethod;
import skipper.models.FixedRetryStrategy;

public class ApprovalWorkflow implements SkipperWorkflow {
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
  public boolean getApproval(@NonNull String user, int amount) {
    operations.notifyApprovalRequest(user, amount);
    try {
      waitUntil(() -> isApproved != null, Duration.ofMinutes(1));
      return isApproved;
    } catch (WaitTimeout t) {
      return false;
    }
  }

  @SignalConsumer
  public void approveTransfer(boolean approved) {
    isApproved = approved;
  }
}
