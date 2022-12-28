package skipper.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.val;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import skipper.SkipperEngine;
import skipper.api.SkipperWorkflow;
import skipper.api.WorkflowCreationRequest;
import skipper.api.WorkflowCreationResponse;
import skipper.api.annotations.WorkflowMethod;
import skipper.models.WorkflowInstance;
import skipper.models.WorkflowType;

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
