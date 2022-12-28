package demo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import demo.operations.Operations;
import demo.services.Ledger;
import maestro.OperationProxyFactory;
import maestro.api.annotations.WorkflowOperation;

public class DemoModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(Ledger.class).toInstance(new Ledger());
  }

  @Provides
  @WorkflowOperation
  static Operations provideGreeterOperation() {
    return OperationProxyFactory.create(Operations.class);
  }
}
