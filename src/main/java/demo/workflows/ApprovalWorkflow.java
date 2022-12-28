package demo.workflows;

import demo.operations.Operations;
import io.github.rgamba.skipper.OperationProxyFactory;
import io.github.rgamba.skipper.api.OperationConfig;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WaitTimeout;
import io.github.rgamba.skipper.api.annotations.SignalConsumer;
import io.github.rgamba.skipper.api.annotations.StateField;
import io.github.rgamba.skipper.api.annotations.WorkflowMethod;
import io.github.rgamba.skipper.models.FixedRetryStrategy;
import java.time.Duration;
import lombok.NonNull;

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
