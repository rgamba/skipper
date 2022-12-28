package maestro.api;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;
import maestro.models.RetryStrategy;

@Value
@Builder
public class OperationConfig {
  Duration timeout;
  RetryStrategy retryStrategy;
}
