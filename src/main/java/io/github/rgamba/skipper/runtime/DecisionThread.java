package io.github.rgamba.skipper.runtime;

import io.github.rgamba.skipper.common.ValidationUtils;
import io.github.rgamba.skipper.models.OperationResponse;
import io.github.rgamba.skipper.models.WorkflowInstance;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

/**
 * Holds state relevant to the workflow decision flow. The data provided by this component must only
 * be used in the context of a workflow decision.
 */
public class DecisionThread {
  private static final ThreadLocal<WorkflowContext> workflowContext = new ThreadLocal<>();
  // This holds the latest time checkpoint for the current workflow decision run. When we execute a
  // workflow, we replay all workflow operations from the beginning and every time we hit an
  // operation for
  // which we have a recorded result, we consider that operation's response time to be the latest
  // execution checkpoint.
  private static final ThreadLocal<Instant> latestCurrentExecutionCheckpoint = new ThreadLocal<>();

  public static void setWorkflowContext(@NonNull WorkflowContext context) {
    workflowContext.set(context);
  }

  public static void clear() {
    workflowContext.remove();
    latestCurrentExecutionCheckpoint.remove();
  }

  public static WorkflowContext getWorkflowContext() {
    if (workflowContext.get() == null) {
      throw new IllegalStateException("workflowContext is null");
    }
    return workflowContext.get();
  }

  public static Optional<Instant> getLatestCurrentExecutionCheckpoint() {
    return Optional.ofNullable(latestCurrentExecutionCheckpoint.get());
  }

  public static void setLatestCurrentExecutionCheckpoint(@NonNull Instant checkpoint) {
    latestCurrentExecutionCheckpoint.set(checkpoint);
  }

  public static void removeLatestCurrentExecutionCheckpoint() {
    latestCurrentExecutionCheckpoint.remove();
  }

  public static Setter contextSetter() {
    return new Setter();
  }

  public static class Setter {
    WorkflowInstance workflowInstance;
    Instant currentTime;
    List<OperationResponse> operationResponses;

    public Setter workflowInstance(@NonNull WorkflowInstance instance) {
      this.workflowInstance = instance;
      return this;
    }

    public Setter currentTime(@NonNull Instant currentTime) {
      this.currentTime = currentTime;
      return this;
    }

    public Setter operationResponses(@NonNull List<OperationResponse> operationResponses) {
      this.operationResponses = operationResponses;
      return this;
    }

    public void set() {
      ValidationUtils.require(workflowInstance != null)
          .orFail("workflow instance must be provided");
      ValidationUtils.require(currentTime != null).orFail("currentTime must be provided");
      ValidationUtils.require(operationResponses != null)
          .orFail("operationResponses must be provided");
      DecisionThread.setWorkflowContext(
          new WorkflowContext(
              workflowInstance.getId(),
              currentTime,
              operationResponses,
              workflowInstance.getCreationTime()));
      DecisionThread.removeLatestCurrentExecutionCheckpoint();
    }
  }
}
