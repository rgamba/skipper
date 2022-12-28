package io.github.rgamba.skipper;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import io.github.rgamba.skipper.api.OperationExecutionResponse;
import io.github.rgamba.skipper.api.WaitTimeout;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.NoRetry;
import io.github.rgamba.skipper.models.OperationRequest;
import io.github.rgamba.skipper.models.OperationType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

public class OperationExecutorTest {

  private OperationExecutor executor;
  private DependencyRegistry registry;

  public static class TestModule extends AbstractModule {}

  public static class Foo {
    public int bar(String a, Integer b) throws FooError {
      return b;
    }

    public void voidMethod() {}

    public void throwsUncheckedException() {
      throw new FooError("runtime_error");
    }

    public void throwsUncheckedCommonException() {
      throw new IllegalArgumentException("runtime_error");
    }

    public void throwsCheckedException() throws FooError {
      throw new FooError("runtime_error");
    }

    public void doesntTakeNulls(@NonNull String a) {}

    public String listArgument(@NonNull List<String> a) {
      return a.get(0);
    }

    public Optional<User> complexReturn() {
      return Optional.of(new User("test"));
    }

    public String returnsNull() {
      return null;
    }
  }

  private static class FooError extends RuntimeException {
    public FooError(String error) {
      super(error);
    }
  }

  @Value
  private static class User {
    String name;
  }

  @Before
  public void setUp() {
    executor = new OperationExecutor();
    registry = mock(DependencyRegistry.class);
    when(registry.getOperation(eq(Foo.class))).thenReturn(new Foo());
  }

  @Test
  public void testExecuteOperation_HappyPath() {
    List<Anything> args = new ArrayList<>();
    args.add(Anything.of("hello"));
    args.add(Anything.of(123));
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "bar"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.COMPLETED, response.getStatus());
    assertEquals(123, response.getResult().getValue());
  }

  @Test
  public void testExecuteOperation_WhenMethodReturnsNull_ResultInResponseIsNull() {
    List<Anything> args = new ArrayList<>();
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "returnsNull"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.COMPLETED, response.getStatus());
    assertNull(response.getResult());
  }

  @Test
  public void testExecuteOperation_WhenMethodNameIsInvalid_IllegalArgumentIsThrown() {
    List<Anything> args = new ArrayList<>();
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "invalidName"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val error = assertThrows(IllegalArgumentException.class, () -> executor.execute(req, registry));
    assertTrue(error.getMessage().contains("does not have a method invalidName"));
  }

  @Test
  public void testExecuteOperation_WhenArgsDontMatchMethodParamType() {
    List<Anything> args = new ArrayList<>();
    args.add(Anything.of("hello"));
    args.add(Anything.of("string")); // This should be an integer
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "bar"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val result = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR, result.getStatus());
    assertTrue(
        result
            .getError()
            .getValue()
            .toString()
            .contains(
                "arg1 with type class java.lang.String when the expected type is class java.lang.Integer"));
  }

  @Test
  public void testExecuteOperation_WhenListArgument() {
    List<Anything> args = new ArrayList<>();
    List<String> arg1 = new ArrayList<>();
    arg1.add("test");
    args.add(Anything.of(arg1));
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "listArgument"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.COMPLETED, response.getStatus());
    assertEquals("test", response.getResult().getValue());
  }

  @Test
  public void testExecuteOperation_WhenReturnIsComplexType() {
    List<Anything> args = new ArrayList<>();
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "complexReturn"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.COMPLETED, response.getStatus());
    assertEquals(Optional.of(new User("test")), response.getResult().getValue());
  }

  @Test
  public void testExecuteOperation_WhenNullableArgIsPassedIn() {
    List<Anything> args = new ArrayList<>();
    args.add(null);
    args.add(Anything.of(123));
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "bar"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.COMPLETED, response.getStatus());
    assertEquals(123, response.getResult().getValue());
  }

  @Test
  public void testExecuteOperation_WhenIncorrectArgumentNumberIsPassedIn() {
    List<Anything> args = new ArrayList<>();
    args.add(null);
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "bar"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val result = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR, result.getStatus());
    assertEquals(IllegalArgumentException.class, result.getError().getClazz());
    assertTrue(
        result.getError().getValue().toString().contains("expected 2 method arguments but got 1"));
  }

  @Test
  public void testExecuteOperation_WhenNullableArgIsPassedInButParamIsNotNullable() {
    List<Anything> args = new ArrayList<>();
    args.add(null);
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "doesntTakeNulls"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR, response.getStatus());
  }

  @Test
  public void testExecuteOperationWithVoidMethod() {
    List<Anything> args = new ArrayList<>();
    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "voidMethod"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.COMPLETED, response.getStatus());
    assertNull(response.getResult());
  }

  @Test
  public void
      testExecuteOperation_WhenNonCheckedCommonNonRetriableExceptionIsThrown_RetriableErrorIsReturned() {
    List<Anything> args = new ArrayList<>();

    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "throwsUncheckedCommonException"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR, response.getStatus());
    assertNull(response.getResult());
    assertEquals(response.getError().getType().getType(), IllegalArgumentException.class);
  }

  @Test
  public void testExecuteOperation_WhenNonCheckedExceptionIsThrown_RetriableErrorIsReturned() {
    List<Anything> args = new ArrayList<>();

    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "throwsUncheckedException"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.RETRIABLE_ERROR, response.getStatus());
    assertNull(response.getResult());
    assertEquals(response.getError().getType().getType(), FooError.class);
  }

  @Test
  public void testExecuteOperation_WhenCheckedExceptionIsThrown_NonRetriableErrorIsThrown() {
    List<Anything> args = new ArrayList<>();

    OperationRequest req =
        OperationRequest.builder()
            .operationType(new OperationType(Foo.class, "throwsCheckedException"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR, response.getStatus());
    assertNull(response.getResult());
    assertEquals(response.getError().getType().getType(), FooError.class);
  }

  @Test
  public void testExecuteOperation_WhenIsWaitTimeout_NonRetriableErrorIsReturned() {
    List<Anything> args = new ArrayList<>();

    OperationRequest req =
        OperationRequest.builder()
            .operationType(OperationType.waitTimeout("1"))
            .operationRequestId("req123")
            .workflowInstanceId("wf123")
            .iteration(0)
            .arguments(args)
            .creationTime(Instant.now())
            .retryStrategy(new NoRetry())
            .timeout(Duration.ZERO)
            .failedAttempts(0)
            .build();

    val response = executor.execute(req, registry);
    assertEquals(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR, response.getStatus());
    assertNull(response.getResult());
    assertEquals(response.getError().getType().getType(), WaitTimeout.class);
  }
}
