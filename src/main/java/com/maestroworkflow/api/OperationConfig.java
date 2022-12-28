package com.maestroworkflow.api;

import com.maestroworkflow.models.RetryStrategy;
import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OperationConfig {
  Duration timeout;
  RetryStrategy retryStrategy;
}
