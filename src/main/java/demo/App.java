package demo;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.maestroworkflow.admin.AdminResource;
import com.maestroworkflow.module.MaestroEngineFactory;
import com.maestroworkflow.module.MaestroModule;
import com.maestroworkflow.module.TimerProcessorFactory;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import lombok.val;

public class App extends Application<AppConfig> {
    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void run(AppConfig appConfig, Environment environment) throws Exception {
        Injector injector = Guice.createInjector(new MaestroModule());
        Injector repo = Guice.createInjector(new DemoModule());
        val engine = injector.getInstance(MaestroEngineFactory.class).create(repo);
        val processor = injector.getInstance(TimerProcessorFactory.class).create(engine);
        new Thread(processor::startProcessing).start();


        val resource = new AppResource(engine);
        environment.jersey().register(resource);
        environment.jersey().register(new AdminResource(engine));
    }

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        bootstrap.addBundle(new ViewBundle<AppConfig>());
    }
}
