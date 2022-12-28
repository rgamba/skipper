package maestro.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.val;
import maestro.MaestroEngine;
import maestro.api.MaestroWorkflow;
import maestro.api.WorkflowCreationRequest;
import maestro.api.WorkflowCreationResponse;
import maestro.api.annotations.WorkflowMethod;
import maestro.models.WorkflowInstance;
import maestro.models.WorkflowType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MaestroClientFactoryTest {
  private final WorkflowInstance wf1 =
      WorkflowInstance.builder()
          .id("wf1")
          .workflowType(new WorkflowType(MaestroWorkflow.class))
          .status(WorkflowInstance.Status.ACTIVE)
          .state(new HashMap<>())
          .correlationId("corr1")
          .version(1)
          .initialArgs(new ArrayList<>())
          .creationTime(Instant.MIN)
          .build();

  @Test
  public void testCreate() throws Exception {
    MaestroEngine engine = mock(MaestroEngine.class);
    WorkflowCreationResponse resp =
        WorkflowCreationResponse.builder().workflowInstance(wf1).build();
    when(engine.createWorkflowInstance(any())).thenReturn(resp);
    val factory = new MaestroClientFactory(engine);
    val client = factory.create(Foo.class, "correlation1", null);
    client.bar("test");
    ArgumentCaptor<WorkflowCreationRequest> argumentCaptor =
        ArgumentCaptor.forClass(WorkflowCreationRequest.class);
    verify(engine, times(1)).createWorkflowInstance(argumentCaptor.capture());
    assertEquals("correlation1", argumentCaptor.getValue().getCorrelationId());
  }

  public static class Foo implements MaestroWorkflow {
    @WorkflowMethod
    public String bar(String a) {
      return a;
    }
  }
}
