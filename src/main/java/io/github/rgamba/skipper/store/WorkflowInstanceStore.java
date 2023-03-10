package io.github.rgamba.skipper.store;

import io.github.rgamba.skipper.models.WorkflowInstance;
import java.util.List;
import lombok.NonNull;

public interface WorkflowInstanceStore {
  void create(@NonNull WorkflowInstance workflowInstance);

  WorkflowInstance get(@NonNull String workflowInstanceId);

  List<WorkflowInstance> find();

  void update(
      @NonNull String workflowInstanceId,
      @NonNull WorkflowInstance.Mutation mutation,
      int currentVersion);
}
