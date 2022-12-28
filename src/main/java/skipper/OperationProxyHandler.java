package skipper;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.util.proxy.MethodHandler;
import lombok.NonNull;
import lombok.val;
import skipper.api.OperationConfig;
import skipper.api.SkipperWorkflow;
import skipper.common.Anything;
import skipper.models.OperationRequest;
import skipper.models.OperationType;
import skipper.runtime.DecisionThread;
import skipper.runtime.StopWorkflowExecution;

/**
 * Handler for the proxy operation object that will be used for workflow decisions at runtime.
 *
 * <p>THis is a handler for a single operation in a single run of a workflow decision. This class is
 * NOT thread-safe. The underlying workflow decider class should be instantiated as a new object
 * every time it runs, so that this object doesn't get reused for multiple workflow decision runs.
 */
public class OperationProxyHandler implements MethodHandler {
  // This counter assumes only a single run! If this handler is used for multiple runs for
  // potentially multiple
  // workflow instances, the counts will not be wiped out, and you will get wrong results!
  private final Map<String, AtomicInteger> iteration = new HashMap<>();
  private final Class<?> operationClazz;
  private final OperationConfig operationConfig;
  private final boolean isWorkflow;

  public OperationProxyHandler(
      @NonNull Class<?> operationClazz, OperationConfig operationConfig, boolean isWorkflow) {
    this.operationClazz = operationClazz;
    this.operationConfig =
        operationConfig != null ? operationConfig : OperationExecutor.DEFAULT_OPERATION_CONFIG;
    this.isWorkflow = isWorkflow;
  }

  @Override
  public Object invoke(Object proxy, Method method, Method method1, Object[] args)
      throws Throwable {
    val context = DecisionThread.getWorkflowContext();
    val responses = context.getOperationResponses();
    if (!iteration.containsKey(method.getName())) {
      iteration.put(method.getName(), new AtomicInteger(0));
    }
    val response =
        responses.stream()
            .filter(resp -> resp.getOperationType().getClazz().equals(operationClazz))
            .filter(resp -> resp.getOperationType().getMethod().equals(method.getName()))
            .filter(resp -> resp.getIteration() == iteration.get(method.getName()).get())
            .filter(resp -> !resp.isTransient())
            .findFirst();
    if (response.isPresent()) {
      iteration.get(method.getName()).incrementAndGet();
      DecisionThread.setLatestCurrentExecutionCheckpoint(response.get().getCreationTime());
      if (!response.get().isSuccess()) {
        // If it is not success then we'll raise an exception
        val error = response.get().getError();
        throw (Throwable) error.getClazz().cast(error.getValue());
      }
      if (response.get().getResult() == null) {
        return null;
      }
      return response.get().getResult().getValue();
    }

    val operationRequest =
        OperationRequest.builder()
            .operationRequestId("")
            .iteration(iteration.get(method.getName()).get())
            .operationType(
                new OperationType(
                    operationClazz,
                    method.getName(),
                    isWorkflow
                        ? OperationType.ClazzType.WORKFLOW
                        : OperationType.ClazzType.OPERATION))
            .workflowInstanceId(context.getWorkflowInstanceId())
            .creationTime(context.getCurrentTime())
            .retryStrategy(
                operationConfig.getRetryStrategy() != null
                    ? operationConfig.getRetryStrategy()
                    : OperationExecutor.DEFAULT_RETRY_STRATEGY)
            .timeout(Duration.ZERO)
            .arguments(new ArrayList<>())
            .build();
    val operationRequestId = OperationRequest.createOperationRequestId(operationRequest);
    val argsList =
        Stream.of(args)
            .map(
                arg -> {
                  if (arg == null) {
                    return null;
                  }
                  if (SkipperWorkflow.IDEMPOTENCY_TOKEN_PLACEHOLDER.equals(arg)) {
                    return Anything.of(operationRequest.generateIdempotencyToken());
                  }
                  return Anything.of(arg);
                })
            .collect(Collectors.toList());
    throw new StopWorkflowExecution(
        new ArrayList<OperationRequest>() {
          {
            add(
                operationRequest
                    .toBuilder()
                    .operationRequestId(operationRequestId)
                    .arguments(argsList)
                    .build());
          }
        });
  }
}
