package demo;

import com.maestroworkflow.api.WorkflowContext;
import java.time.Instant;
import java.util.ArrayList;
import lombok.val;
import org.junit.Test;

public class TransferWorkflowTest {
  @Test
  public void testCreation() {
    WorkflowContext.set(new WorkflowContext("", Instant.now(), new ArrayList<>(), Instant.MIN));
    val workflow = new TransferWorkflow();
  }
}
