package io.github.rgamba.skipper;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.*;
import java.util.concurrent.Callable;

public class Metrics {
  public static MetricRegistry registry = new MetricRegistry();

  public static Meter WORKFLOW_INSTANCE_CREATION_COUNT =
      registry.meter(name("workflow", "creation", "count"));

  public static Timer TIMERS_DISPATCH_LATENCY_TIMER =
      registry.timer(name("timers", "dispatch", "latency"));

  public static Timer getDecisionLatencyTimer(Class<?> className) {
    return registry.timer(name("workflow", "decision", "latency", className.getSimpleName()));
  }

  public static Timer getOperationExecutionLatencyTimer(Class<?> className, String method) {
    return registry.timer(
        name("operation", "execution", "latency", className.getSimpleName(), method));
  }

  public static Timer getStoreLatencyTimer(String component, String operation) {
    return registry.timer(name("db", component, operation, "latency"));
  }

  public static Counter errorCounter(String component, String detail) {
    return registry.counter(name("errors", component, detail));
  }

  public static Counter getCounter(String component, String detail) {
    return registry.counter(name(component, detail, "count"));
  }

  public static Meter getTimerProcessingCount(String timerType) {
    return registry.meter(name("timers", timerType, "process", "count"));
  }

  public static void registerIntegerGauge(
      String component, String detail, Callable<Integer> callable) {
    Metrics.registry.register(
        name(component, detail),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            try {
              return callable.call();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
  }
}
