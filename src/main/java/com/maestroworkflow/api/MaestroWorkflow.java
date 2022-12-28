package com.maestroworkflow.api;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.NonNull;

public interface MaestroWorkflow {
  default List<Object> join(@NonNull Callable<Object>... promises) {
    return Control.join(promises);
  }

  default void waitUntil(@NonNull Callable<Boolean> condition, Duration timeout) {
    Control.waitUntil(condition, timeout);
  }
}
