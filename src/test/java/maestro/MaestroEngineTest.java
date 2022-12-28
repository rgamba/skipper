package maestro;

import static junit.framework.TestCase.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.inject.Injector;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.val;
import maestro.api.*;
import maestro.common.Anything;
import maestro.models.*;
import maestro.store.OperationStore;
import maestro.store.TimerStore;
import maestro.store.WorkflowInstanceStore;
import maestro.timers.DecisionTimerHandler;
import maestro.timers.OperationRequestTimerHandler;
import maestro.timers.WorkflowInstanceCallbackTimerHandler;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@NotThreadSafe
public class MaestroEngineTest {
  @Mock private WorkflowInstanceStore workflowInstanceStore;
  @Mock private OperationStore operationStore;
  @Mock private TimerStore timerStore;
  @Mock private DecisionExecutor decisionExecutor;
  @Mock private OperationExecutor operationExecutor;
  @Mock private Clock clock;
  @Mock private Injector injector;
  private MaestroEngine engine;

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
  private static final OperationType TEST_OPERATION_TYPE = new OperationType(String.class, "test");

  @Before
  public void init() {
    MockitoAnnotations.openMocks(this);
    this.engine =
        new MaestroEngine(
            clock,
            workflowInstanceStore,
            operationStore,
            timerStore,
            decisionExecutor,
            operationExecutor,
            injector);
    when(clock.instant()).thenReturn(Instant.MIN);
  }

  @Test
  public void testCreateWorkflowInstanceHappyPath() {
    // given
    doNothing().when(workflowInstanceStore).create(any());
    when(clock.instant()).thenReturn(Instant.MIN);
    when(timerStore.createOrUpdate(any())).thenReturn(null);
    val req =
        WorkflowCreationRequest.builder()
            .workflowType(new WorkflowType(MaestroWorkflow.class))
            .arguments(new ArrayList<>())
            .correlationId("test123")
            .build();
    // when
    val response = engine.createWorkflowInstance(req);
    // then
    ArgumentCaptor<Timer> captor = ArgumentCaptor.forClass(Timer.class);
    verify(workflowInstanceStore, times(1)).create(any());
    verify(timerStore, times(1)).createOrUpdate(captor.capture());
    assertEquals(response.getWorkflowInstance().getId(), captor.getValue().getPayload().getValue());
    assertEquals(DecisionTimerHandler.class, captor.getValue().getHandlerClazz());
  }

