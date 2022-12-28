package io.github.rgamba.skipper.testUtils;

import io.github.rgamba.skipper.WorkflowInspector;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.runtime.WorkflowContext;
import java.time.Instant;
import java.util.ArrayList;
import lombok.NonNull;
import org.junit.Before;
import org.springframework.test.util.ReflectionTestUtils;

public abstract class WorkflowTest {
  @Before
  protected void setUp() {
    DecisionThread.setWorkflowContext(
        new WorkflowContext("", Instant.MIN, new ArrayList<>(), Instant.MIN));
  }

  protected void mockOperationField(
      SkipperWorkflow workflowInstance, @NonNull String fieldName, Object mockObject) {
    ReflectionTestUtils.setField(workflowInstance, fieldName, mockObject);
  }

  protected void assertWorkflowIsValid(@NonNull SkipperWorkflow workflow) {
    WorkflowInspector inspector = new WorkflowInspector(workflow.getClass(), workflow);
    inspector.getWorkflowMethod();
  }
}
