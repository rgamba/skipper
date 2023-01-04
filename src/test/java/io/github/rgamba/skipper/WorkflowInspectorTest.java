package io.github.rgamba.skipper;

import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.annotations.StateField;
import io.github.rgamba.skipper.api.annotations.WorkflowMethod;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class WorkflowInspectorTest {

  @Test
  public void testGetState() {
    List<String> users = new ArrayList<>();
    StateParamComplexType instance = new StateParamComplexType(users);
    WorkflowInspector inspector = new WorkflowInspector(StateParamComplexType.class, instance);
    inspector.getState();
  }

  private static class StateParamComplexType implements SkipperWorkflow {
    @StateField List<String> users;

    StateParamComplexType(List<String> users) {
      this.users = users;
    }

    @WorkflowMethod
    public void test() {}
  }
}
