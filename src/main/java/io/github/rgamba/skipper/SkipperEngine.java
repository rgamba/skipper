package io.github.rgamba.skipper;

import com.google.inject.assistedinject.Assisted;
import io.github.rgamba.skipper.api.*;
import io.github.rgamba.skipper.api.annotations.SignalConsumer;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.*;
import io.github.rgamba.skipper.models.Timer;
import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.store.OperationStore;
import io.github.rgamba.skipper.store.StorageError;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.store.WorkflowInstanceStore;
import io.github.rgamba.skipper.timers.DecisionTimerHandler;
import io.github.rgamba.skipper.timers.OperationRequestTimerHandler;
import io.github.rgamba.skipper.timers.WorkflowInstanceCallbackTimerHandler;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

/**
 * SkipperEngine is the facade to the workflow engine.
 *
 * <p>All interactions with workflow instances, workflow management, etc. will be done through this
 * facade.
 */
@Slf4j
public class SkipperEngine {
  private final WorkflowInstanceStore workflowInstanceStore;
  private final OperationStore operationStore;
  private final TimerStore timerStore;
  private final DecisionExecutor decisionExecutor;
  private final OperationExecutor operationExecutor;
  private final Clock clock;
  private final DependencyRegistry registry;

  public SkipperEngine(
      Clock clock,
      @NonNull WorkflowInstanceStore workflowInstanceStore,
      @NonNull OperationStore operationStore,
      @NonNull TimerStore timerStore,
      @NonNull DecisionExecutor decisionExecutor,
      @NonNull OperationExecutor operationExecutor,
      @NonNull @Assisted DependencyRegistry registry) {
    this.workflowInstanceStore = workflowInstanceStore;
    this.operationStore = operationStore;
    this.timerStore = timerStore;
    this.decisionExecutor = decisionExecutor;
    this.operationExecutor = operationExecutor;
    this.clock = clock;
    this.registry = registry;
  }

  /**
   * Create a new workflow instance
   *
   * @param req The workflow instance creation request
   * @return The workflow creation response containing the workflow instance created. This method
   *     will throw runtime exceptions in case invalid input was provided.
   */
  public WorkflowCreationResponse createWorkflowInstance(@NonNull WorkflowCreationRequest req) {
    val workflowInstance =
        WorkflowInstance.builder()
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
    log.debug("workflow instance persisted on storage {}", workflowInstance);
    scheduleDecision(workflowInstance.getId());
    Metrics.WORKFLOW_INSTANCE_CREATION_COUNT.mark();
    return WorkflowCreationResponse.builder().workflowInstance(workflowInstance).build();
  }

  private void scheduleDecision(String workflowInstanceId) {
    scheduleDecision(workflowInstanceId, null);
  }

  private void scheduleDecision(String workflowInstanceId, @Nullable Duration timeout) {
    val timerBuilder =
        Timer.builder()
            .timerId(String.format("%s-decision", workflowInstanceId))
            .payload(new Anything(String.class, workflowInstanceId))
            .handlerClazz(DecisionTimerHandler.class);
    if (timeout != null) {
      timerBuilder.timeout(clock.instant().plus(timeout));
    }
    val timer = timerBuilder.build();
    timerStore.createOrUpdate(timer);
    log.debug("scheduled decision. timer={}", timer);
  }

  /**
   * Retry a workflow instance whose status is ERROR.
   *
   * <p>In case the workflow is on a terminal state (COMPLETED), this method will fail with an
   * exception, otherwise it will convert all operation responses to transient and will schedule a
   * workflow decision.
   *
   * @param workflowInstanceId The workflow instance ID
   */
  public void retryFailedWorkflowInstance(String workflowInstanceId) {
    val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
    if (workflowInstance.getStatus() == WorkflowInstance.Status.COMPLETED) {
      throw new IllegalStateException("completed workflow instances cannot be replayed");
    }
    operationStore.convertAllErrorResponsesToTransient(workflowInstance.getId());
    scheduleDecision(workflowInstanceId);
  }

