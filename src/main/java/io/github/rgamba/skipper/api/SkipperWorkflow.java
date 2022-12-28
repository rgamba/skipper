package io.github.rgamba.skipper.api;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.NonNull;

public interface SkipperWorkflow {
  static String IDEMPOTENCY_TOKEN_PLACEHOLDER = "__IDEMPOTENCY_TOKEN_PLACEHOLDER__";

  default List<Object> join(@NonNull Promise... promises) {
    return Control.join(promises);
  }

  default List<Object> joinAll(@NonNull List<Promise> promises) {
    Promise[] arr = new Promise[promises.size()];
    return Control.join(promises.toArray(arr));
  }

  default void waitUntil(@NonNull Callable<Boolean> condition, Duration timeout) {
    Control.waitUntil(condition, timeout);
  }

  /**
   * Returns an idempotency token placeholder that will be replaced by an actual idempotency token
   * created for the operation request in question. This is only useful when called as an operation
   * argument.
   *
   * @return An idempotency token placeholder.
   */
  default String genIdempotencyToken() {
    return IDEMPOTENCY_TOKEN_PLACEHOLDER;
  }
}
