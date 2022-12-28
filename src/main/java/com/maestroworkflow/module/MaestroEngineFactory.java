package com.maestroworkflow.module;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.maestroworkflow.DecisionExecutor;
import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.OperationExecutor;
import com.maestroworkflow.store.OperationStore;
import com.maestroworkflow.store.TimerStore;
import com.maestroworkflow.store.WorkflowInstanceStore;
import lombok.NonNull;

import java.time.Clock;

public class MaestroEngineFactory {
    private final WorkflowInstanceStore workflowInstanceStore;
    private final OperationStore operationStore;
    private final TimerStore timerStore;
    private final DecisionExecutor decisionExecutor;
    private final OperationExecutor operationExecutor;
    private final Clock clock;

    @Inject
    public MaestroEngineFactory(@Named("UTC") Clock clock, @NonNull WorkflowInstanceStore workflowInstanceStore, @NonNull OperationStore operationStore,
                                @NonNull TimerStore timerStore, @NonNull DecisionExecutor decisionExecutor,
                                @NonNull OperationExecutor operationExecutor) {
        this.workflowInstanceStore = workflowInstanceStore;
        this.operationStore = operationStore;
        this.timerStore = timerStore;
        this.decisionExecutor = decisionExecutor;
        this.operationExecutor = operationExecutor;
        this.clock = clock;
    }

    public MaestroEngine create(Injector injector) {
        return new MaestroEngine(clock, workflowInstanceStore, operationStore, timerStore, decisionExecutor,
                operationExecutor, injector);
    }
}
