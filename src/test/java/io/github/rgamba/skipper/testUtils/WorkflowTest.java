package io.github.rgamba.skipper.testUtils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import io.github.rgamba.skipper.WorkflowInspector;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WaitTimeout;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.OperationRequest;
import io.github.rgamba.skipper.models.OperationResponse;
import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.runtime.StopWorkflowExecution;
import io.github.rgamba.skipper.runtime.WorkflowContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import junit.framework.AssertionFailedError;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
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

  protected void assertWorkflowIsInWaitingState(Runnable callable) {
    try {
      callable.run();
      throw new AssertionFailedError("Workflow should've been in waiting state");
    } catch (StopWorkflowExecution e) {
      assertTrue(e.getOperationRequests().isEmpty());
      assertNotNull(e.getWaitForDuration());
    }
  }

  protected void advanceCurrentTimeBy(Duration duration) {
    WorkflowContext context = DecisionThread.getWorkflowContext();
    DecisionThread.setWorkflowContext(
        new WorkflowContext(
            context.getWorkflowInstanceId(),
            context.getCurrentTime().plus(duration),
            context.getOperationResponses(),
            context.getWorkflowInstanceCreationTime()));
  }

  protected void increaseExecutionCheckpoint() {
    if (!DecisionThread.getLatestCurrentExecutionCheckpoint().isPresent()) {
      DecisionThread.setLatestCurrentExecutionCheckpoint(Instant.MIN);
    }
    DecisionThread.setLatestCurrentExecutionCheckpoint(
        DecisionThread.getLatestCurrentExecutionCheckpoint().get().plus(Duration.ofSeconds(1)));
  }

  @SneakyThrows
  protected <T> T expectAndRecordWaitTimeout(Callable<T> callable) {
    try {
      callable.call();
      throw new AssertionFailedError("Workflow should've thrown a WaitTimeout exception");
    } catch (StopWorkflowExecution e) {
      assertEquals(1, e.getOperationRequests().size());
      assertTrue(e.getOperationRequests().get(0).getOperationType().isWaitTimeout());
      // Now, record the wait timeout in the operation responses of the current decision context
      WorkflowContext context = DecisionThread.getWorkflowContext();
      val newResponses = context.getOperationResponses();
      newResponses.add(createWaitTimeoutResponse(e.getOperationRequests().get(0)));
      WorkflowContext newContext =
          new WorkflowContext(
              context.getWorkflowInstanceId(),
              context.getCurrentTime(),
              newResponses,
              context.getWorkflowInstanceCreationTime());
      DecisionThread.setWorkflowContext(newContext);
      // Running the workflow again should no longer throw a StopWorkflowExecution exception but
      // rather
      // throw a WaitTimeout exception. This exception should be caught by the workflow code or
      // bubbled
      // up to the test handle.
      T result = callable.call();
      increaseExecutionCheckpoint();
      return result;
    }
  }

  protected void expectAndRecordWaitTimeout(Runnable runnable) {
    expectAndRecordWaitTimeout(
        () -> {
          runnable.run();
          return null;
        });
  }

  protected OperationResponse createWaitTimeoutResponse(OperationRequest req) {
    return OperationResponse.builder()
        .operationRequestId(req.getOperationRequestId())
        .result(null)
        .isSuccess(false)
        .operationType(req.getOperationType())
        .iteration(req.getIteration())
        .id(UUID.randomUUID().toString())
        .workflowInstanceId(req.getWorkflowInstanceId())
        .creationTime(DecisionThread.getWorkflowContext().getCurrentTime())
        .isTransient(false)
        .error(Anything.of(new WaitTimeout()))
        .executionDuration(Duration.ZERO)
        .build();
  }
}
