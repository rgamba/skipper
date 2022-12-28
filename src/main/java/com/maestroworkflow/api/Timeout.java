package com.maestroworkflow.api;

import com.maestroworkflow.models.OperationResponse;
import lombok.NonNull;

import java.time.Duration;
import java.time.Instant;

public class Timeout extends Promise {
    public Timeout(@NonNull Duration timeout) {
        super(() -> {
            Instant lastCheckpoint = WorkflowContext.getLatestCurrentExecutionCheckpoint()
                    .orElse(WorkflowContext.get().getWorkflowInstanceCreationTime());
            Instant now = WorkflowContext.get().getCurrentTime();
            Instant timeoutEnd = lastCheckpoint.plus(timeout);
            if (now.isBefore(timeoutEnd)) {
                Duration timeDelta = Duration.between(now, timeoutEnd);
                throw new StopWorkflowExecution(timeDelta);
            }
            return null;
        });
    }

    public static Timeout of(@NonNull Duration duration) {
        return new Timeout(duration);
    }
}
