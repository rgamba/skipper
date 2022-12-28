package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.common.ValidationUtils;
import java.time.Duration;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class OperationExecutionResponse {
  @NonNull Status status;
  Anything result;
  Anything error;
  Duration executionDuration;

  public OperationExecutionResponse(
      @NonNull Status status, Anything result, Anything error, Duration executionDuration) {
    ValidationUtils.when(status == Status.RETRIABLE_ERROR || status == Status.NON_RETRIABLE_ERROR)
        .thenExpect(error != null, "error must be provided when status is error");
    this.status = status;
    this.result = result;
    this.error = error;
    this.executionDuration = executionDuration;
  }

  public boolean isError() {
    return status == Status.RETRIABLE_ERROR || status == Status.NON_RETRIABLE_ERROR;
  }

  public enum Status {
    COMPLETED,
    PENDING,
    RETRIABLE_ERROR,
    NON_RETRIABLE_ERROR;
  }
}
