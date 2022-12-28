package com.maestroworkflow.api;

import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.WorkflowType;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class WorkflowCreationRequest {
  @NonNull String correlationId;
  @NonNull WorkflowType workflowType;
  @NonNull List<Anything> arguments;
  Class<? extends CallbackHandler> callbackHandlerClazz;
}
