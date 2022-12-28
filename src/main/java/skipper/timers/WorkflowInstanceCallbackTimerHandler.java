package skipper.timers;

import lombok.NonNull;
import skipper.SkipperEngine;
import skipper.common.ValidationUtils;

public class WorkflowInstanceCallbackTimerHandler implements TimerHandler {
  @Override
  public void process(Object payload, @NonNull SkipperEngine engine) {
    ValidationUtils.require(payload instanceof String)
        .orFail("payload '%s' must be a string", payload);
    engine.executeWorkflowInstanceCallback((String) payload);
  }
}
