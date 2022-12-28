package com.maestroworkflow.models;

import com.maestroworkflow.ValidationUtils;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class OperationResponse implements Serializable {
  @NonNull String id;
  @NonNull String workflowInstanceId;
  @NonNull OperationType operationType;
  int iteration;
  @NonNull Instant creationTime;
  boolean isSuccess;
  boolean isTransient;
  @NonNull String operationRequestId;
  Anything result;
  Anything error;
  Duration executionDuration;
  String childWorkflowInstanceId;

  public OperationResponse(
      @NonNull String id,
      @NonNull String workflowInstanceId,
      @NonNull OperationType operationType,
      int iteration,
      @NonNull Instant creationTime,
      boolean isSuccess,
      boolean isTransient,
      @NonNull String operationRequestId,
      Anything result,
      Anything error,
      Duration executionDuration,
      String childWorkflowInstanceId) {
    ValidationUtils.when(!isSuccess)
        .thenExpect(error != null, "error must be set when isSuccess != true");
    ValidationUtils.when(isSuccess)
        .thenExpect(!isTransient, "successful responses must not be transient");
    ValidationUtils.when(operationType.isWorkflow())
        .thenExpect(
            childWorkflowInstanceId != null,
            "when operation type is workflow, childWorkflowInstanceId must be provided");
    this.id = id;
    this.workflowInstanceId = workflowInstanceId;
    this.operationType = operationType;
    this.iteration = iteration;
    this.creationTime = creationTime;
    this.isSuccess = isSuccess;
    this.isTransient = isTransient;
    this.operationRequestId = operationRequestId;
    this.result = result;
    this.error = error;
    this.executionDuration = executionDuration;
    this.childWorkflowInstanceId = childWorkflowInstanceId;
  }
}
