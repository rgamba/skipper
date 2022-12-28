package demo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import maestro.OperationProxyFactory;
import maestro.api.annotations.WorkflowOperation;

public class DemoModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  @WorkflowOperation
  static Operations provideGreeterOperation() {
    return OperationProxyFactory.create(Operations.class);
  }
}
