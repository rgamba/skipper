package maestro.module;

import com.google.inject.Inject;
import java.util.HashMap;
import lombok.NonNull;
import lombok.val;
import maestro.MaestroEngine;
import maestro.TimerProcessor;
import maestro.store.TimerStore;
import maestro.timers.DecisionTimerHandler;
import maestro.timers.OperationRequestTimerHandler;
import maestro.timers.TimerHandler;
import maestro.timers.WorkflowInstanceCallbackTimerHandler;

public class TimerProcessorFactory {
  private final TimerStore timerStore;
  private final DecisionTimerHandler decisionTimerHandler;
  private final OperationRequestTimerHandler operationRequestTimerHandler;
  private final WorkflowInstanceCallbackTimerHandler workflowInstanceCallbackTimerHandler;

  @Inject
  public TimerProcessorFactory(
      @NonNull TimerStore timerStore,
      @NonNull DecisionTimerHandler decisionTimerHandler,
      @NonNull OperationRequestTimerHandler operationRequestTimerHandler,
      @NonNull WorkflowInstanceCallbackTimerHandler workflowInstanceCallbackTimerHandler) {
    this.timerStore = timerStore;
    this.decisionTimerHandler = decisionTimerHandler;
    this.operationRequestTimerHandler = operationRequestTimerHandler;
    this.workflowInstanceCallbackTimerHandler = workflowInstanceCallbackTimerHandler;
  }

  public TimerProcessor create(@NonNull MaestroEngine engine) {
    val timerHandlers =
        new HashMap<Class<? extends TimerHandler>, TimerHandler>() {
          {
            put(DecisionTimerHandler.class, decisionTimerHandler);
            put(OperationRequestTimerHandler.class, operationRequestTimerHandler);
            put(WorkflowInstanceCallbackTimerHandler.class, workflowInstanceCallbackTimerHandler);
          }
        };
    return new TimerProcessor(engine, timerStore, timerHandlers);
  }
}
