package demo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.Any;
import demo.operations.Operations;
import demo.workflowHandlers.TransferCallbackHandler;
import demo.workflows.ApprovalWorkflow;
import demo.workflows.TransferWorkflow;
import lombok.NonNull;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.DependencyRegistry;
import skipper.SkipperEngine;
import skipper.api.CallbackHandler;
import skipper.api.WorkflowCreationRequest;
import skipper.common.Anything;
import skipper.models.WorkflowInstance;
import skipper.models.WorkflowType;
import skipper.module.SkipperEngineFactory;
import skipper.module.TimerProcessorFactory;
import skipper.store.mysql.MySqlMigrationsManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class TestRunner {
    private static final Map<String, Instant> workflows = new ConcurrentHashMap<>();
    private static final int numberOfTests = 10000;

    public static void main(String[] args) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.WARN);

        Injector injector = Guice.createInjector(new DemoModule());
        val migrationMgr = injector.getInstance(MySqlMigrationsManager.class);
        migrationMgr.migrate();

        CountDownLatch latch = new CountDownLatch(numberOfTests);
        CallbackHandler handler = new CompletedHandler(latch);
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
        Instant startTime = Instant.now();
        System.out.println("Starting tests...");
        runTests(engine);
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        if (!completed) {
            System.out.printf("Timeout. %d tests completed only%n", latch.getCount());
        } else {
            System.out.printf("Completed %d tests in %d millis%n", numberOfTests, (Instant.now().toEpochMilli() - startTime.toEpochMilli()));
        }
        Thread.sleep(2000);
        System.exit(1);
    }

    private static void runTests(SkipperEngine engine) {

        val executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        for (int i = 0; i <= numberOfTests; i++) {
            executor.execute(() -> runTest(engine));
        }
    }

    private static void runTest(SkipperEngine engine) {
        List<Anything> args = new ArrayList<>();
        args.add(Anything.of("system"));
        args.add(Anything.of("paola"));
        args.add(Anything.of(1));
        WorkflowCreationRequest req = WorkflowCreationRequest.builder()
                .correlationId(UUID.randomUUID().toString())
                .arguments(args)
                .workflowType(new WorkflowType(TransferWorkflow.class))
                .callbackHandlerClazz(CompletedHandler.class)
                .build();
        val resp = engine.createWorkflowInstance(req);
        workflows.put(resp.getWorkflowInstance().getId(), Instant.now());
    }

    public static class CompletedHandler implements CallbackHandler {
        private final CountDownLatch latch;

        public CompletedHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void handleUpdate(@NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine) {
            //System.out.printf("%s -> %s%n", workflowInstance.getId(), workflowInstance.getStatus());
            latch.countDown();
        }
    }
}
