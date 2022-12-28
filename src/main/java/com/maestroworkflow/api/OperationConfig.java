package com.maestroworkflow.api;

import com.maestroworkflow.models.RetryStrategy;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class OperationConfig {
    Duration timeout;
    RetryStrategy retryStrategy;
}
