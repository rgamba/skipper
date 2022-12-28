package com.maestroworkflow.api;

import com.maestroworkflow.ValidationUtils;
import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.OperationRequest;
import com.maestroworkflow.models.WorkflowInstance;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class DecisionResponse {
  Anything result;
  @NonNull List<OperationRequest> operationRequests;
  Map<String, Anything> newState;
  @NonNull WorkflowInstance.Status newStatus;
  String statusReason;
  Duration waitForDuration;

  public DecisionResponse(
      Anything result,
      @NonNull List<OperationRequest> operationRequests,
      Map<String, Anything> newState,
      @NonNull WorkflowInstance.Status newStatus,
      String statusReason,
      Duration waitForDuration) {
    ValidationUtils.when(
            newStatus.equals(WorkflowInstance.Status.COMPLETED)
                || newStatus.equals(WorkflowInstance.Status.ERROR))
        .thenExpect(
            operationRequests.isEmpty(),
            "operationRequests are not allowed when workflow instance status is %s",
            newStatus);
    ValidationUtils.when(!newStatus.equals(WorkflowInstance.Status.COMPLETED))
        .thenExpect(result == null, "result can only be set when Status == COMPLETED");
    ValidationUtils.when(newStatus.equals(WorkflowInstance.Status.ERROR))
        .thenExpect(statusReason != null, "statusReason must be provided when Status == ERROR");
    ValidationUtils.when(newStatus.equals(WorkflowInstance.Status.ACTIVE))
        .thenExpect(
            operationRequests.size() > 0 || waitForDuration != null,
            "operationRequests or waitDuration are expected when Status == ACTIVE");
    ValidationUtils.when(waitForDuration != null)
        .thenExpect(
            operationRequests.isEmpty(), "cannot have a simultaneous wait and operation request");

    this.result = result;
    this.operationRequests = operationRequests;
    this.newState = newState;
    this.newStatus = newStatus;
    this.statusReason = statusReason;
    this.waitForDuration = waitForDuration;
  }
}
