package demo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import demo.operations.Operations;
import demo.services.Ledger;
import io.github.rgamba.skipper.OperationProxyFactory;
import io.github.rgamba.skipper.api.annotations.WorkflowOperation;
import io.github.rgamba.skipper.module.SkipperModule;

public class DemoModule extends AbstractModule {
  @Override
  protected void configure() {
    install(
        new SkipperModule(
            "jdbc:mysql://localhost:3306/maestro?serverTimezone=UTC", "root", "root"));
    bind(Ledger.class).toInstance(new Ledger());
  }

  @Provides
  @WorkflowOperation
  static Operations provideGreeterOperation() {
    return OperationProxyFactory.create(Operations.class);
  }
}
