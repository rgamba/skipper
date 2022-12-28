package demo;

import com.google.inject.Guice;
import com.google.inject.Injector;
import demo.resources.AppResource;
import demo.services.Ledger;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import lombok.val;
import maestro.admin.AdminResource;
import maestro.client.MaestroClient;
import maestro.module.MaestroEngineFactory;
import maestro.module.MaestroModule;
import maestro.module.TimerProcessorFactory;
import maestro.store.mysql.MySqlMigrationsManager;

public class App extends Application<AppConfig> {
  public static void main(String[] args) throws Exception {
    new App().run(args);
  }

  @Override
  public void run(AppConfig appConfig, Environment environment) throws Exception {
    Injector injector =
        Guice.createInjector(
            new MaestroModule(
                "jdbc:mysql://localhost:3306/maestro?serverTimezone=UTC", "root", "root"));
    val migrationMgr = injector.getInstance(MySqlMigrationsManager.class);
    migrationMgr.migrate();
    Injector repo = Guice.createInjector(new DemoModule());
    val engine = injector.getInstance(MaestroEngineFactory.class).create(repo);
    val processor = injector.getInstance(TimerProcessorFactory.class).create(engine);
    processor.start();

    val resource = new AppResource(new MaestroClient(engine), repo.getInstance(Ledger.class));
    environment.jersey().register(resource);
    environment.jersey().register(new AdminResource(engine));
  }

  @Override
  public void initialize(Bootstrap<AppConfig> bootstrap) {
    bootstrap.addBundle(new ViewBundle<AppConfig>());
  }
}
