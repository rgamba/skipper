package demo;

import com.google.inject.Guice;
import com.google.inject.Injector;
import demo.operations.Operations;
import demo.resources.AppResource;
import demo.services.Ledger;
import demo.workflowHandlers.TransferCallbackHandler;
import demo.workflows.ApprovalWorkflow;
import demo.workflows.TransferWorkflow;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import lombok.val;
import skipper.DependencyRegistry;
import skipper.admin.AdminResource;
import skipper.client.SkipperClient;
import skipper.module.SkipperEngineFactory;
import skipper.module.TimerProcessorFactory;
import skipper.store.mysql.MySqlMigrationsManager;

public class App extends Application<AppConfig> {
  public static void main(String[] args) throws Exception {
    new App().run(args);
  }

  @Override
  public void run(AppConfig appConfig, Environment environment) throws Exception {
    Injector injector = Guice.createInjector(new DemoModule());
    ;
    val migrationMgr = injector.getInstance(MySqlMigrationsManager.class);
    migrationMgr.migrate();
    val registry =
        DependencyRegistry.builder()
            .addWorkflowFactory(() -> injector.getInstance(TransferWorkflow.class))
            .addWorkflowFactory(() -> injector.getInstance(ApprovalWorkflow.class))
            .addOperation(injector.getInstance(Operations.class))
            .addCallbackHandler(new TransferCallbackHandler())
            .build();
    val engine = injector.getInstance(SkipperEngineFactory.class).create(registry);
    val processor = injector.getInstance(TimerProcessorFactory.class).create(engine);
    processor.start();

    val resource = new AppResource(new SkipperClient(engine), injector.getInstance(Ledger.class));
    environment.jersey().register(resource);
    environment.jersey().register(new AdminResource(engine));
  }

  @Override
  public void initialize(Bootstrap<AppConfig> bootstrap) {
    bootstrap.addBundle(new ViewBundle<AppConfig>());
  }
}
