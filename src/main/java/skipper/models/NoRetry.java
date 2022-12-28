package skipper.models;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import lombok.Value;

@Value
public class NoRetry implements RetryStrategy, Serializable {
  String type = NoRetry.class.getName();

  @Override
  public Optional<Duration> getNextRetryDelay(int currentRetries) {
    return Optional.empty();
  }
}
