package com.maestroworkflow.store;

import com.maestroworkflow.models.OperationRequest;
import com.maestroworkflow.models.OperationResponse;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.NonNull;

import java.util.List;

public interface OperationStore {
    boolean createOperationRequest(@NonNull OperationRequest operationRequest);
    boolean createOperationResponse(@NonNull OperationResponse operationResponse);
    List<OperationResponse> getOperationResponses(@NonNull String workflowInstanceId, boolean includeTransientResponses);
    OperationRequest getOperationRequest(@NonNull String operationRequestId);

    void convertAllErrorResponsesToTransient(String workflowInstanceId);
}
