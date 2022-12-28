package com.maestroworkflow;

import com.google.inject.Inject;
import com.maestroworkflow.api.WorkflowContext;
import com.maestroworkflow.models.Timer;
import com.maestroworkflow.store.TimerStore;
import com.maestroworkflow.timers.TimerHandler;
import java.time.Duration;
import java.util.Map;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerProcessor {
  private static Logger logger = LoggerFactory.getLogger(TimerProcessor.class);

  private final MaestroEngine engine;
  private final TimerStore timerStore;
  private final Duration fetchDelay = Duration.ofMillis(100);
  private final Map<Class<? extends TimerHandler>, TimerHandler> handlerMap;

  @Inject
  public TimerProcessor(
      @NonNull MaestroEngine engine,
      @NonNull TimerStore timerStore,
      @NonNull Map<Class<? extends TimerHandler>, TimerHandler> handlerMap) {
    this.engine = engine;
    this.timerStore = timerStore;
    this.handlerMap = handlerMap;
  }

  @SneakyThrows
  public void startProcessing() {
    logger.info("starting timer processing");
    while (true) {
      val timers = timerStore.getExpiredTimers();
      logger.debug("fetched {} timers to process", timers.size());
      if (timers.isEmpty()) {
        Thread.sleep(fetchDelay.toMillis());
        continue;
      }
      timers.forEach(this::processTimer);
    }
  }

  private void processTimer(Timer t) {
    if (!handlerMap.containsKey(t.getHandlerClazz())) {
      logger.error("invalid timer handler class {}", t.getHandlerClazz());
      return;
    }
    logger.info("processing timer={}", t);
    try {
      handlerMap.get(t.getHandlerClazz()).process(t.getPayload().getValue(), engine);
      if (!timerStore.delete(t)) {
        logger.info(
            "unable to delete the timer as it was probably updated by the handler. timer={}", t);
      }
    } catch (Exception e) {
      logger.error("error while processing timer: {}\n{}", e.getMessage(), e.getStackTrace());
    } finally {
      WorkflowContext.remove();
    }
  }
}
