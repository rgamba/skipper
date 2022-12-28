package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.models.WorkflowInstance;
import lombok.NonNull;

public interface CallbackHandler {
  void handleUpdate(@NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine);
}
