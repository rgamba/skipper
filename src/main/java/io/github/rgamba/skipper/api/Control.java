package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.models.NoRetry;
import io.github.rgamba.skipper.models.OperationRequest;
import io.github.rgamba.skipper.models.OperationResponse;
import io.github.rgamba.skipper.models.OperationType;
import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.runtime.StopWorkflowExecution;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class Control {
  @SneakyThrows
  public static List<Object> join(@NonNull Promise... promises) {
    List<OperationRequest> operationRequests = new ArrayList<>();
    List<Object> result = new ArrayList<>();
    for (Promise promise : promises) {
      try {
        result.add(promise.call());
      } catch (StopWorkflowExecution e) {
        operationRequests.addAll(e.getOperationRequests());
      }
    }
    if (operationRequests.isEmpty()) {
      return result;
    }
    throw new StopWorkflowExecution(operationRequests);
  }

  @SneakyThrows
  public static void waitUntil(@NonNull Callable<Boolean> condition, Duration timeout) {
    val timeoutId =
        String.format(
            "%s-timeout-%d",
            DecisionThread.getWorkflowContext().getWorkflowInstanceId(),
            DecisionThread.getLatestCurrentExecutionCheckpoint()
                .map(Instant::getEpochSecond)
                .orElse(0L));
    val index =
        DecisionThread.getLatestCurrentExecutionCheckpoint()
            .map(Instant::getEpochSecond)
            .orElse(0L)
            .toString();
    Optional<OperationResponse> timeoutResponse =
        DecisionThread.getWorkflowContext().getOperationResponses().stream()
            .filter(resp -> resp.getOperationType().isWaitTimeout())
            .filter(resp -> resp.getOperationType().getMethod().equals(index))
            .filter(resp -> !resp.isTransient())
            .findFirst();
    if (timeoutResponse.isPresent()) {
      throw new WaitTimeout();
    }
    if (!condition.call()) {
      Timeout timer = Timeout.of(timeout);
      try {
        timer.call();
        // If we got here then it means that the timeout has completed
        val request =
            OperationRequest.builder()
                .operationRequestId(timeoutId)
                .iteration(0)
                .operationType(OperationType.waitTimeout(index))
                .workflowInstanceId(DecisionThread.getWorkflowContext().getWorkflowInstanceId())
                .creationTime(DecisionThread.getWorkflowContext().getCurrentTime())
                .timeout(Duration.ZERO)
                .retryStrategy(new NoRetry())
                .arguments(new ArrayList<>())
                .build();
        throw new StopWorkflowExecution(
            new ArrayList<OperationRequest>() {
              {
                add(request);
              }
            });
      } catch (StopWorkflowExecution e) {
        // If we got here then it means that the condition has not been met but the timer
        // still has not expired,
        // so we'll signal a wait so the engine can "sleep" for a while.
        throw e;
      }
    }
    // If we got here then it means that the condition has been met and it was met on time.
  }
}
