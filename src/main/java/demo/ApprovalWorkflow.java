package demo;

import java.time.Duration;
import maestro.OperationProxyFactory;
import maestro.api.MaestroWorkflow;
import maestro.api.OperationConfig;
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

  @WorkflowMethod
  public String startApproval(String user) {
    operations.debit(user, 1);
    return "approval granted for " + user;
  }
}
