package io.github.rgamba.skipper.client;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.api.CallbackHandler;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WorkflowCreationRequest;
import io.github.rgamba.skipper.api.WorkflowCreationResponse;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.WorkflowType;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.util.proxy.MethodHandler;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

public class WorkflowProxyHandler implements MethodHandler {
  private final SkipperEngine engine;
  private final Class<? extends SkipperWorkflow> clazz;
  private final String correlationId;
  private final Class<? extends CallbackHandler> callbackHandler;
  @Getter private WorkflowCreationResponse workflowCreationResponse;

  public WorkflowProxyHandler(
      @NonNull SkipperEngine engine,
      @NonNull Class<? extends SkipperWorkflow> clazz,
      @NonNull String correlationId,
      Class<? extends CallbackHandler> callbackHandler) {
    this.engine = engine;
    this.clazz = clazz;
    this.correlationId = correlationId;
    this.callbackHandler = callbackHandler;
  }

  @Override
  public Object invoke(Object proxy, Method method, Method method1, Object[] args)
      throws Throwable {
    val argsList =
        Stream.of(args).map(arg -> new Anything(arg.getClass(), arg)).collect(Collectors.toList());
    WorkflowCreationRequest req =
        WorkflowCreationRequest.builder()
            .arguments(argsList)
            .correlationId(correlationId)
            .workflowType(new WorkflowType(this.clazz))
            .callbackHandlerClazz(callbackHandler)
            .build();
    this.workflowCreationResponse = engine.createWorkflowInstance(req);
    return null;
  }
}
