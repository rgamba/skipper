package com.maestroworkflow.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.maestroworkflow.store.*;
import java.time.Clock;

public class MaestroModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(Clock.class).annotatedWith(Names.named("UTC")).toInstance(Clock.systemUTC());
    bind(WorkflowInstanceStore.class).to(InMemoryWorkflowInstanceStore.class);
    bind(TimerStore.class).to(InMemoryTimerStore.class);
    bind(OperationStore.class).to(InMemoryOperationStore.class);
  }
}
