package com.maestroworkflow.store;

import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

public interface WorkflowInstanceStore {
    void create(@NonNull WorkflowInstance workflowInstance);

    WorkflowInstance get(@NonNull String workflowInstanceId);

    List<WorkflowInstance> find();

    void update(@NonNull String workflowInstanceId, @NonNull WorkflowInstance.Mutation mutation);
}
