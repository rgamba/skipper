package maestro;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Value;
import lombok.val;
import lombok.var;
import maestro.api.DecisionRequest;
import maestro.api.MaestroWorkflow;
import maestro.api.StopWorkflowExecution;
import maestro.api.annotations.WorkflowMethod;
import maestro.common.Anything;
import maestro.models.*;
import org.junit.Before;
import org.junit.Test;

public class DecisionExecutorTest {
  private DecisionExecutor executor;
  private Injector injector;
  private MaestroWorkflow deciderMock;

  private static final String TEST_WORKFLOW_ID = "test-123";
  private static final WorkflowInstance TEST_WORKFLOW_INSTANCE =
      WorkflowInstance.builder()
          .workflowType(new WorkflowType(MaestroWorkflow.class))
          .id(TEST_WORKFLOW_ID)
          .correlationId("corr-test-123")
          .initialArgs(new ArrayList<>())
          .status(WorkflowInstance.Status.ACTIVE)
          .state(new HashMap<>())
          .creationTime(Instant.MIN)
          .build();
  private static final DecisionRequest DECISION_REQUEST =
      DecisionRequest.builder()
          .workflowInstance(TEST_WORKFLOW_INSTANCE)
          .operationResponses(new ArrayList<>())
          .build();

  @Before
  public void setUp() {
    executor = new DecisionExecutor();
    injector = mock(Injector.class);
    deciderMock = mock(MaestroWorkflow.class);
  }

  @Test
  public void testExecuteWhenDeciderCompletesAndReturnIsVoid() {
    when(injector.getInstance(eq(VoidReturnType.class))).thenReturn(new VoidReturnType());
    val req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(VoidReturnType.class))
                    .build())
            .build();
    val response = executor.execute(req, injector);
    assertNull(response.getResult());
    assertEquals(response.getNewStatus(), WorkflowInstance.Status.COMPLETED);
  }

  @Test
  public void testExecuteWhenDeciderHasPrimitiveReturnType() {
    when(injector.getInstance(eq(PrimitiveReturnType.class))).thenReturn(new PrimitiveReturnType());
    val req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(PrimitiveReturnType.class))
                    .build())
            .build();
    val response = executor.execute(req, injector);
    assertEquals(response.getResult().getType().getType(), Integer.class);
    assertEquals(response.getResult().getValue(), 1);
    assertEquals(response.getNewStatus(), WorkflowInstance.Status.COMPLETED);
  }

  @Test
  public void testExecuteWhenDeciderIsProvidedIncorrectArguments() {
    when(injector.getInstance(eq(PrimitiveArgument.class))).thenReturn(new PrimitiveArgument());
    // less args than expected
    var req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(PrimitiveArgument.class))
                    .build())
            .build();
    var result = executor.execute(req, injector);
    assertEquals(result.getNewStatus(), WorkflowInstance.Status.ERROR);
    assertTrue(result.getStatusReason().contains("expected 1 method arguments but got 0"));
    // more args than expected
    List<Anything> args = new ArrayList<>();
    args.add(Anything.of(1));
    args.add(Anything.of(2));
    req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(PrimitiveArgument.class))
                    .initialArgs(args)
                    .build())
            .build();
    result = executor.execute(req, injector);
    assertEquals(result.getNewStatus(), WorkflowInstance.Status.ERROR);
    assertTrue(result.getStatusReason().contains("expected 1 method arguments but got 2"));
    // passing null to a non-nullable arg
    args.clear();
    args.add(null);
    req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(PrimitiveArgument.class))
                    .initialArgs(args)
                    .build())
            .build();
    result = executor.execute(req, injector);
    assertEquals(result.getNewStatus(), WorkflowInstance.Status.ERROR);
    assertTrue(result.getStatusReason().contains("null value provided for non-nullable argument"));
  }

  @Test
  public void testExecuteWhenDeciderTakesInComplexType() {
    when(injector.getInstance(eq(ComplexArgType.class))).thenReturn(new ComplexArgType());
    ArrayList<User> arg1 = new ArrayList<>();
    arg1.add(new User("ricardo"));
    List<Anything> arg = new ArrayList<>();
    arg.add(Anything.of(arg1));
    // less args than expected
    var req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(ComplexArgType.class))
                    .initialArgs(arg)
                    .build())
            .build();
    var result = executor.execute(req, injector);
    assertEquals(result.getNewStatus(), WorkflowInstance.Status.COMPLETED);
    assertEquals(result.getResult().getValue(), "ricardo");
  }

  @Test
  public void testExecuteWhenDeciderReturnsNullValue() {
    when(injector.getInstance(eq(ReturnsNull.class))).thenReturn(new ReturnsNull());
    // less args than expected
    var req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(ReturnsNull.class))
                    .build())
            .build();
    var result = executor.execute(req, injector);
    assertEquals(result.getNewStatus(), WorkflowInstance.Status.COMPLETED);
    assertNull(result.getResult());
  }

  @Test
  public void testExecuteWhenDecisionThrowsStopWorkflowException() {
    when(injector.getInstance(eq(ThrowsStopExecution.class))).thenReturn(new ThrowsStopExecution());
    // less args than expected
    var req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(ThrowsStopExecution.class))
                    .build())
            .build();
    var result = executor.execute(req, injector);
    assertEquals(1, result.getOperationRequests().size());
    assertEquals(WorkflowInstance.Status.ACTIVE, result.getNewStatus());
  }

  @Test
  public void testExecuteWhenDecisionThrowsUnexpectedException() {
    when(injector.getInstance(eq(ThrowsUnexpected.class))).thenReturn(new ThrowsUnexpected());
    // less args than expected
    var req =
        DECISION_REQUEST
            .toBuilder()
            .workflowInstance(
                TEST_WORKFLOW_INSTANCE
                    .toBuilder()
                    .workflowType(new WorkflowType(ThrowsUnexpected.class))
                    .build())
            .build();
    var result = executor.execute(req, injector);
    assertEquals(WorkflowInstance.Status.ERROR, result.getNewStatus());
    assertTrue(result.getStatusReason().contains("unexpected error!"));
  }

  private static class VoidReturnType implements MaestroWorkflow {
    @WorkflowMethod
    public void test() {}
  }

  private static class PrimitiveReturnType implements MaestroWorkflow {
    @WorkflowMethod
    public int test() {
      return 1;
    }
  }

  private static class PrimitiveArgument implements MaestroWorkflow {
    @WorkflowMethod
    public void test(int a) {}
  }

  private static class ComplexArgType implements MaestroWorkflow {
    @WorkflowMethod
    public String test(List<User> a) {
      return a.get(0).getName();
    }
  }

  private static class ReturnsNull implements MaestroWorkflow {
    @WorkflowMethod
    public String test() {
      return null;
    }
  }

  private static class ThrowsStopExecution implements MaestroWorkflow {
    @WorkflowMethod
    public String test() {
      List<OperationRequest> nextOps = new ArrayList<>();
      nextOps.add(
          OperationRequest.builder()
              .workflowInstanceId("wf123")
              .operationRequestId("req1")
              .arguments(new ArrayList<>())
              .creationTime(Instant.now())
              .iteration(1)
              .operationType(new OperationType(String.class, "test"))
              .timeout(Duration.ZERO)
              .retryStrategy(new NoRetry())
              .build());
      throw StopWorkflowExecution.builder().operationRequests(nextOps).build();
    }
  }

  private static class ThrowsUnexpected implements MaestroWorkflow {
    @WorkflowMethod
    public void test() {
      throw new RuntimeException("unexpected error!");
    }
  }

  @Value
  private static class User {
    String name;
  }
}
