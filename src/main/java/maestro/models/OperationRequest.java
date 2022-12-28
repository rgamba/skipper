package maestro.models;

import com.google.gson.annotations.JsonAdapter;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import maestro.common.Anything;
import maestro.serde.RetryStrategyAdapter;

@Builder(toBuilder = true)
@Value
public class OperationRequest implements Serializable {
  @NonNull String operationRequestId;
  @NonNull String workflowInstanceId;
  @NonNull OperationType operationType;
  int iteration;
  @NonNull List<Anything> arguments;
  @NonNull Instant creationTime;

  @JsonAdapter(RetryStrategyAdapter.class)
  @NonNull
  RetryStrategy retryStrategy;

  @NonNull Duration timeout;
  int failedAttempts;

  public static String createOperationRequestId(OperationRequest req) {
    return String.format(
        "%s_%s_%s_%d_%d",
        req.getWorkflowInstanceId(),
        req.getOperationType().getClazz().getName(),
        req.getOperationType().getMethod(),
        req.getIteration(),
        req.getFailedAttempts());
  }
}
