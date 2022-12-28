package com.maestroworkflow.api;

import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.NonNull;

public interface CallbackHandler {
  void handleUpdate(@NonNull WorkflowInstance workflowInstance, @NonNull MaestroEngine engine);
}
