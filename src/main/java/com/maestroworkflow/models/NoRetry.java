package com.maestroworkflow.models;

import java.time.Duration;
import java.util.Optional;

public class NoRetry implements RetryStrategy {
    @Override
    public Optional<Duration> getNextRetryDelay(int currentRetries) {
        return Optional.empty();
    }
}
