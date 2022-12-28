package skipper.api;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;
import skipper.models.RetryStrategy;

@Value
@Builder
public class OperationConfig {
  Duration timeout;
  RetryStrategy retryStrategy;
}
