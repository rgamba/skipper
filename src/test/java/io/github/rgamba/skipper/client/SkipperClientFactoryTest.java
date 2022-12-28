package io.github.rgamba.skipper.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WorkflowCreationRequest;
import io.github.rgamba.skipper.api.WorkflowCreationResponse;
import io.github.rgamba.skipper.api.annotations.WorkflowMethod;
import io.github.rgamba.skipper.models.WorkflowInstance;
import io.github.rgamba.skipper.models.WorkflowType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.val;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SkipperClientFactoryTest {
  private final WorkflowInstance wf1 =
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

  @Test
  public void testCreate() throws Exception {
    SkipperEngine engine = mock(SkipperEngine.class);
    WorkflowCreationResponse resp =
        WorkflowCreationResponse.builder().workflowInstance(wf1).build();
    when(engine.createWorkflowInstance(any())).thenReturn(resp);
    val factory = new SkipperClientFactory(engine);
    val client = factory.create(Foo.class, "correlation1", null);
    client.bar("test");
    ArgumentCaptor<WorkflowCreationRequest> argumentCaptor =
        ArgumentCaptor.forClass(WorkflowCreationRequest.class);
    verify(engine, times(1)).createWorkflowInstance(argumentCaptor.capture());
    assertEquals("correlation1", argumentCaptor.getValue().getCorrelationId());
  }

  public static class Foo implements SkipperWorkflow {
    @WorkflowMethod
    public String bar(String a) {
      return a;
    }
  }
}
