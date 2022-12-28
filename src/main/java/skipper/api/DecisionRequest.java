package skipper.api;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import skipper.models.OperationResponse;
import skipper.models.WorkflowInstance;

@Value
@Builder(toBuilder = true)
public class DecisionRequest {
  @NonNull WorkflowInstance workflowInstance;
  @NonNull List<OperationResponse> operationResponses;
}
