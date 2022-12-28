package maestro.client;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.val;
import maestro.MaestroEngine;
import maestro.api.CallbackHandler;
import maestro.api.MaestroWorkflow;
import maestro.api.WorkflowCreationRequest;
import maestro.api.WorkflowCreationResponse;
import maestro.common.Anything;
import maestro.models.WorkflowInstance;
import maestro.models.WorkflowType;

public class MaestroClient {
  private final MaestroEngine engine;

  public MaestroClient(@NonNull MaestroEngine engine) {
    this.engine = engine;
  }

  public WorkflowCreationResponse createWorkflowInstance(
      @NonNull Class<? extends MaestroWorkflow> workflow,
      @NonNull String correlationId,
      Object... initialArgs) {
    return createWorkflowInstance(workflow, correlationId, null, initialArgs);
  }

  public WorkflowCreationResponse createWorkflowInstance(
      @NonNull Class<? extends MaestroWorkflow> workflow,
      @NonNull String correlationId,
      Class<? extends CallbackHandler> callbackHandler,
      Object... initialArgs) {
    val argsList =
        Stream.of(initialArgs)
            .map(arg -> new Anything(arg.getClass(), arg))
            .collect(Collectors.toList());
    WorkflowCreationRequest req =
        WorkflowCreationRequest.builder()
            .arguments(argsList)
            .correlationId(correlationId)
            .workflowType(new WorkflowType(workflow))
            .callbackHandlerClazz(callbackHandler)
            .build();
    return engine.createWorkflowInstance(req);
  }

  public WorkflowInstance getWorkflowInstance(@NonNull String workflowInstanceId) {
    return engine.getWorkflowInstance(workflowInstanceId);
  }

  public void sendInputSignal(
      @NonNull String workflowInstanceId, @NonNull String methodName, Object... args) {
    val argsList =
        Stream.of(args).map(arg -> new Anything(arg.getClass(), arg)).collect(Collectors.toList());
    engine.executeSignalConsumer(workflowInstanceId, methodName, argsList);
  }
}
