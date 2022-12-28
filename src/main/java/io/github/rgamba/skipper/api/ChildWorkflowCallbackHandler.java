package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.models.WorkflowInstance;
import lombok.NonNull;

public class ChildWorkflowCallbackHandler implements CallbackHandler {
  @Override
  public void handleUpdate(
      @NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine) {
    if (workflowInstance.getStatus().isCompleted()) {
      engine.handleChildWorkflowCompleted(workflowInstance.getId());
    }
    // We don't care about handling other workflow statuses
  }
}
