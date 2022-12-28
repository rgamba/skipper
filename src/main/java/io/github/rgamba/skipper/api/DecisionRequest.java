package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.models.OperationResponse;
import io.github.rgamba.skipper.models.WorkflowInstance;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class DecisionRequest {
  @NonNull WorkflowInstance workflowInstance;
  @NonNull List<OperationResponse> operationResponses;
}
