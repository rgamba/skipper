package skipper.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import skipper.common.Anything;
import skipper.models.OperationRequest;

/**
 * Serves as a signal to the decision runner that the workflow decision execution cannot proceed any
 * further because some required operation responses are not present. This is a way to get control
 * back to the decision execution by "yielding" or "returning early".
 *
 * <p>There is a strong assumption that this exception type must not be caught anywhere in the
 * workflow decision context.
 */
@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class StopWorkflowExecution extends Error {
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
