package io.github.rgamba.skipper.module;

import com.google.inject.Inject;
import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.TimerProcessor;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.timers.DecisionTimerHandler;
import io.github.rgamba.skipper.timers.OperationRequestTimerHandler;
import io.github.rgamba.skipper.timers.TimerHandler;
import io.github.rgamba.skipper.timers.WorkflowInstanceCallbackTimerHandler;
import java.util.HashMap;
import lombok.NonNull;
import lombok.val;

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
    return new TimerProcessor(engine, timerStore, timerHandlers);
  }
}
