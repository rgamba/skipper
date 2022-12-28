package com.maestroworkflow.store;

import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.WorkflowInstance;
import com.maestroworkflow.models.WorkflowType;
import lombok.val;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class WorkflowInstanceStoreTest {
    @Test
    public void testWorkflowInstance() {
        val store = new InMemoryWorkflowInstanceStore();
        val w1 = WorkflowInstance.builder()
                .id("wf1")
                .workflowType(new WorkflowType(MaestroWorkflow.class))
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

         store.update(w1.getId(), WorkflowInstance.Mutation.builder().status(WorkflowInstance.Status.COMPLETED).build());
         assertEquals(WorkflowInstance.Status.COMPLETED, store.get(w1.getId()).getStatus());
         val state = new HashMap<String, Anything>();
         state.put("foo", new Anything(String.class, "bar"));
        store.update(w1.getId(), WorkflowInstance.Mutation.builder().state(state).build());
    }
}
