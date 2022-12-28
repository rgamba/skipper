package com.maestroworkflow.api;

import com.maestroworkflow.models.OperationResponse;
import com.maestroworkflow.models.WorkflowInstance;
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
