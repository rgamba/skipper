package skipper;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import lombok.val;
import org.junit.Test;
import skipper.api.SkipperWorkflow;
import skipper.api.annotations.WorkflowMethod;

public class DependencyRegistryTest {
  public static class TestWorkflow implements SkipperWorkflow {
    @WorkflowMethod
    public void test() {}
  }

  @Test
  public void testWorkflowFactoryIsNotASingleton() {
    TestWorkflow testWorkflow = new TestWorkflow();
    val error =
        assertThrows(
            IllegalArgumentException.class,
            () -> DependencyRegistry.builder().addWorkflowFactory(() -> testWorkflow).build());
    assertTrue(error.getMessage().contains("must not be a singleton"));
  }

  @Test
  public void testWorkflowFactoryHappyPath() {
    DependencyRegistry.builder().addWorkflowFactory(TestWorkflow::new).build();
  }
}
