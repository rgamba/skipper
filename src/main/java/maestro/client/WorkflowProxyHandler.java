package maestro.client;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.util.proxy.MethodHandler;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import maestro.MaestroEngine;
import maestro.api.CallbackHandler;
import maestro.api.MaestroWorkflow;
import maestro.api.WorkflowCreationRequest;
import maestro.api.WorkflowCreationResponse;
import maestro.common.Anything;
import maestro.models.WorkflowType;

public class WorkflowProxyHandler implements MethodHandler {
  private final MaestroEngine engine;
  private final Class<? extends MaestroWorkflow> clazz;
  private final String correlationId;
  private final Class<? extends CallbackHandler> callbackHandler;
  @Getter private WorkflowCreationResponse workflowCreationResponse;

  public WorkflowProxyHandler(
      @NonNull MaestroEngine engine,
      @NonNull Class<? extends MaestroWorkflow> clazz,
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