  /**
   * Process a decision request for the given workflow.
   *
   * @param workflowInstanceId The workflow instance ID.
   */
  public void processDecision(@NonNull String workflowInstanceId) {
    val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
    val operationResponses = operationStore.getOperationResponses(workflowInstanceId, false);
    val decisionRequest =
        DecisionRequest.builder()
            .workflowInstance(workflowInstance)
            .operationResponses(operationResponses)
            .build();

    resetWorkflowContextData(workflowInstance, operationResponses);

    val decisionResponse = decisionExecutor.execute(decisionRequest, registry);
    decisionResponse
        .getOperationRequests()
        .forEach(
            req -> {
              try {
                operationStore.createOperationRequest(req);
              } catch (StorageError e) {
                if (e.getType().equals(StorageError.Type.DUPLICATE_ENTRY)) {
                  log.warn(
                      "unable to persist operation request with id '{}' since it already existed: {}",
                      req.getOperationRequestId(),
                      e.getMessage());
                } else {
                  throw e;
                }
              }
              timerStore.createOrUpdate(
                  Timer.builder()
                      .handlerClazz(OperationRequestTimerHandler.class)
                      .payload(Anything.of(req.getOperationRequestId()))
                      // deterministic timer ID to avoid multiple active
                      // timers for the same operation request
                      .timerId(req.getOperationRequestId())
                      .build());
            });
    if (decisionResponse.getWaitForDuration() != null) {
      scheduleDecision(workflowInstanceId, decisionResponse.getWaitForDuration());
    }
    log.info("decision response: {}", decisionResponse);

    if (workflowInstanceWasUpdated(workflowInstance, decisionResponse)) {
      val mutation =
          WorkflowInstance.Mutation.builder()
              .status(decisionResponse.getNewStatus())
              .statusReason(
                  decisionResponse.getStatusReason() != null
                      ? decisionResponse.getStatusReason()
                      : null)
              .state(decisionResponse.getNewState() != null ? decisionResponse.getNewState() : null)
              .result(decisionResponse.getResult() != null ? decisionResponse.getResult() : null)
              .build();
      workflowInstanceStore.update(workflowInstanceId, mutation, workflowInstance.getVersion());
    }
    if (!decisionResponse.getInlineExecutions().isEmpty()) {
      decisionResponse
          .getInlineExecutions()
          .forEach(
              exe ->
                  operationStore.createOperationRequestAndResponse(
                      exe.getRequest(), exe.getResponse()));
    }

    if (decisionResponse.getNewStatus().isCompleted()
        || decisionResponse.getNewStatus().isError()) {
      scheduleCallback(workflowInstance);
    }
  }

  private boolean workflowInstanceWasUpdated(
      WorkflowInstance workflowInstance, DecisionResponse decisionResponse) {
    boolean statusChanged = decisionResponse.getNewStatus() != workflowInstance.getStatus();
    boolean resultChanged =
        !Objects.equals(decisionResponse.getResult(), workflowInstance.getResult());
    boolean stateChanged =
        decisionResponse.getNewState() != null
            && !Objects.equals(decisionResponse.getNewState(), workflowInstance.getState());
    return statusChanged || resultChanged || stateChanged;
  }

  private void resetWorkflowContextData(
      WorkflowInstance workflowInstance, List<OperationResponse> operationResponses) {
    DecisionThread.contextSetter()
        .workflowInstance(workflowInstance)
        .currentTime(clock.instant())
        .operationResponses(operationResponses)
        .set();
  }

  private void scheduleCallback(WorkflowInstance workflowInstance) {
    val timer =
        Timer.builder()
            .timerId(
                String.format(
                    "%s-callback-%d", workflowInstance.getId(), workflowInstance.getVersion()))
            .handlerClazz(WorkflowInstanceCallbackTimerHandler.class)
            .payload(new Anything(String.class, workflowInstance.getId()))
            .build();
    timerStore.createOrUpdate(timer);
  }

  /**
   * Process an operation request.
   *
   * <p>This method actually triggers the execution of the operation but the operation execution
   * might or might not be completed by the time this function returns.
   *
   * @param operationRequestId The operation request ID.
   * @throws IllegalArgumentException in case the operation request ID provided is invalid
   */
  public void processOperationRequest(@NonNull String operationRequestId) {
    val operationRequest = operationStore.getOperationRequest(operationRequestId);
    val workflowInstance = workflowInstanceStore.get(operationRequest.getWorkflowInstanceId());

    if (operationRequest.getOperationType().isWorkflow()) {
      createChildWorkflow(operationRequest);
    } else {
      processRegularOperation(operationRequest, workflowInstance);
    }
  }

