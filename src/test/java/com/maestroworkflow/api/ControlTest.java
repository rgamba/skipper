package com.maestroworkflow.api;

import com.maestroworkflow.models.*;
import lombok.val;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class ControlTest {

    private static final String TEST_WORKFLOW_ID = "test-123";
    private static final WorkflowInstance TEST_WORKFLOW_INSTANCE = WorkflowInstance.builder()
            .workflowType(new WorkflowType(MaestroWorkflow.class))
            .id(TEST_WORKFLOW_ID)
            .correlationId("corr-test-123")
            .initialArgs(new ArrayList<>())
            .status(WorkflowInstance.Status.ACTIVE)
            .state(new HashMap<>())
            .creationTime(Instant.MIN)
            .build();
    public static Instant t1 = Instant.MIN;
    public static Instant t2 = t1.plus(1, ChronoUnit.SECONDS);
    public static Instant t3 = t2.plus(1, ChronoUnit.SECONDS);

    @Test
    public void testWaitUntilWhenConditionIsNotMetAndTimeoutHasNotExpired() {
        val context = WorkflowContext.builder()
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .currentTime(t1)
                .workflowInstanceCreationTime(t1)
                .operationResponses(new ArrayList<>())
                .build();
        WorkflowContext.set(context);
        try {
            Control.waitUntil(() -> false, Duration.ofSeconds(1));
            throw new RuntimeException("expected a stop workflow execution exception");
        } catch (StopWorkflowExecution e) {
            assertEquals(Duration.ofSeconds(1), e.getWaitForDuration());
        }
    }

    @Test
    public void testWaitUntilWhenConditionIsNotMetAndTimeoutHasExpired_WhenTimeoutHasNotBeenRecorded_WaitTimeoutOperationIsRaised() {
        val context = WorkflowContext.builder()
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .currentTime(t2)
                .workflowInstanceCreationTime(t1)
                .operationResponses(new ArrayList<>())
                .build();
        WorkflowContext.set(context);
        try {
            Control.waitUntil(() -> false, Duration.ofSeconds(1));
            throw new RuntimeException("expected a stop workflow execution exception");
        } catch (StopWorkflowExecution e) {
            assertEquals(1, e.getOperationRequests().size());
            assertEquals(WaitTimeout.class, e.getOperationRequests().get(0).getOperationType().getClazz());
            assertEquals(TEST_WORKFLOW_ID, e.getOperationRequests().get(0).getWorkflowInstanceId());
        }
    }

    @Test
    public void testWaitUntilWhenConditionIsMetAndTimeoutHasNotExpired() {
        val context = WorkflowContext.builder()
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .currentTime(t1)
                .workflowInstanceCreationTime(t1)
                .operationResponses(new ArrayList<>())
                .build();
        WorkflowContext.set(context);
        Control.waitUntil(() -> true, Duration.ofSeconds(1));
    }

    @Test
    public void testWaitUntilWhenConditionIsMetAndTimeoutHasExpired_ConditionWasMetBeforeTimeout_NothingShouldHappen() {
        val context = WorkflowContext.builder()
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .currentTime(t2)
                .workflowInstanceCreationTime(t1)
                .operationResponses(new ArrayList<>())
                .build();
        WorkflowContext.set(context);
        Control.waitUntil(() -> true, Duration.ofSeconds(1));
    }

    @Test
    public void testWaitUntilWhenConditionIsMetAndTimeoutHasExpired_TimeoutHappenedFirst_TimeoutExceptionShouldBeRaised() {
        val context = WorkflowContext.builder()
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .currentTime(t2)
                .workflowInstanceCreationTime(t1)
                .operationResponses(new ArrayList<OperationResponse>(){{
                    add(OperationResponse.builder()
                            .id(String.format("%s-timeout-0", TEST_WORKFLOW_ID))
                            .operationRequestId("1")
                            .iteration(0)
                            .isTransient(false)
                            .isSuccess(false)
                            .workflowInstanceId(TEST_WORKFLOW_ID)
                            .creationTime(t1)
                            .isSuccess(false)
                            .error(new Anything("error"))
                            .operationType(new OperationType(WaitTimeout.class, "0"))
                            .build());
                }})
                .build();
        WorkflowContext.set(context);
        try {
            Control.waitUntil(() -> true, Duration.ofSeconds(1));
            throw new RuntimeException("expected wait timeout exception");
        } catch (WaitTimeout ignored) {
        }
    }
}
