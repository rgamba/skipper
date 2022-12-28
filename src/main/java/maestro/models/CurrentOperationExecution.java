package maestro.models;

import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class CurrentOperationExecution {
  @NonNull String workflowInstanceId;
  @NonNull String operationRequestId;
  @NonNull Instant executionStartTime;
  @NonNull OperationType operationType;
  int iteration;
}
