package skipper.api;

import lombok.NonNull;
import skipper.SkipperEngine;
import skipper.models.WorkflowInstance;

public interface CallbackHandler {
  void handleUpdate(@NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine);
}
