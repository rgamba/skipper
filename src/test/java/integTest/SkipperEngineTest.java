package integTest;

import static junit.framework.TestCase.*;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.github.rgamba.skipper.DependencyRegistry;
import io.github.rgamba.skipper.OperationProxyFactory;
import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.api.CallbackHandler;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WorkflowCreationRequest;
import io.github.rgamba.skipper.api.annotations.StateField;
import io.github.rgamba.skipper.api.annotations.WorkflowMethod;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.OperationType;
import io.github.rgamba.skipper.models.WorkflowInstance;
import io.github.rgamba.skipper.models.WorkflowType;
import io.github.rgamba.skipper.module.SkipperEngineFactory;
import io.github.rgamba.skipper.module.SkipperModule;
import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.runtime.WorkflowContext;
import io.github.rgamba.skipper.store.OperationStore;
import io.github.rgamba.skipper.store.SqlTransactionManager;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.store.WorkflowInstanceStore;
import io.github.rgamba.skipper.timers.DecisionTimerHandler;
import io.github.rgamba.skipper.timers.OperationRequestTimerHandler;
import io.github.rgamba.skipper.timers.WorkflowInstanceCallbackTimerHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
public class SkipperEngineTest {
  private static Logger logger = LoggerFactory.getLogger(SkipperEngineTest.class);

  private SkipperEngine engine;
  private Injector injector;
  private DependencyRegistry registry;

  private static final String TEST_WORKFLOW_ID = "test-123";
  private static final WorkflowInstance TEST_WORKFLOW_INSTANCE =
      WorkflowInstance.builder()
          .workflowType(new WorkflowType(SkipperWorkflow.class))
          .id(TEST_WORKFLOW_ID)
          .correlationId("corr-test-123")
          .initialArgs(new ArrayList<>())
          .status(WorkflowInstance.Status.ACTIVE)
          .state(new HashMap<>())
          .creationTime(Instant.MIN)
          .build();
  private static final OperationType TEST_OPERATION_TYPE = new OperationType(String.class, "test");
  private WorkflowInstanceStore instanceStore;
  private OperationStore operationStore;
  private TimerStore timerStore;

  public static class TestWorkflow implements SkipperWorkflow {
    @StateField private String defaultName = "test";

    public final GreeterOperation greeter = OperationProxyFactory.create(GreeterOperation.class);

    @WorkflowMethod
    @SneakyThrows
    public String foo(String bar) {
      String response = "";
      try {
        response = greeter.greet(bar);
      } catch (Exception e) {
        // Making sure that exception catching won't inadvertently catch StopWorkflowExecution
      }
      this.defaultName = bar;
      return response;
    }
  }

  public static class GreeterOperation {
    public String greet(String name) {
      return "Hello, " + name + "!";
    }
  }

  @Singleton
  public static class TestCallback implements CallbackHandler {
    public int counter = 0;

    @Override
    public void handleUpdate(
        @NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine) {
      this.counter += 1;
    }
  }

  @Before
  public void setUp() {
    DecisionThread.setWorkflowContext(
        new WorkflowContext("", Instant.now(), new ArrayList<>(), Instant.MIN));
    injector =
        Guice.createInjector(
            new SkipperModule(
                "jdbc:mysql://localhost:3306/maestro?serverTimezone=UTC", "root", "root"));
    injector.getInstance(SqlTransactionManager.class).begin(); // Begin "global" test transaction

    instanceStore = injector.getInstance(WorkflowInstanceStore.class);
    operationStore = injector.getInstance(OperationStore.class);
    timerStore = injector.getInstance(TimerStore.class);
    val engineFactory = injector.getInstance(SkipperEngineFactory.class);
    registry =
        DependencyRegistry.builder()
            .addWorkflowFactory(() -> new TestWorkflow())
            .addOperation(new GreeterOperation())
            .addCallbackHandler(new TestCallback())
            .build();
    engine = engineFactory.create(registry);
  }

  @After
  public void tearDown() {
    injector
        .getInstance(SqlTransactionManager.class)
        .rollback(); // rollback "global" test transaction
  }

