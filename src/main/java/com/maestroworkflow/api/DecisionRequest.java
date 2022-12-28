package com.maestroworkflow.api;

import com.maestroworkflow.models.OperationResponse;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class DecisionRequest {
    @NonNull WorkflowInstance workflowInstance;
    @NonNull List<OperationResponse> operationResponses;
}
