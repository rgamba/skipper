package com.maestroworkflow.api;

import com.maestroworkflow.models.OperationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Value
@Builder
@AllArgsConstructor
public class WorkflowContext {
    @NonNull String workflowInstanceId;
    @NonNull Instant currentTime;
    @NonNull List<OperationResponse> operationResponses;
    @NonNull Instant workflowInstanceCreationTime;

    private static final ThreadLocal<WorkflowContext> workflowContext = new ThreadLocal<>();
    // This holds the latest time checkpoint for the current workflow execution. When we execute a workflow, we
    // replay all workflow operations from the beginning and every time we hit an operation for which we have a
    // result recorded, we consider that to be the latestCurrentExecutionCheckpoint.
    private static final ThreadLocal<Instant> latestCurrentExecutionCheckpoint = new ThreadLocal<>();

    public static void set(@NonNull WorkflowContext context) {
        workflowContext.set(context);
    }

    public static void remove() {
        workflowContext.remove();
    }

    public static WorkflowContext get() {
        if (workflowContext.get() == null) {
            throw new IllegalStateException("workflowContext is null");
        }
        return workflowContext.get();
    }

    public static Optional<Instant> getLatestCurrentExecutionCheckpoint() {
        return Optional.ofNullable(latestCurrentExecutionCheckpoint.get());
    }

    public static void setLatestCurrentExecutionCheckpoint(@NonNull Instant checkpoint) {
        latestCurrentExecutionCheckpoint.set(checkpoint);
    }

    public static void removeLatestCurrentExecutionCheckpoint() {
        latestCurrentExecutionCheckpoint.remove();
    }
}
