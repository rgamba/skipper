package maestro.timers;

import lombok.NonNull;
import maestro.MaestroEngine;
import maestro.ValidationUtils;

public class WorkflowInstanceCallbackTimerHandler implements TimerHandler {
  @Override
  public void process(Object payload, @NonNull MaestroEngine engine) {
    ValidationUtils.require(payload instanceof String)
        .orFail("payload '%s' must be a string", payload);
    engine.executeWorkflowInstanceCallback((String) payload);
  }
}
