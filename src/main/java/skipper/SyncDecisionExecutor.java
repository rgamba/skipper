package skipper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import skipper.api.DecisionRequest;
import skipper.api.DecisionResponse;
import skipper.api.OperationError;
import skipper.api.OperationExecutionResponse;
import skipper.common.Anything;
import skipper.models.OperationRequest;
import skipper.models.OperationResponse;

public class SyncDecisionExecutor extends DecisionExecutor {
  @NonNull private final OperationExecutor operationExecutor;
  @NonNull private final Clock clock;

  @Inject
  public SyncDecisionExecutor(
      @NonNull OperationExecutor operationExecutor, @Named("UTC") @NonNull Clock clock) {
    this.operationExecutor = operationExecutor;
    this.clock = clock;
  }

  @Override
  public DecisionResponse execute(
      @NonNull DecisionRequest decisionRequest, @NonNull DependencyRegistry registry) {
    List<OperationRequest> operationRequests = new ArrayList<>();
    // We'll keep track in memory of all operation responses we've received so far so that we can
    // return them as part of the decision response.
    List<OperationResponse> operationResponses = decisionRequest.getOperationResponses();
    val decisionResponseBuilder = DecisionResponse.builder();
    do {
      DecisionResponse response = super.execute(decisionRequest, registry);
      decisionResponseBuilder.newStatus(response.getNewStatus());
      decisionResponseBuilder.newState(response.getNewState());
      decisionResponseBuilder.statusReason(response.getStatusReason());
      decisionResponseBuilder.result(response.getResult());
      if (response.getWaitForDuration() != null) {
        // If a wait is required, we no longer can proceed synchronously
        decisionResponseBuilder.operationRequests(response.getOperationRequests());
        break;
      }
      operationRequests = response.getOperationRequests();
      for (val opResponse : executeOperations(operationRequests, registry)) {
        if (opResponse.isTransient()) {
          // Non-transient operation results cannot be processed synchronously,
          // so we need to break in order to retry async.
          break;
        }
        operationResponses.add(opResponse);
      }
      ;
    } while (!operationRequests.isEmpty());
    return decisionResponseBuilder
            .operationRequests(operationRequests)
            .operationResponses(operationResponses).build();
  }

  private List<OperationResponse> executeOperations(
      @NonNull List<OperationRequest> operationRequests, @NonNull DependencyRegistry registry) {
    return operationRequests.stream()
        .map(
            req -> {
              val response = operationExecutor.execute(req, registry);
              //TODO: refactor all these code and put it in OperationExecutor to remove duplication with SkipperEngine
              var responseError = response.getError();
              boolean isTransient = false;
              if (response.getStatus().equals(OperationExecutionResponse.Status.RETRIABLE_ERROR)) {
                if (req.getRetryStrategy()
                    .getNextRetryDelay(req.getFailedAttempts() + 1)
                    .isPresent()) {
                  isTransient = true;
                } else {
                  responseError =
                      new Anything(
                          OperationError.class,
                          new OperationError((Throwable) responseError.getValue()));
                }
              }
              return OperationResponse.builder()
                  .operationRequestId(req.getOperationRequestId())
                  .result(response.getResult())
                  .isSuccess(!response.isError())
                  .operationType(req.getOperationType())
                  .iteration(req.getIteration())
                  .id(UUID.randomUUID().toString())
                  .workflowInstanceId(req.getWorkflowInstanceId())
                  .creationTime(clock.instant())
                  .isTransient(isTransient)
                  .error(responseError)
                  .executionDuration(response.getExecutionDuration())
                  .build();
            })
        .collect(Collectors.toList());
  }
}
