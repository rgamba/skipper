package com.maestroworkflow.models;

import com.maestroworkflow.ValidationUtils;
import com.maestroworkflow.api.CallbackHandler;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@Value
public class WorkflowInstance implements Serializable {
    @NonNull String id;
    @NonNull String correlationId;
    @NonNull WorkflowType workflowType;
    Anything result;
    @NonNull Map<String, Anything> state;
    @NonNull List<Anything> initialArgs;
    int version;
    @NonNull Status status;
    String statusReason;
    Class<? extends CallbackHandler> callbackHandlerClazz;

    @NonNull Instant creationTime;

    public WorkflowInstance(@NonNull String id, @NonNull String correlationId, @NonNull WorkflowType workflowType,
                            Anything result, @NonNull Map<String, Anything> state, @NonNull List<Anything> initialArgs,
                            int version, @NonNull Status status, String statusReason, Class<? extends CallbackHandler> callbackHandlerClazz, @NonNull Instant creationTime) {
        ValidationUtils.when(status.isError()).thenExpect(statusReason != null, "when status == ERROR, statusReason must be provided");
        this.id = id;
        this.correlationId = correlationId;
        this.workflowType = workflowType;
        this.result = result;
        this.state = state;
        this.initialArgs = initialArgs;
        this.version = version;
        this.status = status;
        this.statusReason = statusReason;
        this.callbackHandlerClazz = callbackHandlerClazz;
        this.creationTime = creationTime;
    }

    public enum Status {
        ACTIVE,
        COMPLETED,
        ERROR;

        public boolean isCompleted() {
            return this.equals(Status.COMPLETED);
        }

        public boolean isError() {
            return this.equals(Status.ERROR);
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class Mutation {
        Status status;
        Map<String, Anything> state;
        String statusReason;
        Anything result;
    }
}
