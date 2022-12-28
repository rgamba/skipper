package maestro;

import com.google.inject.Injector;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import maestro.api.DecisionRequest;
import maestro.api.DecisionResponse;
import maestro.api.StopWorkflowExecution;
import maestro.common.Anything;
import maestro.models.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionExecutor {
  private static Logger logger = LoggerFactory.getLogger(MaestroEngine.class);

  public DecisionResponse execute(
      @NonNull DecisionRequest decisionRequest, @NonNull Injector injector) {
    DecisionResponse.DecisionResponseBuilder builder =
        DecisionResponse.builder().operationRequests(new ArrayList<>());
    try {
      builder = executeInternal(decisionRequest, injector);
      builder.newStatus(WorkflowInstance.Status.COMPLETED);
    } catch (StopWorkflowExecution e) {
      // We need to override some fields passed by the handler
      val reqs =
          e.getOperationRequests().stream()
              .map(
                  req ->
                      req.toBuilder()
                          .workflowInstanceId(decisionRequest.getWorkflowInstance().getId())
                          .build())
              .collect(Collectors.toList());
      builder.operationRequests(reqs);
      builder.waitForDuration(e.getWaitForDuration());
      builder.newStatus(WorkflowInstance.Status.ACTIVE);
      builder.newState(e.getNewState());
    } catch (Exception e) {
      builder.newStatus(WorkflowInstance.Status.ERROR);
      String reason = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
      builder.statusReason(reason == null ? "No error message" : reason);
    }
    return builder.build();
  }

  @SneakyThrows
  public DecisionResponse.DecisionResponseBuilder executeInternal(
      @NonNull DecisionRequest decisionRequest, @NonNull Injector injector) {
    val clazz = decisionRequest.getWorkflowInstance().getWorkflowType().getClazz();
    val decider = injector.getInstance(clazz);
    val inspector = new WorkflowInspector(clazz, decider);
    inspector.setState(decisionRequest.getWorkflowInstance().getState());
    val initialState = inspector.getState();
    val workflowMethod = inspector.getWorkflowMethod();
    val params =
        inspector.getWorkflowMethodParams(decisionRequest.getWorkflowInstance().getInitialArgs());
    Object result = null;
    try {
      result = workflowMethod.invoke(decider, params);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          String.format("unable to invoke workflow method on workflow %s", clazz), e);
    } catch (InvocationTargetException e) {
      if (e.getCause() != null && e.getCause() instanceof StopWorkflowExecution) {
        throw ((StopWorkflowExecution) e.getCause())
            .toBuilder()
            .newState(inspector.getState())
            .build();
      }
      if (e.getCause() != null) {
        throw e.getCause();
      }
      throw new RuntimeException(e);
    }
    val finalState = inspector.getState();
    val response = DecisionResponse.builder().operationRequests(new ArrayList<>());
    response.newState(finalState);
    if (result != null) {
      response.result(new Anything(workflowMethod.getReturnType(), result));
    }
    return response;
  }

  @SneakyThrows
  public Map<String, Anything> executeSignalConsumer(
      @NonNull WorkflowInstance instance,
      @NonNull Injector injector,
      @NonNull String signalMethodName,
      @NonNull List<Anything> args) {
    val clazz = instance.getWorkflowType().getClazz();
    val decider = injector.getInstance(clazz);
    val inspector = new WorkflowInspector(clazz, decider);
    val signalMethod = inspector.getSignalConsumerMethod(signalMethodName);
    val params = inspector.getMethodParams(signalMethod, args);
    try {
      signalMethod.invoke(decider, params);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          String.format(
              "unable to invoke signal consumer method '%s' on workflow %s",
              signalMethodName, clazz),
          e);
    } catch (InvocationTargetException e) {
      if (e.getCause() != null) {
        throw e.getCause();
      }
      throw new RuntimeException(e);
    }
    return inspector.getState();
  }
}