  private void processRegularOperation(
      OperationRequest operationRequest, WorkflowInstance workflowInstance) {
    val response = operationExecutor.execute(operationRequest, registry);
    var responseError = response.getError();
    boolean isTransient = false;
    if (response.getStatus().equals(OperationExecutionResponse.Status.RETRIABLE_ERROR)) {
      if (operationRequest
          .getRetryStrategy()
          .getNextRetryDelay(operationRequest.getFailedAttempts() + 1)
          .isPresent()) {
        isTransient = true;
      } else {
        responseError =
            new Anything(
                OperationError.class, new OperationError((Throwable) responseError.getValue()));
      }
    }
    val operationResponse =
        OperationResponse.builder()
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
      log.warn("unable to persist operation response: {}", operationResponse);
    }
    if (isTransient) {
      val delay =
          operationRequest
              .getRetryStrategy()
              .getNextRetryDelay(operationRequest.getFailedAttempts() + 1)
              .get();
      log.info(
          "transient operation error, scheduling retry number {}",
          operationRequest.getFailedAttempts() + 1);
      operationStore.incrementOperationRequestFailedAttempts(
          operationRequest.getOperationRequestId(), operationRequest.getFailedAttempts());
      timerStore.createOrUpdate(
          Timer.builder()
              .handlerClazz(OperationRequestTimerHandler.class)
              .payload(new Anything(String.class, operationRequest.getOperationRequestId()))
              .timerId(UUID.randomUUID().toString())
              .timeout(clock.instant().plus(delay))
              .build());
    } else {
      scheduleDecision(operationRequest.getWorkflowInstanceId());
    }
  }

  private void createChildWorkflow(OperationRequest operationRequest) {
    try {
      createWorkflowInstance(
          WorkflowCreationRequest.builder()
              .arguments(operationRequest.getArguments())
              .workflowType(
                  new WorkflowType(
                      operationRequest
                          .getOperationType()
                          .getClazz()
                          .asSubclass(SkipperWorkflow.class)))
              .correlationId(operationRequest.getOperationRequestId())
              .callbackHandlerClazz(ChildWorkflowCallbackHandler.class)
              .build());
    } catch (StorageError e) {
      if (e.getType() == StorageError.Type.DUPLICATE_ENTRY) {
        log.warn(
            "unable to create child workflow with correlationId={}, duplicate error was thrown",
            operationRequest.getOperationRequestId());
      }
    }
  }

  /**
   * Process a callback request for the given workflow instance.
   *
   * @param workflowInstanceId The workflow instance ID.
   * @throws IllegalArgumentException in case the workflow instance ID provided is invalid.
   */
  public void processCallback(@NonNull String workflowInstanceId) {
    val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
    if (workflowInstance.getCallbackHandlerClazz() != null) {
      val handler = registry.getCallbackHandler(workflowInstance.getCallbackHandlerClazz());
      handler.handleUpdate(workflowInstance, this);
    }
  }

  /**
   * Get the workflow instance
   *
   * @param id The workflow instance ID.
   * @return The workflow instance
   * @throws IllegalArgumentException In case the workflow ID provided is invalid.
   */
  public WorkflowInstance getWorkflowInstance(String id) {
    return workflowInstanceStore.get(id);
  }

  public Optional<WorkflowInstance> getWorkflowInstanceByCorrelationId(String correlationId) {
    // TODO: implement a proper find by correlation ID here.
    return workflowInstanceStore.find().stream()
        .filter(wf -> wf.getCorrelationId().equals(correlationId))
        .findFirst();
  }

  /**
   * Get the operation results for the given workflow instance
   *
   * @param workflowInstanceId The workflow instance ID
   * @return The list of operation responses
   * @throws IllegalArgumentException In case the workflow instance ID provided is invalid
   */
  public List<OperationResponse> getWorkflowInstanceOperationResults(
      @NonNull String workflowInstanceId) {
    return operationStore.getOperationResponses(workflowInstanceId, true);
  }

  /**
   * Get all operation requests for the given workflow instance
   *
   * @param workflowInstanceId The workflow instance ID
   * @return A list of operation requests associated with the workflow
   * @throws IllegalArgumentException in case the workflow instance ID is invalid
   */
  public List<OperationRequest> getWorkflowInstanceOperationRequests(
      @NonNull String workflowInstanceId) {
    return operationStore.getOperationRequests(workflowInstanceId);
  }

  public List<WorkflowInstance> findWorkflowInstances() {
    return workflowInstanceStore.find();
  }

  /**
   * Executes a workflow signal consumer
   *
   * @param workflowInstanceId The workflow instance ID.
   * @param signalMethodName The signal method name. This must match a method name on the workflow
   *     class and that method must be annotated with {@link SignalConsumer}
   * @param signalArgs The list of arguments to be passed into the signal method. The parameter
   *     types must match the signalMethod signature otherwise the request will fail. The arguments
   *     will be passed into the signal method in the same order they are provided.
   * @throws IllegalArgumentException In case The workflow instance ID is not valid, the
   *     signalMethod does not exist or if the signalArgs are invalid or don't match the
   *     signalMethod signature.
   */
  public void executeSignalConsumer(
      @NonNull String workflowInstanceId,
      @NonNull String signalMethodName,
      @NonNull List<Anything> signalArgs) {
    val workflowInstance = workflowInstanceStore.get(workflowInstanceId);
    if (workflowInstance.getStatus().isCompleted()) {
      throw new IllegalStateException(
          "sending input signals to a completed workflow is not allowed");
    }
    // Even though signals run in the thread context, they don't access operation results, so we
    // just set the basic context but leave the operation results empty.
    resetWorkflowContextData(workflowInstance, new ArrayList<>());

    val newState =
        decisionExecutor.executeSignalConsumer(
            workflowInstance, registry, signalMethodName, signalArgs);
    workflowInstanceStore.update(
        workflowInstanceId,
        WorkflowInstance.Mutation.builder().state(newState).build(),
        workflowInstance.getVersion());
    scheduleDecision(workflowInstanceId);
  }

  /**
   * Handle the completion of a child workflow instance.
   *
   * @param childWorkflowInstanceId The workflow ID of the CHILD workflow instance.
   * @throws IllegalArgumentException In case the workflow instance ID is invalid.
   */
  public void handleChildWorkflowCompleted(@NonNull String childWorkflowInstanceId) {
    val workflowInstance = getWorkflowInstance(childWorkflowInstanceId);
    // the correlation ID of the child workflow is the request ID pointing to the operation
    // request associated
    // to the parent workflow.
    val operationRequest = operationStore.getOperationRequest(workflowInstance.getCorrelationId());
    val parentWorkflowInstanceId = operationRequest.getWorkflowInstanceId();
    val operationResponse =
        OperationResponse.builder()
            .operationRequestId(operationRequest.getOperationRequestId())
            .operationType(operationRequest.getOperationType())
            .result(workflowInstance.getResult())
            .iteration(operationRequest.getIteration())
            .creationTime(clock.instant())
            .isSuccess(true)
            .isTransient(false)
            .executionDuration(
                Duration.between(workflowInstance.getCreationTime(), clock.instant()))
            .id(UUID.randomUUID().toString())
            .workflowInstanceId(parentWorkflowInstanceId)
            .childWorkflowInstanceId(childWorkflowInstanceId)
            .build();
    if (!operationStore.createOperationResponse(operationResponse)) {
      log.warn("unable to persist operation response: {}", operationResponse);
    }
    scheduleDecision(operationRequest.getWorkflowInstanceId());
  }

  /**
   * Executes a callback for the given workflow instance.
   *
   * @param workflowInstanceId The workflow instance ID
   * @throws IllegalArgumentException In case the workflow instance ID is invalid.
   */
  public void executeWorkflowInstanceCallback(@NonNull String workflowInstanceId) {
    val instance = getWorkflowInstance(workflowInstanceId);
    if (instance.getCallbackHandlerClazz() != null) {
      val handler = registry.getCallbackHandler(instance.getCallbackHandlerClazz());
      handler.handleUpdate(instance, this);
    }
  }
}
