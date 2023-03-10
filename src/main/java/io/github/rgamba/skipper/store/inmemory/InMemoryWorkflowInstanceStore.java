package io.github.rgamba.skipper.store.inmemory;

import com.google.inject.Singleton;
import io.github.rgamba.skipper.models.WorkflowInstance;
import io.github.rgamba.skipper.store.WorkflowInstanceStore;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.val;

@Singleton
public class InMemoryWorkflowInstanceStore implements WorkflowInstanceStore {
  private List<WorkflowInstance> data;

  public InMemoryWorkflowInstanceStore() {
    this.data = new ArrayList<>();
  }

  @Override
  public void create(@NonNull WorkflowInstance workflowInstance) {
    if (this.data.stream().anyMatch(wf -> wf.getId().equals(workflowInstance.getId()))) {
      throw new IllegalArgumentException("workflow instance with that id already exists");
    }
    this.data.add(workflowInstance);
  }

  @Override
  public WorkflowInstance get(@NonNull String workflowInstanceId) {
    Optional<WorkflowInstance> instance =
        this.data.stream().filter(wf -> wf.getId().equals(workflowInstanceId)).findFirst();
    if (instance.isPresent()) {
      return instance.get();
    }
    throw new IllegalArgumentException("invalid workflow ID provided");
  }

  public List<WorkflowInstance> find() {
    return new ArrayList<>(this.data);
  }

  @Override
  public void update(
      @NonNull String workflowInstanceId,
      @NonNull WorkflowInstance.Mutation mutation,
      int version) {
    this.data =
        this.data.stream()
            .map(
                wf -> {
                  if (wf.getId().equals(workflowInstanceId)) {
                    val builder = wf.toBuilder().version(wf.getVersion() + 1);
                    if (mutation.getState() != null) builder.state(mutation.getState()).build();
                    if (mutation.getStatus() != null) builder.status(mutation.getStatus());
                    if (mutation.getStatusReason() != null)
                      builder.statusReason(mutation.getStatusReason());
                    if (mutation.getResult() != null) builder.result(mutation.getResult());
                    return builder.build();
                  }
                  return wf;
                })
            .collect(Collectors.toList());
  }
}
