package io.github.rgamba.skipper;

import com.google.inject.Inject;
import io.github.rgamba.skipper.models.Timer;
import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.timers.TimerHandler;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerProcessor {
  private static Logger logger = LoggerFactory.getLogger(TimerProcessor.class);

  private final SkipperEngine engine;
  private final TimerStore timerStore;
  private final Duration fetchDelay = Duration.ofMillis(50);
  private final Map<Class<? extends TimerHandler>, TimerHandler> handlerMap;
  private final ThreadPoolExecutor executor;
  final AtomicInteger prevTimersCount = new AtomicInteger();

  @Inject
  public TimerProcessor(
      @NonNull SkipperEngine engine,
      @NonNull TimerStore timerStore,
      @NonNull Map<Class<? extends TimerHandler>, TimerHandler> handlerMap) {
    this.engine = engine;
    this.timerStore = timerStore;
    this.handlerMap = handlerMap;
    executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  }

  @SneakyThrows
  public void start() {
    executor.submit(this::startInternal);
  }

  @SneakyThrows
  private void startInternal() {
    Metrics.registerIntegerGauge(
        "timers", "expired_backlog_size", () -> (int) timerStore.countExpiredTimers());
    Metrics.registerIntegerGauge("timers", "expired_timer_fetch_count", prevTimersCount::get);
    while (true) {
      try {
        startProcessing();
      } catch (Throwable e) {
        logger.error("startProcessing threw unexpected error: {}", e.getMessage());
        Thread.sleep(100);
      }
    }
  }

  @SneakyThrows
  private void startProcessing() {
    logger.info("starting timer processing");
    while (true) {
      val timers = timerStore.getExpiredTimers();
      prevTimersCount.set(timers.size());
      Metrics.getTimerProcessingCount("all").mark(timers.size());
      logger.debug("fetched {} timers to process", timers.size());
      if (timers.isEmpty()) {
        Thread.sleep(fetchDelay.toMillis());
        continue;
      }

      timers.forEach(
          timer -> {
            try (val unused = Metrics.TIMERS_DISPATCH_LATENCY_TIMER.time()) {
              executor.execute(() -> processTimer(timer));
            }
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
      logger.warn(
          "error while processing timer '{}': {}\n{}",
          t.getTimerId(),
          e.getMessage(),
          e.getStackTrace());
      try {
        timerStore.update(
            t.getTimerId(), Duration.ZERO); // Give it some breathing room before we retry
      } catch (Exception e1) {
        logger.error(
            "unable to re-schedule timer '{}', it will be re-executed on lease expiration. error={}",
            t.getTimerId(),
            e1.getMessage());
      }
    } finally {
      DecisionThread.clear();
    }
  }
}
