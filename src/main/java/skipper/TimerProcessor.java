package skipper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.models.Timer;
import skipper.runtime.DecisionThread;
import skipper.store.PartitionConfig;
import skipper.store.TimerStore;
import skipper.timers.TimerHandler;

public class TimerProcessor {
  private static Logger logger = LoggerFactory.getLogger(TimerProcessor.class);

  private final SkipperEngine engine;
  private final TimerStore timerStore;
  private final Duration fetchDelay = Duration.ofMillis(50);
  private final Map<Class<? extends TimerHandler>, TimerHandler> handlerMap;
  private final ThreadPoolExecutor executor;
  final AtomicInteger prevTimersCount = new AtomicInteger();
  private final int numberOfPartitions;

  @Inject
  public TimerProcessor(
      @NonNull SkipperEngine engine,
      @NonNull TimerStore timerStore,
      @NonNull Map<Class<? extends TimerHandler>, TimerHandler> handlerMap, int numberOfPartitions) {
    this.engine = engine;
    this.timerStore = timerStore;
    this.handlerMap = handlerMap;
    this.numberOfPartitions = numberOfPartitions;
    executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  }

  @SneakyThrows
  public void start() {
    Metrics.registerIntegerGauge(
            "timers", "expired_backlog_size", () -> (int) timerStore.countExpiredTimers());
    Metrics.registerIntegerGauge(
            "timers", "executor.queue_size", () -> executor.getQueue().size());
    IntStream.range(0, numberOfPartitions).forEach(i -> executor.submit(() -> startInternal(new PartitionConfig(numberOfPartitions, i))));
  }

  @SneakyThrows
  private void startInternal(@NonNull PartitionConfig partitionConfig) {
    Metrics.registerIntegerGauge("timers.expired_timer_fetch_count", String.format("partition_%d", partitionConfig.getCurrentPartition()), prevTimersCount::get);
    while (true) {
      try {
        startProcessing(partitionConfig);
      } catch (Throwable e) {
        logger.error("startProcessing threw unexpected error for partition {}: {}", partitionConfig.getCurrentPartition(), e.getMessage());
        Thread.sleep(100);
      }
    }
  }

  @SneakyThrows
  private void startProcessing(@NonNull PartitionConfig partitionConfig) {
    logger.info("starting timer processing for partition {}", partitionConfig.getCurrentPartition());
    while (true) {
      val timers = timerStore.getExpiredTimers(partitionConfig);
      prevTimersCount.set(timers.size());
      Metrics.getTimerProcessingCount(String.format("partition_%d", partitionConfig.getCurrentPartition())).mark(timers.size());
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
