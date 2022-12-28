package skipper.timers;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.SkipperEngine;
import skipper.common.ValidationUtils;

public class OperationRequestTimerHandler implements TimerHandler {
  private static Logger logger = LoggerFactory.getLogger(OperationRequestTimerHandler.class);

  @Override
  public void process(Object payload, @NonNull SkipperEngine engine) {
    ValidationUtils.require(payload instanceof String)
        .orFail("payload '%s' must be a string", payload);
    String operationRequestId = (String) payload;
    logger.info("processing operation request with id={}", operationRequestId);
    engine.processOperationRequest(operationRequestId);
  }
}
