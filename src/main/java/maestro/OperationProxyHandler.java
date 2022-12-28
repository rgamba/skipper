package maestro;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.util.proxy.MethodHandler;
import lombok.NonNull;
import lombok.val;
import maestro.api.MaestroWorkflow;
import maestro.api.OperationConfig;
import maestro.api.StopWorkflowExecution;
import maestro.api.WorkflowContext;
import maestro.common.Anything;
import maestro.models.OperationRequest;
import maestro.models.OperationResponse;
import maestro.models.OperationType;

public class OperationProxyHandler implements MethodHandler {
  private final List<OperationResponse> responses;
  private final Map<String, AtomicInteger> iteration = new HashMap<>();
  private final WorkflowContext context;
  private final Class<?> operationClazz;
  private final OperationConfig operationConfig;
  private final boolean isWorkflow;

  public OperationProxyHandler(
      @NonNull List<OperationResponse> responses,
      @NonNull WorkflowContext context,
      @NonNull Class<?> operationClazz,
      OperationConfig operationConfig,
      boolean isWorkflow) {
    this.responses =
        responses.stream()
            .filter(o -> !o.isTransient())
            .filter(resp -> resp.getOperationType().getClazz().equals(operationClazz))
            .collect(Collectors.toList());
    this.context = context;
    this.operationClazz = operationClazz;
    this.operationConfig =
        operationConfig != null ? operationConfig : OperationExecutor.DEFAULT_OPERATION_CONFIG;
    this.isWorkflow = isWorkflow;
  }

  @Override
  public Object invoke(Object proxy, Method method, Method method1, Object[] args)
      throws Throwable {
    if (!iteration.containsKey(method.getName())) {
      iteration.put(method.getName(), new AtomicInteger(0));
    }
    val response =
        responses.stream()
            .filter(resp -> resp.getOperationType().getMethod().equals(method.getName()))
            .filter(resp -> resp.getIteration() == iteration.get(method.getName()).get())
            .findFirst();
    if (response.isPresent()) {
      iteration.get(method.getName()).incrementAndGet();
      WorkflowContext.setLatestCurrentExecutionCheckpoint(response.get().getCreationTime());
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
                  if (MaestroWorkflow.IDEMPOTENCY_TOKEN_PLACEHOLDER.equals(arg)) {
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
