package demo;

import com.maestroworkflow.api.WorkflowContext;
import lombok.val;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;

public class TransferWorkflowTest {
    @Test
    public void testCreation() {
        WorkflowContext.set(new WorkflowContext("", Instant.now(), new ArrayList<>(), Instant.MIN));
        val workflow = new TransferWorkflow();
    }
}