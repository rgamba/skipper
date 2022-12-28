package com.maestroworkflow.store;

import com.maestroworkflow.models.OperationRequest;
import com.maestroworkflow.models.OperationResponse;
import java.util.List;
import lombok.NonNull;

public interface OperationStore {
  boolean createOperationRequest(@NonNull OperationRequest operationRequest);

  boolean createOperationResponse(@NonNull OperationResponse operationResponse);

  List<OperationResponse> getOperationResponses(
      @NonNull String workflowInstanceId, boolean includeTransientResponses);

  OperationRequest getOperationRequest(@NonNull String operationRequestId);

  List<OperationRequest> getOperationRequests(@NonNull String workflowInstanceId);

  void convertAllErrorResponsesToTransient(String workflowInstanceId);
}
