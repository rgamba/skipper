package maestro.testUtils;

import java.time.Instant;
import java.util.ArrayList;
import lombok.NonNull;
import maestro.api.MaestroWorkflow;
import maestro.api.WorkflowContext;
import org.junit.Before;
import org.springframework.test.util.ReflectionTestUtils;

public abstract class WorkflowTest {
  @Before
  protected void setUp() {
    WorkflowContext.set(new WorkflowContext("", Instant.MIN, new ArrayList<>(), Instant.MIN));
  }

  protected void mockOperationField(
      MaestroWorkflow workflowInstance, @NonNull String fieldName, Object mockObject) {
    ReflectionTestUtils.setField(workflowInstance, fieldName, mockObject);
  }
}
