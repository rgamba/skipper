package maestro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import lombok.val;
import maestro.api.MaestroWorkflow;
import maestro.api.OperationConfig;
import maestro.api.StopWorkflowExecution;
import maestro.api.WorkflowContext;
import maestro.common.Anything;
import maestro.models.FixedRetryStrategy;
import maestro.models.OperationResponse;
import maestro.models.OperationType;
import org.junit.Test;

public class OperationProxyHandlerTest {

  private static class Foo {
    Integer bar(String a, Integer b) {
      return 1;
    }
  }

  @Test
  public void testInvokeWhenResponseIsNotPresent_StopExecutionIsThrown() throws Throwable {
    Foo proxy = new Foo();
    Method method = proxy.getClass().getDeclaredMethods()[0];
    val responses =
        new ArrayList<OperationResponse>() {
          {
            // Add a response from the same operation but different method. Should be
            // ignored
            add(
                OperationResponse.builder()
                    .operationType(new OperationType(Foo.class, "bar1"))
                    .creationTime(Instant.now())
                    .id("resp001")
                    .isSuccess(true)
                    .isTransient(false)
                    .operationRequestId("req001")
                    .workflowInstanceId("wf123")
                    .iteration(0)
                    .build());
            // Add a response from the same operation AND method but different
            // iteration. Should be ignored
            add(
                OperationResponse.builder()
                    .operationType(new OperationType(Foo.class, "bar"))
                    .creationTime(Instant.now())
                    .id("resp002")
                    .isSuccess(true)
                    .isTransient(false)
                    .operationRequestId("req002")
                    .workflowInstanceId("wf123")
                    .iteration(3)
                    .build());
            // Add a response from the same operation AND method AND iteration but the
            // result
            // is a transient error so SHOULD be ignored.
            add(
                OperationResponse.builder()
                    .operationType(new OperationType(Foo.class, "bar"))
                    .creationTime(Instant.now())
                    .id("resp002")
                    .isSuccess(false)
                    .isTransient(true)
                    .operationRequestId("req002")
                    .workflowInstanceId("wf123")
                    .iteration(0)
                    .error(new Anything(new IllegalArgumentException("error")))
                    .build());
          }
        };

    val handler =
        new OperationProxyHandler(
            responses,
            new WorkflowContext("", Instant.now(), responses, Instant.MIN),
            Foo.class,
            null,
            false);
    try {
      handler.invoke(proxy, method, null, new Object[] {"a", 1});
      throw new Exception("Expected invoke to throw StopWorkflowExecution");
    } catch (StopWorkflowExecution e) {
      assertEquals(1, e.getOperationRequests().size());
      val req = e.getOperationRequests().get(0);
      assertEquals(Foo.class, req.getOperationType().getClazz());
      assertEquals("bar", req.getOperationType().getMethod());
      assertEquals(0, req.getIteration());
      assertEquals(2, req.getArguments().size());
      assertEquals(new Anything(String.class, "a"), req.getArguments().get(0));
      assertEquals(new Anything(Integer.class, 1), req.getArguments().get(1));
      assertEquals(OperationExecutor.DEFAULT_RETRY_STRATEGY, req.getRetryStrategy());
    }
  }

  @Test
  public void testInvokeSubstitutesIdempotencyTokenPlaceHolder() throws Throwable {
    Foo proxy = new Foo();
    Method method = proxy.getClass().getDeclaredMethods()[0];
    val responses = new ArrayList<OperationResponse>();
    val handler =
        new OperationProxyHandler(
            responses,
            new WorkflowContext("", Instant.now(), responses, Instant.MIN),
            Foo.class,
            null,
            false);
    try {
      handler.invoke(
          proxy, method, null, new Object[] {MaestroWorkflow.IDEMPOTENCY_TOKEN_PLACEHOLDER, 1});
      throw new Exception("Expected invoke to throw StopWorkflowExecution");
    } catch (StopWorkflowExecution e) {
      assertEquals(1, e.getOperationRequests().size());
      val req = e.getOperationRequests().get(0);
      assertEquals(2, req.getArguments().size());
      assertEquals(req.getArguments().get(0).getValue(), req.generateIdempotencyToken());
    }
  }

