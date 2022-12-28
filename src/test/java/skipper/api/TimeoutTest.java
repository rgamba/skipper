package skipper.api;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Test;
import skipper.runtime.DecisionThread;
import skipper.runtime.StopWorkflowExecution;
import skipper.runtime.WorkflowContext;

@NotThreadSafe
public class TimeoutTest {
  private final Instant t1 = Instant.MIN;
  private final Instant t2 = t1.plus(1, ChronoUnit.SECONDS);
  private final Instant t3 = t2.plus(1, ChronoUnit.SECONDS);

  @Test
  public void testTimeoutWhenTimeoutHasNotExpired() throws Exception {
    WorkflowContext context = new WorkflowContext("1", t1, new ArrayList<>(), t1);
    DecisionThread.setWorkflowContext(context);
    DecisionThread.removeLatestCurrentExecutionCheckpoint();

    Timeout timeout = Timeout.of(Duration.ofSeconds(1));
    try {
      timeout.call();
      throw new RuntimeException("Expected timeout call to throw StopWorkflowExecution");
    } catch (StopWorkflowExecution e) {
      assertEquals(Duration.ofSeconds(1), e.getWaitForDuration());
    }
  }

  @Test
  public void testTimeoutWhenTimeoutHasExpired() throws Exception {
    WorkflowContext context = new WorkflowContext("1", t2, new ArrayList<>(), t1);
    DecisionThread.setWorkflowContext(context);
    DecisionThread.removeLatestCurrentExecutionCheckpoint();

    Timeout timeout = Timeout.of(Duration.ofSeconds(1));
    timeout.call();
  }

  @Test
  public void testTimeoutWhenTimeoutHasNotExpiredAndCheckpointIsPresent() throws Exception {
    WorkflowContext context = new WorkflowContext("1", t2, new ArrayList<>(), t1);
    DecisionThread.setWorkflowContext(context);
    DecisionThread.setLatestCurrentExecutionCheckpoint(t2);

    Timeout timeout = Timeout.of(Duration.ofSeconds(1));
    try {
      timeout.call();
      throw new RuntimeException("Expected timeout call to throw StopWorkflowExecution");
    } catch (StopWorkflowExecution e) {
      assertEquals(Duration.ofSeconds(1), e.getWaitForDuration());
    }
  }

  @Test
  public void testTimeoutWhenTimeoutHasExpiredAndCheckoutIsPresent() throws Exception {
    WorkflowContext context = new WorkflowContext("1", t3, new ArrayList<>(), t1);
    DecisionThread.setWorkflowContext(context);
    DecisionThread.setLatestCurrentExecutionCheckpoint(t2);

    Timeout timeout = Timeout.of(Duration.ofSeconds(1));
    timeout.call();
  }
}