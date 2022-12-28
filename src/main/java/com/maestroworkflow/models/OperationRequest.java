package com.maestroworkflow.models;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Builder(toBuilder = true)
@Value
public class OperationRequest implements Serializable {
    @NonNull String operationRequestId;
    @NonNull String workflowInstanceId;
    @NonNull OperationType operationType;
    int iteration;
    @NonNull List<Anything> arguments;
    @NonNull Instant creationTime;
    @NonNull RetryStrategy retryStrategy;
    @NonNull Duration timeout;
    @NonNull int failedAttempts;
}
