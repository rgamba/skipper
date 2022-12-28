package com.maestroworkflow.timers;

import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.ValidationUtils;
import lombok.NonNull;

public class WorkflowInstanceCallbackTimerHandler implements TimerHandler {
    @Override
    public void process(Object payload, @NonNull MaestroEngine engine) {
        ValidationUtils.require(payload instanceof String).orFail("payload '%s' must be a string", payload);
        engine.executeWorkflowInstanceCallback((String) payload);
    }
}
