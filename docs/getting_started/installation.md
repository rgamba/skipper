# Installing Skipper

## Getting Maven dependency

Skipper is provided as a Maven package.

```xml
<dependency>
  <groupId>io.github.rgamba</groupId>
  <artifactId>skipper-core</artifactId>
  <version>1.1.1</version>
</dependency>
```

## Installing Guice Module

Skipper uses Guice for dependency injection. In case your project doesn't use Guice, you can always instantiate
these objects manually. If you do use Guice, it will be much easier. First step is to install the Skipper module on
your own guice module.

```java
@Override
protected void configure() {
    install(new SkipperModule("<jdbc-url>", "<mysql-user>", "<mysql-pass>"));
}
```

## Choosing a storage engine

Skipper currently provide storage backends for: In memory storage and MySQL (more to come).
InMemory, while good for testing, it is of no much use unless you are running a single node and don't care about
persistence. So let's focus on MySQL.

The first step is to run migration to make sure the MySQL database is current.

```java
public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new MyAppModule());
    MySqlMigrationsManager migrationMgr = injector.getInstance(MySqlMigrationsManager.class);
    migrationMgr.migrate();
}
```

## Initiate your dependency registry

The dependency registry contains both your workflow definitions along with the operations that will be used by the
workflow engine.

For simplicity, we are not
going to include the whole main function along with previous code, but assume that the following lines will build on
top of the previous main function code.

```java
DependencyRegistry registry = DependencyRegistry.builder().build();
```

For now, we don't have any workflows or operations defined so we will just initiate an empty registry. Latter on we
will be adding elements to our registry.

## Initiating the workflow engine

`SkipperEngine` is the facade into the workflow engine. We need to instantiate it next.

```java
SkipperEngine engine = injector.getInstance(SkipperEngineFactory.class).create(registry);
```

## Initiating the processor

The processor is the component that takes care of processing elements on the timer scheduler. The timer scheduler is
the component that drives the cadence of the workflows.

```java
TimerProcessor processor = injector.getInstance(TimerProcessorFactory.class).create(engine);
processor.start();
```

## Optional - Expose Skipper Admin UI

Skipper provides an admin UI to help triage and debug workflows. If using `jersey`, this is as easy as:

```java
environment.jersey().register(new AdminResource(engine));
```

## Next steps

[Learn how to create workflow operations](creating_operations.md)