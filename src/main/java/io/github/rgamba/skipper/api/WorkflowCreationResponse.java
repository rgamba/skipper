package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.models.WorkflowInstance;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class WorkflowCreationResponse {
  @NonNull WorkflowInstance workflowInstance;
}
