package com.maestroworkflow.client;

import com.maestroworkflow.api.WorkflowCreationRequest;
import com.maestroworkflow.api.WorkflowCreationResponse;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.NonNull;

public interface MaestroClient {
    WorkflowCreationResponse createWorkflowInstance(@NonNull WorkflowCreationRequest request);

    WorkflowInstance getWorkflowInstance(@NonNull String workflowInstanceId);
}
