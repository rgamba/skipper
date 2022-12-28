package com.maestroworkflow.models;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class CurrentOperationExecution {
    @NonNull String workflowInstanceId;
    @NonNull String operationRequestId;
    @NonNull Instant executionStartTime;
}
