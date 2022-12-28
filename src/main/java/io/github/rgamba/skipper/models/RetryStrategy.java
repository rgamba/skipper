package io.github.rgamba.skipper.models;

import java.time.Duration;
import java.util.Optional;

public interface RetryStrategy {
  /**
   * Gets the time that we should wait before the next retry
   *
   * @return The duration to wait or None in case the retries have been exhausted and no more
   *     retries should be scheduled.
   */
  Optional<Duration> getNextRetryDelay(int currentRetries);
}
