package com.maestroworkflow.api;

import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.NonNull;

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
