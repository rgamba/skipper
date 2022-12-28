package skipper.timers;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.SkipperEngine;
import skipper.common.ValidationUtils;

public class DecisionTimerHandler implements TimerHandler {
  private static Logger logger = LoggerFactory.getLogger(DecisionTimerHandler.class);

  @Override
  public void process(Object payload, @NonNull SkipperEngine engine) {
    ValidationUtils.require(payload instanceof String)
        .orFail("payload '%s' must be a string", payload);
    String workflowInstanceId = (String) payload;
    logger.info("processing decision request for workflowId={}", workflowInstanceId);
    engine.processDecision(workflowInstanceId);
  }
}
