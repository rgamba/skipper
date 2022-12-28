package skipper.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import skipper.models.WorkflowInstance;

@Value
@Builder(toBuilder = true)
public class WorkflowCreationResponse {
  @NonNull WorkflowInstance workflowInstance;
}
