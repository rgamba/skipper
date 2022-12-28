package maestro.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import maestro.models.WorkflowInstance;

@Value
@Builder(toBuilder = true)
public class WorkflowCreationResponse {
  @NonNull WorkflowInstance workflowInstance;
}