  @Test
  public void testInvokeWhenResponseIsNotPresentAndCustomRetryStrategy_StopExecutionIsThrown()
      throws Throwable {
    Foo proxy = new Foo();
    Method method = proxy.getClass().getDeclaredMethods()[0];
    val responses = new ArrayList<OperationResponse>();
    val config =
        OperationConfig.builder()
            .retryStrategy(
                FixedRetryStrategy.builder().maxRetries(10).retryDelay(Duration.ZERO).build())
            .build();
    val handler =
        new OperationProxyHandler(
            responses,
            new WorkflowContext("", Instant.now(), responses, Instant.MIN),
            Foo.class,
            config,
            false);
    try {
      handler.invoke(proxy, method, null, new Object[] {"a", 1});
      throw new Exception("Expected invoke to throw StopWorkflowExecution");
    } catch (StopWorkflowExecution e) {
      val req = e.getOperationRequests().get(0);
      assertEquals(config.getRetryStrategy(), req.getRetryStrategy());
    }
  }

  @Test
  public void testInvokeWhenResponseIsPresentAndNotError_ValueIsReturned() throws Throwable {
    Foo proxy = new Foo();
    Method method = proxy.getClass().getDeclaredMethods()[0];
    val responses =
        new ArrayList<OperationResponse>() {
          {
            // Add a response from the same operation AND method but different
            // iteration. Should be ignored
            add(
                OperationResponse.builder()
                    .operationType(new OperationType(Foo.class, "bar"))
                    .creationTime(Instant.now())
                    .id("resp002")
                    .isSuccess(true)
                    .isTransient(false)
                    .operationRequestId("req002")
                    .workflowInstanceId("wf123")
                    .iteration(0)
                    .result(new Anything(Integer.class, 123))
                    .build());
          }
        };
    val handler =
        new OperationProxyHandler(
            responses,
            new WorkflowContext("", Instant.now(), responses, Instant.MIN),
            Foo.class,
            null,
            false);
    Object result = handler.invoke(proxy, method, null, new Object[] {"a", 1});
    assertEquals(123, result);
    assertEquals(Integer.class, result.getClass());
  }

  @Test
  public void testInvokeWhenResponseIsPresentAndResultIsNull_NullValueIsReturned()
      throws Throwable {
    Foo proxy = new Foo();
    Method method = proxy.getClass().getDeclaredMethods()[0];
    val responses =
        new ArrayList<OperationResponse>() {
          {
            // Add a response from the same operation AND method but different
            // iteration. Should be ignored
            add(
                OperationResponse.builder()
                    .operationType(new OperationType(Foo.class, "bar"))
                    .creationTime(Instant.now())
                    .id("resp002")
                    .isSuccess(true)
                    .isTransient(false)
                    .operationRequestId("req002")
                    .workflowInstanceId("wf123")
                    .iteration(0)
                    .result(null)
                    .build());
          }
        };

    val handler =
        new OperationProxyHandler(
            responses,
            new WorkflowContext("", Instant.now(), responses, Instant.MIN),
            Foo.class,
            null,
            false);
    Object result = handler.invoke(proxy, method, null, new Object[] {"a", 1});
    assertNull(result);
  }

  @Test
  public void testInvokeWhenResponseIsPresentAndIsError_ExceptionIsRaised() throws Throwable {
    Foo proxy = new Foo();
    Method method = proxy.getClass().getDeclaredMethods()[0];
    val responses =
        new ArrayList<OperationResponse>() {
          {
            // Add a response from the same operation AND method but different
            // iteration. Should be ignored
            add(
                OperationResponse.builder()
                    .operationType(new OperationType(Foo.class, "bar"))
                    .creationTime(Instant.now())
                    .id("resp002")
                    .isSuccess(false)
                    .isTransient(false)
                    .operationRequestId("req002")
                    .workflowInstanceId("wf123")
                    .iteration(0)
                    .error(
                        new Anything(
                            IllegalArgumentException.class, new IllegalArgumentException("error!")))
                    .build());
          }
        };

    val handler =
        new OperationProxyHandler(
            responses,
            new WorkflowContext("", Instant.now(), responses, Instant.MIN),
            Foo.class,
            null,
            false);
    try {
      handler.invoke(proxy, method, null, new Object[] {"a", 1});
      throw new Exception("Expected an IllegalArgumentException to be raised");
    } catch (IllegalArgumentException e) {
      assertEquals("error!", e.getMessage());
    }
  }
}
