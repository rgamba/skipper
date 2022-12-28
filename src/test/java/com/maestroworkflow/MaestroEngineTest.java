package com.maestroworkflow;

import com.google.inject.*;
import com.maestroworkflow.api.*;
import com.maestroworkflow.api.annotations.StateField;
import com.maestroworkflow.api.annotations.WorkflowMethod;
import com.maestroworkflow.models.*;
import com.maestroworkflow.module.MaestroEngineFactory;
import com.maestroworkflow.module.MaestroModule;
import com.maestroworkflow.store.*;
import com.maestroworkflow.timers.DecisionTimerHandler;
import com.maestroworkflow.timers.OperationRequestTimerHandler;
import com.maestroworkflow.timers.WorkflowInstanceCallbackTimerHandler;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MaestroEngineTest {
    private static Logger logger = LoggerFactory.getLogger(MaestroEngineTest.class);

    @Mock private WorkflowInstanceStore workflowInstanceStore;
    @Mock private OperationStore operationStore;
    @Mock private TimerStore timerStore;
    @Mock private DecisionExecutor decisionExecutor;
    @Mock private OperationExecutor operationExecutor;
    @Mock private Clock clock;
    @Mock private Injector injector;
    private MaestroEngine engine;

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
    private static final OperationType TEST_OPERATION_TYPE = new OperationType(String.class, "test");

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        this.engine = new MaestroEngine(clock, workflowInstanceStore, operationStore, timerStore, decisionExecutor, operationExecutor, injector);
        when(clock.instant()).thenReturn(Instant.MIN);
    }

    public static class TestWorkflow implements MaestroWorkflow {
        @StateField
        String defaultName = "test";

        public final GreeterOperation greeter = OperationProxyFactory.create(GreeterOperation.class);

        @WorkflowMethod
        @SneakyThrows
        public String foo(String bar) {
            String response = greeter.greet(bar);
            this.defaultName = bar;
            return response;
        }
    }

    public static class GreeterOperation {
        String greet(String name) {
            return "Hello, " + name + "!";
        }
    }

    @Singleton
    public static class TestCallback implements CallbackHandler {
        public int counter = 0;
        @Override
        public void handleUpdate(@NonNull WorkflowInstance workflowInstance, @NonNull MaestroEngine engine) {
            this.counter += 1;
        }
    }

    @Test
    public void testCreateWorkflow() throws InterruptedException {
        WorkflowContext.set(new WorkflowContext("", Instant.now(), new ArrayList<>(), Instant.MIN));
        Injector injector = Guice.createInjector(new MaestroModule());
        val instanceStore = injector.getInstance(WorkflowInstanceStore.class);
        val operationStore = injector.getInstance(OperationStore.class);
        val timerStore = injector.getInstance(TimerStore.class);
        val engineFactory = injector.getInstance(MaestroEngineFactory.class);
        val engine = engineFactory.create(injector);
        val request = WorkflowCreationRequest.builder()
                .workflowType(new WorkflowType(TestWorkflow.class))
                .correlationId("test-123")
                .arguments(new ArrayList<Anything>() {{
                    add(new Anything(String.class, "Ricardo"));
                }})
                .callbackHandlerClazz(TestCallback.class)
                .build();

        // 1. Create the workflow instance
        val response = engine.createWorkflowInstance(request);
        WorkflowContext.set(new WorkflowContext(response.getWorkflowInstance().getId(), Instant.now(), new ArrayList<>(), Instant.MIN));
        assertEquals("test-123", response.getWorkflowInstance().getCorrelationId());
        assertEquals(response.getWorkflowInstance(), instanceStore.get(response.getWorkflowInstance().getId()));
        Thread.sleep(500);
        // Workflow creation should've queued a decision request in the timers datastore
        var timers = timerStore.getExpiredTimers();
        assertEquals(1, timers.size());

        // The first decision should return an operation request
        // Pop the element from the expired timers
        timerStore.delete(timers.get(0));
        // 2. Process the first decision request
        engine.processDecision(response.getWorkflowInstance().getId());
        // After processing the decision request, the first operation request should've been queued in the timers store.
        Thread.sleep(500);
        var instance = engine.getWorkflowInstance(response.getWorkflowInstance().getId());
        assertEquals(WorkflowInstance.Status.ACTIVE, instance.getStatus());
        assertNull(instance.getResult());
        timers = timerStore.getExpiredTimers();
        assertEquals(1, timers.size());
        var timer = timerStore.get(timers.get(0).getTimerId());
        assertEquals(timer.getHandlerClazz(), OperationRequestTimerHandler.class);

        // 3. Pop the operation request from the timers store and execute it
        timerStore.delete(timer);
        val opRequestId = (String) timer.getPayload().getValue();
        engine.processOperationRequest(opRequestId);
        // Operation request should've scheduled a decision request
        timers = timerStore.getExpiredTimers();
        assertEquals(1, timers.size());
        timer = timerStore.get(timers.get(0).getTimerId());
        assertEquals(timer.getHandlerClazz(), DecisionTimerHandler.class);
        assertEquals(timer.getPayload(), new Anything(String.class, response.getWorkflowInstance().getId()));
        timerStore.delete(timer);

        // Check that the operation response has been persisted as a result of processing the operation request
        val operationResponses = operationStore.getOperationResponses(response.getWorkflowInstance().getId(), true);
        assertEquals(1, operationResponses.size());
        // Now that we have the operation response created, override the workflow context
        WorkflowContext.set(
                new WorkflowContext(response.getWorkflowInstance().getId(), Instant.now(), operationResponses, Instant.MIN));

        // 4. Process the decision request now that the operation response has been persisted. This time around, the
        // workflow should've run to completion so check the response.
        engine.processDecision(response.getWorkflowInstance().getId());
        Thread.sleep(500);
        instance = engine.getWorkflowInstance(response.getWorkflowInstance().getId());
        assertEquals(WorkflowInstance.Status.COMPLETED, instance.getStatus());
        assertNotNull(instance.getResult());
        assertEquals("Hello, Ricardo!", instance.getResult().getValue());

        // 5. After processing the decision and as a result of the workflow being completed, a callback timer should've
        // been created.
        timers = timerStore.getExpiredTimers();
        assertEquals(1, timers.size());
        timer = timerStore.get(timers.get(0).getTimerId());
        assertEquals(timer.getHandlerClazz(), WorkflowInstanceCallbackTimerHandler.class);
        assertEquals(timer.getPayload(), new Anything(String.class, response.getWorkflowInstance().getId()));
        engine.processCallback(response.getWorkflowInstance().getId());
        assertEquals(1, injector.getInstance(TestCallback.class).counter);
    }

    @Test
    public void testCreateWorkflowInstanceHappyPath() {
        //given
        doNothing().when(workflowInstanceStore).create(any());
        when(clock.instant()).thenReturn(Instant.MIN);
        when(timerStore.createOrUpdate(any())).thenReturn(null);
        val req = WorkflowCreationRequest.builder()
                .workflowType(new WorkflowType(MaestroWorkflow.class))
                .arguments(new ArrayList<>())
                .correlationId("test123")
                .build();
        //when
        val response = engine.createWorkflowInstance(req);
        //then
        ArgumentCaptor<Timer> captor = ArgumentCaptor.forClass(Timer.class);
        verify(workflowInstanceStore, times(1)).create(any());
        verify(timerStore, times(1)).createOrUpdate(captor.capture());
        assertEquals(response.getWorkflowInstance().getId(), captor.getValue().getPayload().getValue());
        assertEquals(DecisionTimerHandler.class, captor.getValue().getHandlerClazz());
    }

    @Test
    public void testProcessDecisionWhenNoOperationResponsesAndOperationRequestIsCreated() {
        //given
        val operationType = new OperationType(String.class, "test");
        when(workflowInstanceStore.get(eq(TEST_WORKFLOW_ID))).thenReturn(TEST_WORKFLOW_INSTANCE);
        when(operationStore.getOperationResponses(eq(TEST_WORKFLOW_ID), anyBoolean())).thenReturn(new ArrayList<>());
        val decisionRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
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
        val decisionResponse = DecisionResponse.builder()
                .operationRequests(new ArrayList<OperationRequest>(){{
                    add(operationRequest);
                }})
                .newStatus(WorkflowInstance.Status.ACTIVE)
                .newState(new HashMap<>())
                .build();
        when(decisionExecutor.execute(any(), any())).thenReturn(decisionResponse);
        when(timerStore.createOrUpdate(any())).thenReturn(null);
        when(operationStore.createOperationRequest(any())).thenReturn(true);
        doNothing().when(workflowInstanceStore).update(any(), any());
        //when
        engine.processDecision(TEST_WORKFLOW_ID);
        //then
        assertEquals(TEST_WORKFLOW_ID, WorkflowContext.get().getWorkflowInstanceId());
        ArgumentCaptor<OperationRequest> captor = ArgumentCaptor.forClass(OperationRequest.class);
        verify(operationStore, times(1)).createOperationRequest(captor.capture());
        assertEquals(operationRequest, captor.getValue());
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
        assertEquals(operationRequest.getOperationRequestId(), timerCaptor.getValue().getPayload().getValue());
        assertEquals(OperationRequestTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
    }

    @Test
    public void testProcessDecisionWhenWorkflowWasCompletedAndStateWasUpdated() {
        //given
        when(workflowInstanceStore.get(eq(TEST_WORKFLOW_ID))).thenReturn(TEST_WORKFLOW_INSTANCE);
        when(operationStore.getOperationResponses(eq(TEST_WORKFLOW_ID), anyBoolean())).thenReturn(new ArrayList<>());
        val newStatus = new HashMap<String, Anything>(){{
            put("foo", new Anything("bar"));
        }};
        val decisionResponse = DecisionResponse.builder()
                .operationRequests(new ArrayList<>())
                .newStatus(WorkflowInstance.Status.COMPLETED)
                .newState(newStatus)
                .result(new Anything("completed!"))
                .build();
        when(decisionExecutor.execute(any(), any())).thenReturn(decisionResponse);
        when(timerStore.createOrUpdate(any())).thenReturn(null);
        when(operationStore.createOperationRequest(any())).thenReturn(true);
        doNothing().when(workflowInstanceStore).update(any(), any());
        //when
        engine.processDecision(TEST_WORKFLOW_ID);
        //then
        verify(operationStore, times(0)).createOperationRequest(any());
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
        assertEquals(WorkflowInstanceCallbackTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
        ArgumentCaptor<WorkflowInstance.Mutation> mutationArgumentCaptor = ArgumentCaptor.forClass(WorkflowInstance.Mutation.class);
        verify(workflowInstanceStore, times(1)).update(eq(TEST_WORKFLOW_ID), mutationArgumentCaptor.capture());
        assertEquals(newStatus, mutationArgumentCaptor.getValue().getState());
        assertEquals(WorkflowInstance.Status.COMPLETED, mutationArgumentCaptor.getValue().getStatus());
        assertEquals("completed!", mutationArgumentCaptor.getValue().getResult().getValue());
    }

    @Test
    public void testProcessDecisionWhenWorkflowDecisionReturnsAWaitUntil() {
        //given
        when(workflowInstanceStore.get(eq(TEST_WORKFLOW_ID))).thenReturn(TEST_WORKFLOW_INSTANCE);
        when(operationStore.getOperationResponses(eq(TEST_WORKFLOW_ID), anyBoolean())).thenReturn(new ArrayList<>());
        val decisionResponse = DecisionResponse.builder()
                .operationRequests(new ArrayList<>())
                .newStatus(WorkflowInstance.Status.ACTIVE)
                .newState(new HashMap<>())
                .waitForDuration(Duration.ofSeconds(10))
                .build();
        when(decisionExecutor.execute(any(), any())).thenReturn(decisionResponse);
        when(timerStore.createOrUpdate(any())).thenReturn(null);
        when(operationStore.createOperationRequest(any())).thenReturn(true);
        doNothing().when(workflowInstanceStore).update(any(), any());
        //when
        engine.processDecision(TEST_WORKFLOW_ID);
        //then
        verify(operationStore, times(0)).createOperationRequest(any());
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
        assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
        assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
        assertEquals(clock.instant().plus(Duration.ofSeconds(10)), timerCaptor.getValue().getTimeout());
    }

    @Test
    public void testProcessOperationRequestWhenResultIsSuccess() {
        //given
        val operationRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
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
        val response = OperationExecutionResponse.builder()
                .result(new Anything("op_result"))
                .status(OperationExecutionResponse.Status.COMPLETED)
                .build();
        when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
        //when
        engine.processOperationRequest(operationRequestId);
        //then
        ArgumentCaptor<OperationResponse> responseCaptor = ArgumentCaptor.forClass(OperationResponse.class);
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
        //given
        val operationRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
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
        val response = OperationExecutionResponse.builder()
                .status(OperationExecutionResponse.Status.RETRIABLE_ERROR)
                .error(new Anything(new RuntimeException("error!")))
                .build();
        when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
        //when
        engine.processOperationRequest(operationRequestId);
        //then
        ArgumentCaptor<OperationResponse> responseCaptor = ArgumentCaptor.forClass(OperationResponse.class);
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
        assertEquals(reqCaptor.getValue().getOperationRequestId(), timerCaptor.getValue().getPayload().getValue());
    }

    @Test
    public void testProcessOperationRequestWhenResultIsRetriableErrorAndRetriesAreExhausted() {
        //given
        val operationRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
                .operationRequestId(operationRequestId)
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .operationType(TEST_OPERATION_TYPE)
                .retryStrategy(FixedRetryStrategy.builder().retryDelay(Duration.ZERO).maxRetries(3).build())
                .creationTime(Instant.MIN)
                .timeout(Duration.ZERO)
                .iteration(0)
                .arguments(new ArrayList<>())
                .failedAttempts(2)
                .build();
        when(operationStore.createOperationResponse(any())).thenReturn(true);
        when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
        when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
        val response = OperationExecutionResponse.builder()
                .status(OperationExecutionResponse.Status.RETRIABLE_ERROR)
                .error(new Anything(new RuntimeException("error!")))
                .build();
        when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
        //when
        engine.processOperationRequest(operationRequestId);
        //then
        ArgumentCaptor<OperationResponse> responseCaptor = ArgumentCaptor.forClass(OperationResponse.class);
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
        //given
        val operationRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
                .operationRequestId(operationRequestId)
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .operationType(TEST_OPERATION_TYPE)
                .retryStrategy(FixedRetryStrategy.builder().retryDelay(Duration.ZERO).maxRetries(3).build())
                .creationTime(Instant.MIN)
                .timeout(Duration.ZERO)
                .iteration(0)
                .arguments(new ArrayList<>())
                .failedAttempts(0)
                .build();
        when(operationStore.createOperationResponse(any())).thenReturn(true);
        when(operationStore.getOperationRequest(eq(operationRequestId))).thenReturn(operationRequest);
        when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
        val response = OperationExecutionResponse.builder()
                .status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR)
                .error(new Anything(new RuntimeException("error!")))
                .build();
        when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
        //when
        engine.processOperationRequest(operationRequestId);
        //then
        ArgumentCaptor<OperationResponse> responseCaptor = ArgumentCaptor.forClass(OperationResponse.class);
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
        //given
        val operationRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
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
        val response = OperationExecutionResponse.builder()
                .status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR)
                .error(new Anything(new WaitTimeout()))
                .build();
        when(operationExecutor.execute(eq(operationRequest), any())).thenReturn(response);
        //when
        engine.processOperationRequest(operationRequestId);
        //then
        ArgumentCaptor<OperationResponse> responseCaptor = ArgumentCaptor.forClass(OperationResponse.class);
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
        //given
        val operationRequestId = "req-123";
        val operationRequest = OperationRequest.builder()
                .operationRequestId(operationRequestId)
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .operationType(new OperationType(MaestroWorkflow.class, "test", OperationType.ClazzType.WORKFLOW))
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
        //when
        engine.processOperationRequest(operationRequestId);
        //then
        verify(operationStore, times(0)).createOperationResponse(any());
        verify(operationStore, times(0)).createOperationRequest(any());
        ArgumentCaptor<WorkflowInstance> instanceCaptor = ArgumentCaptor.forClass(WorkflowInstance.class);
        verify(workflowInstanceStore, times(1)).create(instanceCaptor.capture());
        assertEquals(operationRequestId, instanceCaptor.getValue().getCorrelationId());
        assertEquals(ChildWorkflowCallbackHandler.class, instanceCaptor.getValue().getCallbackHandlerClazz());
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
        assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
        assertEquals(instanceCaptor.getValue().getId(), timerCaptor.getValue().getPayload().getValue());
    }

    @Test
    public void testHandleChildWorkflowCompletion() {
        //given
        val operationRequestId = "req-123";
        val newWorkflowId = "childWorkflow123";
        val newWorkflow = TEST_WORKFLOW_INSTANCE.toBuilder()
                .correlationId(operationRequestId)
                .id(newWorkflowId)
                .callbackHandlerClazz(ChildWorkflowCallbackHandler.class)
                .build();
        val operationRequest = OperationRequest.builder()
                .operationRequestId(operationRequestId)
                .workflowInstanceId(TEST_WORKFLOW_ID)
                .operationType(new OperationType(MaestroWorkflow.class, "test", OperationType.ClazzType.WORKFLOW))
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
        //when
        engine.handleChildWorkflowCompleted(newWorkflowId);
        //then
        ArgumentCaptor<OperationResponse> responseCaptor = ArgumentCaptor.forClass(OperationResponse.class);
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
        // The workflow decision should be scheduled for the PARENT workflow, not the child workflow.
        assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
    }

    @Test
    public void testExecuteSignalConsumerMethod() {
        val methodName = "testMethod";
        val newState = new HashMap<String, Anything>(){{
            put("foo", new Anything("bar"));
        }};
        when(workflowInstanceStore.get(TEST_WORKFLOW_ID)).thenReturn(TEST_WORKFLOW_INSTANCE);
        when(decisionExecutor.executeSignalConsumer(eq(TEST_WORKFLOW_INSTANCE), any(), eq(methodName), anyList()))
                .thenReturn(newState);
        //when
        engine.executeSignalConsumer(TEST_WORKFLOW_ID, methodName, new ArrayList<>());
        //then
        ArgumentCaptor<WorkflowInstance.Mutation> argumentCaptor = ArgumentCaptor.forClass(WorkflowInstance.Mutation.class);
        verify(workflowInstanceStore, times(1)).update(eq(TEST_WORKFLOW_ID), argumentCaptor.capture());
        assertEquals(newState, argumentCaptor.getValue().getState());
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(timerStore, times(1)).createOrUpdate(timerCaptor.capture());
        assertEquals(DecisionTimerHandler.class, timerCaptor.getValue().getHandlerClazz());
        assertEquals(TEST_WORKFLOW_ID, timerCaptor.getValue().getPayload().getValue());
    }
}
