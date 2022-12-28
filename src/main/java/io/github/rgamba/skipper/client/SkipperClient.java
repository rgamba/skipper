package io.github.rgamba.skipper.client;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.api.CallbackHandler;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WorkflowCreationRequest;
import io.github.rgamba.skipper.api.WorkflowCreationResponse;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.WorkflowInstance;
import io.github.rgamba.skipper.models.WorkflowType;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.val;

public class SkipperClient {
  private final SkipperEngine engine;

  public SkipperClient(@NonNull SkipperEngine engine) {
    this.engine = engine;
  }

  public WorkflowCreationResponse createWorkflowInstance(
      @NonNull Class<? extends SkipperWorkflow> workflow,
      @NonNull String correlationId,
      Object... initialArgs) {
    return createWorkflowInstance(workflow, correlationId, null, initialArgs);
  }

  public WorkflowCreationResponse createWorkflowInstance(
      @NonNull Class<? extends SkipperWorkflow> workflow,
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
