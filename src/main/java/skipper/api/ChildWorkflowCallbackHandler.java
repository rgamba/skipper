package skipper.api;

import lombok.NonNull;
import skipper.SkipperEngine;
import skipper.models.WorkflowInstance;

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
