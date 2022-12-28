package io.github.rgamba.skipper.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.WorkflowInstance;
import io.github.rgamba.skipper.models.WorkflowType;
import io.github.rgamba.skipper.store.inmemory.InMemoryWorkflowInstanceStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.val;
import org.junit.Test;

public class WorkflowInstanceStoreTest {
  @Test
  public void testWorkflowInstance() {
    val store = new InMemoryWorkflowInstanceStore();
    val w1 =
        WorkflowInstance.builder()
            .id("wf1")
            .workflowType(new WorkflowType(SkipperWorkflow.class))
            .status(WorkflowInstance.Status.ACTIVE)
            .state(new HashMap<>())
            .correlationId("corr1")
            .version(1)
            .initialArgs(new ArrayList<>())
            .creationTime(Instant.MIN)
            .build();
    store.create(w1);
    assertEquals(w1, store.get(w1.getId()));
    assertThrows(IllegalArgumentException.class, () -> store.create(w1));

    store.update(
        w1.getId(),
        WorkflowInstance.Mutation.builder().status(WorkflowInstance.Status.COMPLETED).build(),
        w1.getVersion());
    assertEquals(WorkflowInstance.Status.COMPLETED, store.get(w1.getId()).getStatus());
    val state = new HashMap<String, Anything>();
    state.put("foo", new Anything(String.class, "bar"));
    store.update(
        w1.getId(), WorkflowInstance.Mutation.builder().state(state).build(), w1.getVersion());
  }
}
