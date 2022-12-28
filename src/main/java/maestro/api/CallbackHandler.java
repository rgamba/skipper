package maestro.api;

import lombok.NonNull;
import maestro.MaestroEngine;
import maestro.models.WorkflowInstance;

public interface CallbackHandler {
  void handleUpdate(@NonNull WorkflowInstance workflowInstance, @NonNull MaestroEngine engine);
}
