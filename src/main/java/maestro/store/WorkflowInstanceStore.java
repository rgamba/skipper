package maestro.store;

import java.util.List;
import lombok.NonNull;
import maestro.models.WorkflowInstance;

public interface WorkflowInstanceStore {
  void create(@NonNull WorkflowInstance workflowInstance);

  WorkflowInstance get(@NonNull String workflowInstanceId);

  List<WorkflowInstance> find();

  void update(
      @NonNull String workflowInstanceId,
      @NonNull WorkflowInstance.Mutation mutation,
      int currentVersion);
}
