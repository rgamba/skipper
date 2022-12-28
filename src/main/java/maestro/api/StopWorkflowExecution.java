package maestro.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import maestro.common.Anything;
import maestro.models.OperationRequest;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class StopWorkflowExecution extends RuntimeException {
  @NonNull List<OperationRequest> operationRequests;
  Duration waitForDuration;
  Map<String, Anything> newState;

  public StopWorkflowExecution(
      @NonNull List<OperationRequest> operationRequests, Map<String, Anything> newState) {
    this.operationRequests = operationRequests;
    waitForDuration = null;
    this.newState = newState;
  }

  public StopWorkflowExecution(@NonNull List<OperationRequest> operationRequests) {
    this.operationRequests = operationRequests;
    waitForDuration = null;
    newState = null;
  }

  public StopWorkflowExecution(
      @NonNull List<OperationRequest> operationRequests, @NonNull Duration waitForDuration) {
    this.operationRequests = operationRequests;
    this.waitForDuration = waitForDuration;
    this.newState = null;
  }

  public StopWorkflowExecution(@NonNull Duration waitForDuration) {
    this.waitForDuration = waitForDuration;
    this.operationRequests = new ArrayList<>();
    this.newState = null;
  }
}
