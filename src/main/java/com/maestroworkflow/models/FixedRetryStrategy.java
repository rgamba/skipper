package com.maestroworkflow.models;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;
import java.util.Optional;

@Value
@Builder
public class FixedRetryStrategy implements RetryStrategy {
    @NonNull Duration retryDelay;
    @NonNull int maxRetries;

    @Override
    public Optional<Duration> getNextRetryDelay(int currentRetries) {
        if (currentRetries >= maxRetries) {
            return Optional.empty();
        }
        return Optional.of(retryDelay);
    }
}

