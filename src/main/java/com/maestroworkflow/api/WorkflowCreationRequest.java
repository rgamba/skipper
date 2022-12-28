package com.maestroworkflow.api;

import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.WorkflowType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class WorkflowCreationRequest {
    @NonNull String correlationId;
    @NonNull WorkflowType workflowType;
    @NonNull List<Anything> arguments;
    Class<? extends CallbackHandler> callbackHandlerClazz;
}
