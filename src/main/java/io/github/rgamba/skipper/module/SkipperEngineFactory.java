package io.github.rgamba.skipper.module;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.github.rgamba.skipper.DecisionExecutor;
import io.github.rgamba.skipper.DependencyRegistry;
import io.github.rgamba.skipper.OperationExecutor;
import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.store.OperationStore;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.store.WorkflowInstanceStore;
import java.time.Clock;
import lombok.NonNull;

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