  @Ignore(
      "This is only relevant when using DecisionExecutor but will fail with SyncDecisionExecutor")
  @Test
  public void testCreateWorkflow() throws InterruptedException {
    val correlationId = UUID.randomUUID().toString();
    val request =
        WorkflowCreationRequest.builder()
            .workflowType(new WorkflowType(TestWorkflow.class))
            .correlationId(correlationId)
            .arguments(
                new ArrayList<Anything>() {
                  {
                    add(new Anything(String.class, "Ricardo"));
                  }
                })
            .callbackHandlerClazz(TestCallback.class)
            .build();

    // 1. Create the workflow instance
    val response = engine.createWorkflowInstance(request);
    DecisionThread.setWorkflowContext(
        new WorkflowContext(
            response.getWorkflowInstance().getId(), Instant.now(), new ArrayList<>(), Instant.MIN));
    assertEquals(correlationId, response.getWorkflowInstance().getCorrelationId());
    assertEquals(
        response.getWorkflowInstance(), instanceStore.get(response.getWorkflowInstance().getId()));
    Thread.sleep(500);
    // Workflow creation should've queued a decision request in the timers datastore
    var timers = timerStore.getExpiredTimers();
    assertEquals(1, timers.size());

    // The first decision should return an operation request
    // Pop the element from the expired timers
    timerStore.delete(timers.get(0));
    // 2. Process the first decision request
    engine.processDecision(response.getWorkflowInstance().getId());
    // After processing the decision request, the first operation request should've been queued
    // in the timers store.
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
    assertEquals(
        timer.getPayload(), new Anything(String.class, response.getWorkflowInstance().getId()));
    timerStore.delete(timer);

    // Check that the operation response has been persisted as a result of processing the
    // operation request
    val operationResponses =
        operationStore.getOperationResponses(response.getWorkflowInstance().getId(), true);
    assertEquals(1, operationResponses.size());
    // Now that we have the operation response created, override the workflow context
    DecisionThread.setWorkflowContext(
        new WorkflowContext(
            response.getWorkflowInstance().getId(),
            Instant.now(),
            operationResponses,
            Instant.MIN));

    // 4. Process the decision request now that the operation response has been persisted. This
    // time around, the
    // workflow should've run to completion so check the response.
    engine.processDecision(response.getWorkflowInstance().getId());
    Thread.sleep(500);
    instance = engine.getWorkflowInstance(response.getWorkflowInstance().getId());
    assertEquals(WorkflowInstance.Status.COMPLETED, instance.getStatus());
    assertNotNull(instance.getResult());
    assertEquals("Hello, Ricardo!", instance.getResult().getValue());

    // 5. After processing the decision and as a result of the workflow being completed, a
    // callback timer should've
    // been created.
    timers = timerStore.getExpiredTimers();
    assertEquals(1, timers.size());
    timer = timerStore.get(timers.get(0).getTimerId());
    assertEquals(timer.getHandlerClazz(), WorkflowInstanceCallbackTimerHandler.class);
    assertEquals(
        timer.getPayload(), new Anything(String.class, response.getWorkflowInstance().getId()));
    engine.processCallback(response.getWorkflowInstance().getId());
    assertEquals(1, registry.getCallbackHandler(TestCallback.class).counter);
  }

  @Test
  public void testCreateWorkflowSyncDecisionExecutor() throws InterruptedException {
    val correlationId = UUID.randomUUID().toString();
    val request =
        WorkflowCreationRequest.builder()
            .workflowType(new WorkflowType(TestWorkflow.class))
            .correlationId(correlationId)
            .arguments(
                new ArrayList<Anything>() {
                  {
                    add(new Anything(String.class, "Ricardo"));
                  }
                })
            .callbackHandlerClazz(TestCallback.class)
            .build();

    // 1. Create the workflow instance
    val response = engine.createWorkflowInstance(request);
    DecisionThread.setWorkflowContext(
        new WorkflowContext(
            response.getWorkflowInstance().getId(), Instant.now(), new ArrayList<>(), Instant.MIN));
    assertEquals(correlationId, response.getWorkflowInstance().getCorrelationId());
    assertEquals(
        response.getWorkflowInstance(), instanceStore.get(response.getWorkflowInstance().getId()));
    Thread.sleep(500);
    // Workflow creation should've queued a decision request in the timers datastore
    var timers = timerStore.getExpiredTimers();
    assertEquals(1, timers.size());

    // The first decision should return an operation request
    // Pop the element from the expired timers
    timerStore.delete(timers.get(0));
    // 2. Process the first decision request
    engine.processDecision(response.getWorkflowInstance().getId());
    // After processing the decision request, the sync decision executor should've run all
    // operations synchronously
    // and completed the workflow.
    Thread.sleep(500);
    var instance = engine.getWorkflowInstance(response.getWorkflowInstance().getId());
    assertEquals(WorkflowInstance.Status.COMPLETED, instance.getStatus());
    assertNotNull(instance.getResult());
    assertEquals("Hello, Ricardo!", instance.getResult().getValue());

    // Verify that the workflow requests have been created
    val opRequests = operationStore.getOperationRequests(instance.getId());
    assertEquals(1, opRequests.size());

    // 5. After processing the decision and as a result of the workflow being completed, a
    // callback timer should've been created.
    timers = timerStore.getExpiredTimers();
    assertEquals(1, timers.size());
    var timer = timerStore.get(timers.get(0).getTimerId());
    assertEquals(timer.getHandlerClazz(), WorkflowInstanceCallbackTimerHandler.class);
    assertEquals(
        timer.getPayload(), new Anything(String.class, response.getWorkflowInstance().getId()));
    engine.processCallback(response.getWorkflowInstance().getId());
    assertEquals(1, registry.getCallbackHandler(TestCallback.class).counter);
  }
}
