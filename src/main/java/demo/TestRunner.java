package demo;

import static skipper.Metrics.registry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import demo.operations.Operations;
import demo.workflows.ApprovalWorkflow;
import demo.workflows.TransferWorkflow;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.ObjectName;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.DependencyRegistry;
import skipper.Metrics;
import skipper.SkipperEngine;
import skipper.api.CallbackHandler;
import skipper.api.WorkflowCreationRequest;
import skipper.common.Anything;
import skipper.models.WorkflowInstance;
import skipper.models.WorkflowType;
import skipper.module.SkipperEngineFactory;
import skipper.module.TimerProcessorFactory;
import skipper.store.mysql.MySqlMigrationsManager;

public class TestRunner {
  private static final Map<String, Lapse> workflows = new ConcurrentHashMap<>();

  private static TestRunnerControl control;

  private static final AtomicInteger workflowsCreated = new AtomicInteger(0);
  private static final AtomicInteger workflowsCompleted = new AtomicInteger(0);

  public static void main(String[] args) throws Exception {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.setLevel(Level.WARN);

    final JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();
    final ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(registry).build();

    val mbs = ManagementFactory.getPlatformMBeanServer();
    int numberOfTestsPerRun = 50;
    int numberOfRuns = 1000;
    control = new TestRunnerControl(numberOfTestsPerRun, numberOfRuns, false);
    ObjectName name = new ObjectName("TEST_RUNNER:name=TestRunnerControl");
    mbs.registerMBean(control, name);

    Injector injector = Guice.createInjector(new DemoModule("jdbc:mysql://localhost:3306/maestro?serverTimezone=UTC", "root", "root"));
    val migrationMgr = injector.getInstance(MySqlMigrationsManager.class);
    migrationMgr.migrate();

    CallbackHandler handler = new CompletedHandler();
    val registry =
        DependencyRegistry.builder()
            .addWorkflowFactory(() -> injector.getInstance(TransferWorkflow.class))
            .addWorkflowFactory(() -> injector.getInstance(ApprovalWorkflow.class))
            .addOperation(injector.getInstance(Operations.class))
            .addCallbackHandler(handler)
            .build();
    val engine = injector.getInstance(SkipperEngineFactory.class).create(registry);
    val processor = injector.getInstance(TimerProcessorFactory.class).create(engine);
    processor.start();
    System.out.println("Starting tests...");
    runTests(engine);

    Instant timeout = Instant.now().plus(Duration.ofSeconds(10));
    while (workflowsCompleted.incrementAndGet() < workflowsCreated.get()
        && Instant.now().isBefore(timeout)) {
      Thread.sleep(500);
    }

    long durationSum = 0;
    Duration minDuration = Duration.ofHours(1000);
    Duration maxDuration = Duration.ZERO;

    for (val entry : workflows.entrySet()) {
      if (entry.getValue().getEnd() == null) {
        System.err.printf("workflow %s has not completed!%n", entry.getKey());
        continue;
      }
      durationSum += entry.getValue().getLapse().toMillis();
      if (entry.getValue().getLapse().compareTo(minDuration) < 0) {
        minDuration = entry.getValue().getLapse();
      }
      if (entry.getValue().getLapse().compareTo(maxDuration) > 0) {
        maxDuration = entry.getValue().getLapse();
      }
    }

    consoleReporter.report();

    System.out.printf(
        "Average execution time: %d milliseconds%n", durationSum / numberOfTestsPerRun);
    System.out.printf("Min execution time: %d milliseconds%n", minDuration.toMillis());
    System.out.printf("Max execution time: %d milliseconds%n", maxDuration.toMillis());

    System.exit(0);
  }

  private static void runTests(SkipperEngine engine) throws InterruptedException {
    Metrics.registerIntegerGauge(
        "test_runner",
        "workflow_execution_backlog",
        () -> workflowsCreated.get() - workflowsCompleted.get());
    val executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
    for (int batch = 0; batch <= control.getTestDurationInSeconds(); batch++) {
      for (int i = 0; i <= control.getNumberOfTestsPerSecond(); i++) {
        executor.execute(() -> runTest(engine));
      }
      Thread.sleep(1000);
      if (control.isStop()) {
        break;
      }
    }
  }

  private static void runTest(SkipperEngine engine) {
    List<Anything> args = new ArrayList<>();
    args.add(Anything.of("system"));
    args.add(Anything.of("paola"));
    args.add(Anything.of(1));
    WorkflowCreationRequest req =
        WorkflowCreationRequest.builder()
            .correlationId(UUID.randomUUID().toString())
            .arguments(args)
            .workflowType(new WorkflowType(TransferWorkflow.class))
            .callbackHandlerClazz(CompletedHandler.class)
            .build();
    try {
      val resp = engine.createWorkflowInstance(req);
      val latencyTimer = registry.timer("test_runner.workflow_execution.latency");
      workflows.put(
          resp.getWorkflowInstance().getId(), new Lapse(Instant.now(), null, latencyTimer.time()));
      registry.counter("test_runner.workflows_created").inc();
      workflowsCreated.incrementAndGet();
    } catch (Exception e) {
      registry.counter("test_runner.workflow_creation_errors").inc();
    }
  }

  public static class CompletedHandler implements CallbackHandler {

    public CompletedHandler() {}

    @Override
    public void handleUpdate(
        @NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine) {
      if (workflowInstance.getStatus().isCompleted() || workflowInstance.getStatus().isError()) {
        if (workflows.get(workflowInstance.getId()).getEnd() != null) {
          return;
        }
        val lapse = workflows.get(workflowInstance.getId());
        lapse.setEnd(Instant.now());
        lapse.timer.stop();
        workflowsCompleted.incrementAndGet();
        if (workflowInstance.getStatus().isCompleted()) {
          registry.counter("test_runner.completed_workflows").inc();
        } else {
          registry.counter("test_runner.error_workflows").inc();
        }
      }
    }
  }

  @Data
  @AllArgsConstructor
  public static class Lapse {
    Instant start;
    Instant end;
    Timer.Context timer;

    public Duration getLapse() {
      return Duration.between(start, end);
    }
  }

  public interface TestRunnerControlMBean {
    void stop();

    void setNumberOfTestsPerSecond(int num);

    int getNumberOfTestsPerSecond();

    void setTestDurationInSeconds(int num);

    int getTestDurationInSeconds();
  }

  @AllArgsConstructor
  public static class TestRunnerControl implements TestRunnerControlMBean {
    private int numberOfTestsPerSecond;
    private int testDurationInSeconds;
    @Getter private boolean stop;

    @Override
    public void stop() {
      this.stop = true;
    }

    @Override
    public void setNumberOfTestsPerSecond(int num) {
      this.numberOfTestsPerSecond = num;
    }

    @Override
    public int getNumberOfTestsPerSecond() {
      return this.numberOfTestsPerSecond;
    }

    @Override
    public void setTestDurationInSeconds(int num) {
      this.testDurationInSeconds = num;
    }

    @Override
    public int getTestDurationInSeconds() {
      return testDurationInSeconds;
    }
  }
}
