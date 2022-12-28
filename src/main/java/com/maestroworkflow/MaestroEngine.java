package com.maestroworkflow;

import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.maestroworkflow.api.*;
import com.maestroworkflow.models.*;
import com.maestroworkflow.store.OperationStore;
import com.maestroworkflow.store.TimerStore;
import com.maestroworkflow.store.WorkflowInstanceStore;
import com.maestroworkflow.timers.DecisionTimerHandler;
import com.maestroworkflow.timers.OperationRequestTimerHandler;
import com.maestroworkflow.timers.WorkflowInstanceCallbackTimerHandler;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MaestroEngine {
    private static final Logger logger = LoggerFactory.getLogger(MaestroEngine.class);

    private final WorkflowInstanceStore workflowInstanceStore;
    private final OperationStore operationStore;
    private final TimerStore timerStore;
    private final DecisionExecutor decisionExecutor;
    private final OperationExecutor operationExecutor;
    private final Clock clock;
    private final Injector injector;

    public MaestroEngine(Clock clock, @NonNull WorkflowInstanceStore workflowInstanceStore,
                         @NonNull OperationStore operationStore, @NonNull TimerStore timerStore,
                         @NonNull DecisionExecutor decisionExecutor, @NonNull OperationExecutor operationExecutor,
                         @NonNull @Assisted Injector injector) {
        this.workflowInstanceStore = workflowInstanceStore;
        this.operationStore = operationStore;
        this.timerStore = timerStore;
        this.decisionExecutor = decisionExecutor;
        this.operationExecutor = operationExecutor;
        this.clock = clock;
        this.injector = injector;
    }

    public WorkflowCreationResponse createWorkflowInstance(@NonNull WorkflowCreationRequest req) {
        val workflowInstance = WorkflowInstance.builder()
                .id(UUID.randomUUID().toString())
                .correlationId(req.getCorrelationId())
                .status(WorkflowInstance.Status.ACTIVE)
                .version(0)
                .workflowType(req.getWorkflowType())
                .initialArgs(req.getArguments())
                .state(new HashMap<>())
                .callbackHandlerClazz(req.getCallbackHandlerClazz())
                .creationTime(clock.instant())
                .build();
        workflowInstanceStore.create(workflowInstance);
        logger.debug("workflow instance persisted on storage {}", workflowInstance);
        scheduleDecision(workflowInstance.getId());
        return WorkflowCreationResponse.builder().workflowInstance(workflowInstance).build();
    }

    private void scheduleDecision(String workflowInstanceId) {
        scheduleDecision(workflowInstanceId, null);
    }

    private void scheduleDecision(String workflowInstanceId, @Nullable Duration timeout) {
        val timerBuilder = Timer.builder()
                .timerId(String.format("%s-decision", workflowInstanceId))
                .payload(new Anything(String.class, workflowInstanceId))
                .handlerClazz(DecisionTimerHandler.class);
        if (timeout != null) {
            timerBuilder.timeout(clock.instant().plus(timeout));
        }
        val timer = timerBuilder.build();
        timerStore.createOrUpdate(timer);
        logger.debug("scheduled decision. timer={}", timer);
    }

    public void retryFailedWorkflowInstance(String workflowInstanceId) {
        val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
        if (workflowInstance.getStatus() == WorkflowInstance.Status.COMPLETED) {
            throw new IllegalStateException("completed workflow instances cannot be replayed");
        }
        operationStore.convertAllErrorResponsesToTransient(workflowInstance.getId());
        scheduleDecision(workflowInstanceId);
    }

    public void processDecision(@NonNull String workflowInstanceId) {
        val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
        val operationResponses = operationStore.getOperationResponses(workflowInstanceId, false);
        val decisionRequest = DecisionRequest.builder()
                .workflowInstance(workflowInstance).operationResponses(operationResponses).build();

        resetWorkflowContextData(workflowInstance, operationResponses);

        val decisionResponse = decisionExecutor.execute(decisionRequest, this.injector);
        decisionResponse.getOperationRequests().forEach(req -> {
            operationStore.createOperationRequest(req);
            timerStore.createOrUpdate(Timer.builder()
                    .handlerClazz(OperationRequestTimerHandler.class)
                    .payload(Anything.of(req.getOperationRequestId()))
                    // deterministic timer ID to avoid multiple active timers for the same operation request
                    .timerId(req.getOperationRequestId())
                    .build());
        });
        if (decisionResponse.getWaitForDuration() != null) {
            scheduleDecision(workflowInstanceId, decisionResponse.getWaitForDuration());
        }
        logger.info("decision response: {}", decisionResponse);

        val mutation = WorkflowInstance.Mutation.builder()
                .status(decisionResponse.getNewStatus())
                .statusReason(decisionResponse.getStatusReason() != null ? decisionResponse.getStatusReason() : null)
                .state(decisionResponse.getNewState() != null ? decisionResponse.getNewState() : null)
                .result(decisionResponse.getResult() != null ? decisionResponse.getResult() : null)
                .build();
        workflowInstanceStore.update(workflowInstanceId, mutation);

        if (decisionResponse.getNewStatus().isCompleted() || decisionResponse.getNewStatus().isError()) {
            scheduleCallback(workflowInstance);
        }
    }

    private void resetWorkflowContextData(WorkflowInstance workflowInstance, List<OperationResponse> operationResponses) {
        WorkflowContext.set(new WorkflowContext(workflowInstance.getId(), clock.instant(), operationResponses, workflowInstance.getCreationTime()));
        WorkflowContext.removeLatestCurrentExecutionCheckpoint();
    }

    private void scheduleCallback(WorkflowInstance workflowInstance) {
        val timer = Timer.builder().timerId(String.format("%s-callback-%d", workflowInstance.getId(), workflowInstance.getVersion()))
                .handlerClazz(WorkflowInstanceCallbackTimerHandler.class)
                .payload(new Anything(String.class, workflowInstance.getId()))
                .build();
        timerStore.createOrUpdate(timer);
    }

    public void processOperationRequest(@NonNull String operationRequestId) {
        val operationRequest = operationStore.getOperationRequest(operationRequestId);
        val workflowInstance = workflowInstanceStore.get(operationRequest.getWorkflowInstanceId());

        resetWorkflowContextData(workflowInstance, new ArrayList<>());

        if (operationRequest.getOperationType().isWorkflow()) {
            createChildWorkflow(operationRequest);
        } else {
            processRegularOperation(operationRequest, workflowInstance);
        }
    }

    private void processRegularOperation(OperationRequest operationRequest, WorkflowInstance workflowInstance) {
        val response = operationExecutor.execute(operationRequest, this.injector);
        var responseError = response.getError();
        boolean isTransient = false;
        if (response.getStatus().equals(OperationExecutionResponse.Status.RETRIABLE_ERROR)) {
            if (operationRequest.getRetryStrategy().getNextRetryDelay(operationRequest.getFailedAttempts() + 1).isPresent()) {
                isTransient = true;
            } else {
                responseError = new Anything(OperationError.class, new OperationError((Throwable) responseError.getValue()));
            }
        }
        val operationResponse = OperationResponse.builder()
                .operationRequestId(operationRequest.getOperationRequestId())
                .result(response.getResult())
                .isSuccess(!response.isError())
                .operationType(operationRequest.getOperationType())
                .iteration(operationRequest.getIteration())
                .id(UUID.randomUUID().toString())
                .workflowInstanceId(operationRequest.getWorkflowInstanceId())
                .creationTime(clock.instant())
                .isTransient(isTransient)
                .error(responseError)
                .executionDuration(response.getExecutionDuration())
                .build();
        if (!operationStore.createOperationResponse(operationResponse)) {
            // This shouldn't mean an infra error, but rather that no new
            // operation response was persisted because it already existed.
            logger.warn("unable to persist operation response");
        }
        if (isTransient) {
            val delay = operationRequest.getRetryStrategy()
                    .getNextRetryDelay(operationRequest.getFailedAttempts() + 1).get();
            logger.info("transient operation error, scheduling retry number {}", operationRequest.getFailedAttempts() + 1);
            val newOperationRequestId = UUID.randomUUID().toString();
            // Create a new request with incremented failed attempts
            operationStore.createOperationRequest(operationRequest.toBuilder()
                    .operationRequestId(newOperationRequestId)
                    .failedAttempts(operationRequest.getFailedAttempts() + 1)
                    .build());
            timerStore.createOrUpdate(Timer.builder().
                    handlerClazz(OperationRequestTimerHandler.class)
                    .payload(new Anything(String.class, newOperationRequestId))
                    .timerId(UUID.randomUUID().toString())
                    .timeout(clock.instant().plus(delay))
                    .build());
        } else {
            scheduleDecision(operationRequest.getWorkflowInstanceId());
        }
    }

    private void createChildWorkflow(OperationRequest operationRequest) {
        createWorkflowInstance(WorkflowCreationRequest.builder()
                .arguments(operationRequest.getArguments())
                .workflowType(new WorkflowType(operationRequest.getOperationType().getClazz().asSubclass(MaestroWorkflow.class)))
                .correlationId(operationRequest.getOperationRequestId())
                .callbackHandlerClazz(ChildWorkflowCallbackHandler.class)
                .build());
    }

    public void processCallback(@NonNull String workflowInstanceId) {
        val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
        if (workflowInstance.getCallbackHandlerClazz() != null) {
            val handler = this.injector.getInstance(workflowInstance.getCallbackHandlerClazz());
            handler.handleUpdate(workflowInstance, this);
        }
    }

    public WorkflowInstance getWorkflowInstance(String id) {
        return workflowInstanceStore.get(id);
    }

    public List<OperationResponse> getWorkflowInstanceOperationResults(String id) {
        return operationStore.getOperationResponses(id, true);
    }

    public List<WorkflowInstance> findWorkflowInstances() {
        return workflowInstanceStore.find();
    }

    public void executeSignalConsumer(@NonNull String workflowInstanceId, @NonNull String signalMethodName,
                                      @NonNull List<Anything> signalArgs) {
        val workflowInstance = workflowInstanceStore.get(workflowInstanceId);

        resetWorkflowContextData(workflowInstance, new ArrayList<>());

        val newState = decisionExecutor.executeSignalConsumer(workflowInstance, injector, signalMethodName, signalArgs);
        workflowInstanceStore.update(workflowInstanceId, WorkflowInstance.Mutation.builder().state(newState).build());
        scheduleDecision(workflowInstanceId);
    }

    public void handleChildWorkflowCompleted(@NonNull String childWorkflowInstanceId) {
        val workflowInstance = getWorkflowInstance(childWorkflowInstanceId);
        // the correlation ID of the child workflow is the request ID pointing to the operation request associated
        // to the parent workflow.
        val operationRequest = operationStore.getOperationRequest(workflowInstance.getCorrelationId());
        val parentWorkflowInstanceId = operationRequest.getWorkflowInstanceId();
        val operationResponse = OperationResponse.builder()
                .operationRequestId(operationRequest.getOperationRequestId())
                .operationType(operationRequest.getOperationType())
                .result(workflowInstance.getResult())
                .iteration(operationRequest.getIteration())
                .creationTime(clock.instant())
                .isSuccess(true)
                .isTransient(false)
                .executionDuration(Duration.between(workflowInstance.getCreationTime(), clock.instant()))
                .id(UUID.randomUUID().toString())
                .workflowInstanceId(parentWorkflowInstanceId)
                .childWorkflowInstanceId(childWorkflowInstanceId)
                .build();
        if (!operationStore.createOperationResponse(operationResponse)) {
            logger.warn("unable to persist operation response: {}", operationResponse);
        }
        scheduleDecision(operationRequest.getWorkflowInstanceId());
    }

    public void executeWorkflowInstanceCallback(@NonNull String workflowInstanceId) {
        val instance = getWorkflowInstance(workflowInstanceId);
        if (instance.getCallbackHandlerClazz() != null) {
            val handler = injector.getInstance(instance.getCallbackHandlerClazz());
            handler.handleUpdate(instance, this);
        }
    }
}
