package skipper.module;

import com.google.inject.Inject;
import java.util.HashMap;
import lombok.NonNull;
import lombok.val;
import skipper.SkipperEngine;
import skipper.TimerProcessor;
import skipper.store.TimerStore;
import skipper.timers.DecisionTimerHandler;
import skipper.timers.OperationRequestTimerHandler;
import skipper.timers.TimerHandler;
import skipper.timers.WorkflowInstanceCallbackTimerHandler;

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

  public TimerProcessor create(@NonNull SkipperEngine engine) {
    val timerHandlers =
        new HashMap<Class<? extends TimerHandler>, TimerHandler>() {
          {
            put(DecisionTimerHandler.class, decisionTimerHandler);
            put(OperationRequestTimerHandler.class, operationRequestTimerHandler);
            put(WorkflowInstanceCallbackTimerHandler.class, workflowInstanceCallbackTimerHandler);
          }
        };
    return new TimerProcessor(engine, timerStore, timerHandlers, 1);
  }
}
