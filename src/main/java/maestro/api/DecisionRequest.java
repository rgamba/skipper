package maestro.api;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import maestro.models.OperationResponse;
import maestro.models.WorkflowInstance;

@Value
@Builder(toBuilder = true)
public class DecisionRequest {
  @NonNull WorkflowInstance workflowInstance;
  @NonNull List<OperationResponse> operationResponses;
}
