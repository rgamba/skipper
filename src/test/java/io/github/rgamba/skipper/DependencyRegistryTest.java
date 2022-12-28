package io.github.rgamba.skipper;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.annotations.WorkflowMethod;
import lombok.val;
import org.junit.Test;

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
