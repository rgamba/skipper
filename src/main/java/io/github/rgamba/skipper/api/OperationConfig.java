package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.models.RetryStrategy;
import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OperationConfig {
  Duration timeout;
  RetryStrategy retryStrategy;
}
