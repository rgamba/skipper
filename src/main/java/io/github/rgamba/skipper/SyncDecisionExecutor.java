package io.github.rgamba.skipper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.github.rgamba.skipper.api.DecisionRequest;
import io.github.rgamba.skipper.api.DecisionResponse;
import io.github.rgamba.skipper.api.OperationError;
import io.github.rgamba.skipper.api.OperationExecutionResponse;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.OperationRequest;
import io.github.rgamba.skipper.models.OperationResponse;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.val;
import lombok.var;

/**
 * This is a variation of the {@link DecisionExecutor} that will attempt to execute all operations
 * in-memory without having to incur in the additional overhead and I/O of writing/reading to/from
 * the timer store. This strategy is an optimization that will be significantly faster in the best
 * of cases, but will be the same as the {@link DecisionExecutor} in the worst of cases.
 *
 * <p>This strategy does not give away any of the guarantees that the {@link DecisionExecutor} does,
 * but it does increase the chances of operations being executed more than once in very rare cases.
 */
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
    List<OperationRequest> operationRequests;
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
      operationRequests = response.getOperationRequests();
      if (response.getWaitForDuration() != null
          || operationRequests.stream().anyMatch(req -> req.getOperationType().isWorkflow())) {
        // If a wait is required, or we need to execute sub-workflow, we no longer can proceed
        // synchronously
        decisionResponseBuilder.operationRequests(response.getOperationRequests());
        decisionResponseBuilder.waitForDuration(response.getWaitForDuration());
        break;
      }
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
        .operationResponses(operationResponses)
        .build();
  }

  private List<OperationResponse> executeOperations(
      @NonNull List<OperationRequest> operationRequests, @NonNull DependencyRegistry registry) {
    return operationRequests.stream()
        .map(
            req -> {
              val response = operationExecutor.execute(req, registry);
              // TODO: refactor all these code and put it in OperationExecutor to remove duplication
              // with SkipperEngine
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
