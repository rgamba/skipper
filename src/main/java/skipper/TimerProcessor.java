package skipper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.models.Timer;
import skipper.runtime.DecisionThread;
import skipper.store.TimerStore;
import skipper.timers.TimerHandler;

public class TimerProcessor {
  private static Logger logger = LoggerFactory.getLogger(TimerProcessor.class);

  private final SkipperEngine engine;
  private final TimerStore timerStore;
  private final Duration fetchDelay = Duration.ofMillis(100);
  private final Map<Class<? extends TimerHandler>, TimerHandler> handlerMap;
  private final ThreadPoolExecutor executor;

  @Inject
  public TimerProcessor(
      @NonNull SkipperEngine engine,
      @NonNull TimerStore timerStore,
      @NonNull Map<Class<? extends TimerHandler>, TimerHandler> handlerMap) {
    this.engine = engine;
    this.timerStore = timerStore;
    this.handlerMap = handlerMap;
    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
  }

  @SneakyThrows
  public void start() {
    executor.submit(this::startInternal);
  }

  @SneakyThrows
  private void startInternal() {
    while (true) {
      try {
        startProcessing();
      } catch (Throwable e) {
        logger.error("startProcessing threw unexpected error: {}", e.getMessage());
        Thread.sleep(1000);
      }
    }
  }

  @SneakyThrows
  private void startProcessing() {
    logger.info("starting timer processing");
    while (true) {
      val timers = timerStore.getExpiredTimers();
      logger.debug("fetched {} timers to process", timers.size());
      if (timers.isEmpty()) {
        Thread.sleep(fetchDelay.toMillis());
        continue;
      }
      timers.forEach(
          timer -> {
            executor.execute(() -> processTimer(timer));
          });
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
      DecisionThread.clear();
    }
  }
}
