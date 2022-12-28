package skipper.module;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Clock;
import lombok.NonNull;
import skipper.DecisionExecutor;
import skipper.DependencyRegistry;
import skipper.OperationExecutor;
import skipper.SkipperEngine;
import skipper.store.OperationStore;
import skipper.store.TimerStore;
import skipper.store.WorkflowInstanceStore;

public class SkipperEngineFactory {
  private final WorkflowInstanceStore workflowInstanceStore;
  private final OperationStore operationStore;
  private final TimerStore timerStore;
  private final DecisionExecutor decisionExecutor;
  private final OperationExecutor operationExecutor;
  private final Clock clock;

  @Inject
  public SkipperEngineFactory(
      @Named("UTC") Clock clock,
      @NonNull WorkflowInstanceStore workflowInstanceStore,
      @NonNull OperationStore operationStore,
      @NonNull TimerStore timerStore,
      @NonNull DecisionExecutor decisionExecutor,
      @NonNull OperationExecutor operationExecutor) {
    this.workflowInstanceStore = workflowInstanceStore;
    this.operationStore = operationStore;
    this.timerStore = timerStore;
    this.decisionExecutor = decisionExecutor;
    this.operationExecutor = operationExecutor;
    this.clock = clock;
  }

  public SkipperEngine create(DependencyRegistry registry) {
    return new SkipperEngine(
        clock,
        workflowInstanceStore,
        operationStore,
        timerStore,
        decisionExecutor,
        operationExecutor,
        registry);
  }
}
