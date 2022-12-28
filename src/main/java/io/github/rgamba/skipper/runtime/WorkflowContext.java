package io.github.rgamba.skipper.runtime;

import io.github.rgamba.skipper.models.OperationResponse;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class WorkflowContext {
  @NonNull String workflowInstanceId;
  @NonNull Instant currentTime;
  @NonNull List<OperationResponse> operationResponses;
  @NonNull Instant workflowInstanceCreationTime;
}
