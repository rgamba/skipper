package io.github.rgamba.skipper.models;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.gson.annotations.JsonAdapter;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.serde.RetryStrategyAdapter;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

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
    String token =
        String.format(
            "%s_%s_%s_%d_%d",
            req.getWorkflowInstanceId(),
            req.getOperationType().getClazz().getName(),
            req.getOperationType().getMethod(),
            req.getIteration(),
            req.getFailedAttempts());
    return Hashing.sha256().hashString(token, Charsets.UTF_8).toString();
  }

  /**
   * Idempotency token is similar to the request ID, with the difference being that all execution
   * attempts for the same request will share the same idempotency key so that it can be used by the
   * operation handlers to dedup operation requests.
   *
   * @return The idempotency token
   */
  public String generateIdempotencyToken() {
    String token =
        String.format(
            "%s_%s_%s_%d",
            workflowInstanceId,
            operationType.getClazz().getName(),
            operationType.getMethod(),
            iteration);
    return Hashing.sha256().hashString(token, Charsets.UTF_8).toString();
  }
}
