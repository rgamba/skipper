package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.common.ValidationUtils;
import io.github.rgamba.skipper.models.WorkflowType;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class WorkflowCreationRequest {
  public static int CORRELATION_ID_MAX_SIZE = 100;

  @NonNull String correlationId;
  @NonNull WorkflowType workflowType;
  @NonNull List<Anything> arguments;
  Class<? extends CallbackHandler> callbackHandlerClazz;

  public WorkflowCreationRequest(
      @NonNull String correlationId,
      @NonNull WorkflowType workflowType,
      @NonNull List<Anything> arguments,
      Class<? extends CallbackHandler> callbackHandlerClazz) {
    ValidationUtils.require(correlationId.length() < CORRELATION_ID_MAX_SIZE)
        .orFail("correlationId must be shorter than %d chars", CORRELATION_ID_MAX_SIZE);
    this.correlationId = correlationId;
    this.workflowType = workflowType;
    this.arguments = arguments;
    this.callbackHandlerClazz = callbackHandlerClazz;
  }
}
