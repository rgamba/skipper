package maestro.api;

import java.time.Duration;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import maestro.ValidationUtils;
import maestro.common.Anything;

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
