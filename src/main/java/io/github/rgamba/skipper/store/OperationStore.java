package io.github.rgamba.skipper.store;

import io.github.rgamba.skipper.models.OperationRequest;
import io.github.rgamba.skipper.models.OperationResponse;
import java.util.List;
import lombok.NonNull;

public interface OperationStore {
  boolean createOperationRequest(@NonNull OperationRequest operationRequest);

  void incrementOperationRequestFailedAttempts(
      @NonNull String operationRequestId, int currentRetries);

  boolean createOperationResponse(@NonNull OperationResponse operationResponse);

  List<OperationResponse> getOperationResponses(
      @NonNull String workflowInstanceId, boolean includeTransientResponses);

  OperationRequest getOperationRequest(@NonNull String operationRequestId);

  List<OperationRequest> getOperationRequests(@NonNull String workflowInstanceId);

  void convertAllErrorResponsesToTransient(String workflowInstanceId);
}
