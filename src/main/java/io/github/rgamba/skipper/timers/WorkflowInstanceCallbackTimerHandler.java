package io.github.rgamba.skipper.timers;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.common.ValidationUtils;
import lombok.NonNull;

public class WorkflowInstanceCallbackTimerHandler implements TimerHandler {
  @Override
  public void process(Object payload, @NonNull SkipperEngine engine) {
    ValidationUtils.require(payload instanceof String)
        .orFail("payload '%s' must be a string", payload);
    engine.executeWorkflowInstanceCallback((String) payload);
  }
}
