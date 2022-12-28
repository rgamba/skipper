package com.maestroworkflow.module;

import com.google.inject.Inject;
import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.TimerProcessor;
import com.maestroworkflow.store.TimerStore;
import com.maestroworkflow.timers.DecisionTimerHandler;
import com.maestroworkflow.timers.OperationRequestTimerHandler;
import com.maestroworkflow.timers.TimerHandler;
import com.maestroworkflow.timers.WorkflowInstanceCallbackTimerHandler;
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