  @Test
  public void testProcessDecisionWhenNoOperationResponsesAndOperationRequestIsCreated() {
    // given
    val operationType = new OperationType(String.class, "test");
    when(workflowInstanceStore.get(eq(TEST_WORKFLOW_ID))).thenReturn(TEST_WORKFLOW_INSTANCE);
    when(operationStore.getOperationResponses(eq(TEST_WORKFLOW_ID), anyBoolean()))
        .thenReturn(new ArrayList<>());
    val decisionRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(decisionRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(operationType)
            .retryStrategy(OperationExecutor.DEFAULT_RETRY_STRATEGY)
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    val decisionResponse =
        DecisionResponse.builder()
            .operationRequests(
                new ArrayList<OperationRequest>() {
                  {
                    add(operationRequest);
                  }
                })
            .newStatus(WorkflowInstance.Status.ACTIVE)
            .newState(new HashMap<>())
            .build();
    when(decisionExecutor.execute(any(), any())).thenReturn(decisionResponse);
    when(timerStore.createOrUpdate(any())).thenReturn(null);
    when(operationStore.createOperationRequest(any())).thenReturn(true);
    doNothing().when(workflowInstanceStore).update(any(), any(), anyInt());
    // when
    engine.processDecision(TEST_WORKFLOW_ID);
    // then
    assertEquals(TEST_WORKFLOW_ID, WorkflowContext.get().getWorkflowInstanceId());
    ArgumentCaptor<OperationRequest> captor = ArgumentCaptor.forClass(OperationRequest.class);
    verify(operationStore, times(1)).createOperationRequest(captor.capture());
    assertEquals(operationRequest, captor.getValue());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(
        operationRequest.getOperationRequestId(), timerCaptor.getValue().getPayload().getValue());
    assertEquals(OperationRequestTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
  }

  @Test
  public void testProcessDecisionWhenWorkflowWasCompletedAndStateWasUpdated() {
    // given
    when(workflowInstanceStore.get(eq(TEST_WORKFLOW_ID))).thenReturn(TEST_WORKFLOW_INSTANCE);
    when(operationStore.getOperationResponses(eq(TEST_WORKFLOW_ID), anyBoolean()))
        .thenReturn(new ArrayList<>());
    val newStatus =
        new HashMap<String, Anything>() {
          {
            put("foo", new Anything("bar"));
          }
        };
    val decisionResponse =
        DecisionResponse.builder()
            .operationRequests(new ArrayList<>())
            .newStatus(WorkflowInstance.Status.COMPLETED)
            .newState(newStatus)
            .result(new Anything("completed!"))
            .build();
    when(decisionExecutor.execute(any(), any())).thenReturn(decisionResponse);
    when(timerStore.createOrUpdate(any())).thenReturn(null);
    when(operationStore.createOperationRequest(any())).thenReturn(true);
    doNothing().when(workflowInstanceStore).update(any(), any(), anyInt());
    // when
    engine.processDecision(TEST_WORKFLOW_ID);
    // then
    verify(operationStore, times(0)).createOperationRequest(any());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(
        WorkflowInstanceCallbackTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    ArgumentCaptor<WorkflowInstance.Mutation> mutationArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowInstance.Mutation.class);
    verify(workflowInstanceStore, times(1))
        .update(eq(TEST_WORKFLOW_ID), mutationArgumentCaptor.capture(), anyInt());
    assertEquals(newStatus, mutationArgumentCaptor.getValue().getState());
    assertEquals(WorkflowInstance.Status.COMPLETED, mutationArgumentCaptor.getValue().getStatus());
    assertEquals("completed!", mutationArgumentCaptor.getValue().getResult().getValue());
  }

  @Test
  public void testProcessDecisionWhenWorkflowDecisionReturnsAWaitUntil() {
    // given
    when(workflowInstanceStore.get(eq(TEST_WORKFLOW_ID))).thenReturn(TEST_WORKFLOW_INSTANCE);
    when(operationStore.getOperationResponses(eq(TEST_WORKFLOW_ID), anyBoolean()))
        .thenReturn(new ArrayList<>());
    val decisionResponse =
        DecisionResponse.builder()
            .operationRequests(new ArrayList<>())
            .newStatus(WorkflowInstance.Status.ACTIVE)
            .newState(new HashMap<>())
            .waitForDuration(Duration.ofSeconds(10))
            .build();
    when(decisionExecutor.execute(any(), any())).thenReturn(decisionResponse);
    when(timerStore.createOrUpdate(any())).thenReturn(null);
    when(operationStore.createOperationRequest(any())).thenReturn(true);
    doNothing().when(workflowInstanceStore).update(any(), any(), anyInt());
    // when
    engine.processDecision(TEST_WORKFLOW_ID);
    // then
    verify(operationStore, times(0)).createOperationRequest(any());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
    assertEquals(clock.instant().plus(Duration.ofSeconds(10)), timerCaptor.getValue().getTimeout());
  }

  @Test
  public void testProcessOperationRequestWhenResultIsSuccess() {
    // given
    val operationRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(TEST_OPERATION_TYPE)
            .retryStrategy(OperationExecutor.DEFAULT_RETRY_STRATEGY)
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    when(operationStore.createOperationResponse(any())).thenReturn(true);
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    val response =
        OperationExecutionResponse.builder()
            .result(new Anything("op_result"))
            .status(OperationExecutionResponse.Status.COMPLETED)
            .build();
    when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
    // when
    engine.processOperationRequest(operationRequestId);
    // then
    ArgumentCaptor<OperationResponse> responseCaptor =
        ArgumentCaptor.forClass(OperationResponse.class);
    verify(operationStore, times(1)).createOperationResponse(responseCaptor.capture());
    assertEquals(TEST_WORKFLOW_ID, responseCaptor.getValue().getWorkflowInstanceId());
    assertEquals(TEST_OPERATION_TYPE, responseCaptor.getValue().getOperationType());
    assertEquals(0, responseCaptor.getValue().getIteration());
    assertEquals("op_result", responseCaptor.getValue().getResult().getValue());
    assertTrue(responseCaptor.getValue().isSuccess());
    assertFalse(responseCaptor.getValue().isTransient());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
  }

  @Test
  public void testProcessOperationRequestWhenResultIsRetriableError() {
    // given
    val operationRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(TEST_OPERATION_TYPE)
            .retryStrategy(OperationExecutor.DEFAULT_RETRY_STRATEGY)
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    when(operationStore.createOperationResponse(any())).thenReturn(true);
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    val response =
        OperationExecutionResponse.builder()
            .status(OperationExecutionResponse.Status.RETRIABLE_ERROR)
            .error(new Anything(new RuntimeException("error!")))
            .build();
    when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
    // when
    engine.processOperationRequest(operationRequestId);
    // then
    ArgumentCaptor<OperationResponse> responseCaptor =
        ArgumentCaptor.forClass(OperationResponse.class);
    verify(operationStore, times(1)).createOperationResponse(responseCaptor.capture());
    assertFalse(responseCaptor.getValue().isSuccess());
    assertTrue(responseCaptor.getValue().isTransient());
    ArgumentCaptor<OperationRequest> reqCaptor = ArgumentCaptor.forClass(OperationRequest.class);
    verify(operationStore, times(1)).createOperationRequest(reqCaptor.capture());
    assertEquals(TEST_WORKFLOW_ID, reqCaptor.getValue().getWorkflowInstanceId());
    assertEquals(1, reqCaptor.getValue().getFailedAttempts());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(OperationRequestTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    assertEquals(
        reqCaptor.getValue().getOperationRequestId(),
        timerCaptor.getValue().getPayload().getValue());
  }

  @Test
  public void testProcessOperationRequestWhenResultIsRetriableErrorAndRetriesAreExhausted() {
    // given
    val operationRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(TEST_OPERATION_TYPE)
            .retryStrategy(
                FixedRetryStrategy.builder().retryDelay(Duration.ZERO).maxRetries(3).build())
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(2)
            .build();
    when(operationStore.createOperationResponse(any())).thenReturn(true);
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    val response =
        OperationExecutionResponse.builder()
            .status(OperationExecutionResponse.Status.RETRIABLE_ERROR)
            .error(new Anything(new RuntimeException("error!")))
            .build();
    when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
    // when
    engine.processOperationRequest(operationRequestId);
    // then
    ArgumentCaptor<OperationResponse> responseCaptor =
        ArgumentCaptor.forClass(OperationResponse.class);
    verify(operationStore, times(1)).createOperationResponse(responseCaptor.capture());
    assertFalse(responseCaptor.getValue().isSuccess());
    assertFalse(responseCaptor.getValue().isTransient());
    verify(operationStore, times(0)).createOperationRequest(any());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
  }

  @Test
  public void testProcessOperationRequestWhenResultIsNonRetriableError() {
    // given
    val operationRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(TEST_OPERATION_TYPE)
            .retryStrategy(
                FixedRetryStrategy.builder().retryDelay(Duration.ZERO).maxRetries(3).build())
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    when(operationStore.createOperationResponse(any())).thenReturn(true);
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    val response =
        OperationExecutionResponse.builder()
            .status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR)
            .error(new Anything(new RuntimeException("error!")))
            .build();
    when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
    // when
    engine.processOperationRequest(operationRequestId);
    // then
    ArgumentCaptor<OperationResponse> responseCaptor =
        ArgumentCaptor.forClass(OperationResponse.class);
    verify(operationStore, times(1)).createOperationResponse(responseCaptor.capture());
    assertFalse(responseCaptor.getValue().isSuccess());
    assertFalse(responseCaptor.getValue().isTransient());
    verify(operationStore, times(0)).createOperationRequest(any());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
  }

  @Test
  public void testProcessOperationRequestWhenOperationIsWaitTimeout() {
    // given
    val operationRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(new OperationType(WaitTimeout.class, ""))
            .retryStrategy(new NoRetry())
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    when(operationStore.createOperationResponse(any())).thenReturn(true);
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    val response =
        OperationExecutionResponse.builder()
            .status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR)
            .error(new Anything(new WaitTimeout()))
            .build();
    when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
    // when
    engine.processOperationRequest(operationRequestId);
    // then
    ArgumentCaptor<OperationResponse> responseCaptor =
        ArgumentCaptor.forClass(OperationResponse.class);
    verify(operationStore, times(1)).createOperationResponse(responseCaptor.capture());
    assertFalse(responseCaptor.getValue().isSuccess());
    assertFalse(responseCaptor.getValue().isTransient());
    verify(operationStore, times(0)).createOperationRequest(any());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
  }

  @Test
  public void testProcessOperationRequestWhenOperationIsWorkflow() {
    // given
    val operationRequestId = "req-123";
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(
                new OperationType(MaestroWorkflow.class, "test", OperationType.ClazzType.WORKFLOW))
            .retryStrategy(new NoRetry())
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    doNothing().when(workflowInstanceStore).create(any(WorkflowInstance.class));
    // when
    engine.processOperationRequest(operationRequestId);
    // then
    verify(operationStore, times(0)).createOperationResponse(any());
    verify(operationStore, times(0)).createOperationRequest(any());
    ArgumentCaptor<WorkflowInstance> instanceCaptor =
        ArgumentCaptor.forClass(WorkflowInstance.class);
    verify(workflowInstanceStore, times(1)).create(instanceCaptor.capture());
    assertEquals(operationRequestId, instanceCaptor.getValue().getCorrelationId());
    assertEquals(
        ChildWorkflowCallbackHandler.class, instanceCaptor.getValue().getCallbackHandlerClazz());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    assertEquals(instanceCaptor.getValue().getId(), timerCaptor.getValue().getPayload().getValue());
  }

  @Test
  public void testHandleChildWorkflowCompletion() {
    // given
    val operationRequestId = "req-123";
    val newWorkflowId = "childWorkflow123";
    val newWorkflow =
        TEST_WORKFLOW_INSTANCE
            .toBuilder()
            .correlationId(operationRequestId)
            .id(newWorkflowId)
            .callbackHandlerClazz(ChildWorkflowCallbackHandler.class)
            .build();
    val operationRequest =
        OperationRequest.builder()
            .operationRequestId(operationRequestId)
            .workflowInstanceId(TEST_WORKFLOW_ID)
            .operationType(
                new OperationType(MaestroWorkflow.class, "test", OperationType.ClazzType.WORKFLOW))
            .retryStrategy(new NoRetry())
            .creationTime(Instant.MIN)
            .timeout(Duration.ZERO)
            .iteration(0)
            .arguments(new ArrayList<>())
            .failedAttempts(0)
            .build();
    when(workflowInstanceStore.get(eq(newWorkflowId))).thenReturn(newWorkflow);
    when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
    when(operationStore.createOperationResponse(any())).thenReturn(true);
    // when
    engine.handleChildWorkflowCompleted(newWorkflowId);
    // then
    ArgumentCaptor<OperationResponse> responseCaptor =
        ArgumentCaptor.forClass(OperationResponse.class);
    verify(operationStore, times(1)).createOperationResponse(responseCaptor.capture());
    assertEquals(TEST_WORKFLOW_ID, responseCaptor.getValue().getWorkflowInstanceId());
    assertTrue(responseCaptor.getValue().isSuccess());
    assertFalse(responseCaptor.getValue().isTransient());
    assertEquals(MaestroWorkflow.class, responseCaptor.getValue().getOperationType().getClazz());
    assertTrue(responseCaptor.getValue().getOperationType().isWorkflow());
    assertEquals(0, responseCaptor.getValue().getIteration());
    assertEquals("test", responseCaptor.getValue().getOperationType().getMethod());
    assertEquals(newWorkflowId, responseCaptor.getValue().getChildWorkflowInstanceId());

    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    // The workflow decision should be scheduled for the PARENT workflow, not the child
    // workflow.
    assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
  }

  @Test
  public void testExecuteSignalConsumerMethod() {
    val methodName = "testMethod";
    val newState =
        new HashMap<String, Anything>() {
          {
            put("foo", new Anything("bar"));
          }
        };
    when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
    when(decisionExecutor.executeSignalConsumer(
            eq(TEST_WORKFLOW_INSTANCE), any(), eq(methodName), anyList()))
        .thenReturn(newState);
    // when
    engine.executeSignalConsumer(TEST_WORKFLOW_ID, methodName, new ArrayList<>());
    // then
    ArgumentCaptor<WorkflowInstance.Mutation> argumentCaptor =
        ArgumentCaptor.forClass(WorkflowInstance.Mutation.class);
    verify(workflowInstanceStore, times(1))
        .update(eq(TEST_WORKFLOW_ID), argumentCaptor.capture(), anyInt());
    assertEquals(newState, argumentCaptor.getValue().getState());
    ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
    verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
    assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
  }
}