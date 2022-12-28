package com.maestroworkflow.api;

import com.maestroworkflow.models.WorkflowInstance;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class WorkflowCreationResponse {
  @NonNull WorkflowInstance workflowInstance;
}
