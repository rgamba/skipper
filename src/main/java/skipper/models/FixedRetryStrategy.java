package skipper.models;

import java.time.Duration;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class FixedRetryStrategy implements RetryStrategy {
  @NonNull public Duration retryDelay;
  @NonNull public int maxRetries;

  @Override
  public Optional<Duration> getNextRetryDelay(int currentRetries) {
    if (currentRetries >= maxRetries) {
      return Optional.empty();
    }
    return Optional.of(retryDelay);
  }
}
