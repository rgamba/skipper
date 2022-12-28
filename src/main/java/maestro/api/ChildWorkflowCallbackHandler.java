package maestro.api;

import lombok.NonNull;
import maestro.MaestroEngine;
import maestro.models.WorkflowInstance;

public class ChildWorkflowCallbackHandler implements CallbackHandler {
  @Override
  public void handleUpdate(
      @NonNull WorkflowInstance workflowInstance, @NonNull MaestroEngine engine) {
    if (workflowInstance.getStatus().isCompleted()) {
      engine.handleChildWorkflowCompleted(workflowInstance.getId());
    }
    // We don't care about handling other workflow statuses
  }
}
