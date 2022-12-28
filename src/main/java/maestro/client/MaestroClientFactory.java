package maestro.client;

import javassist.util.proxy.ProxyFactory;
import lombok.NonNull;
import maestro.MaestroEngine;
import maestro.api.CallbackHandler;
import maestro.api.MaestroWorkflow;
import maestro.api.annotations.SignalConsumer;
import maestro.api.annotations.WorkflowMethod;

public class MaestroClientFactory {
  private final MaestroEngine engine;

  public MaestroClientFactory(@NonNull MaestroEngine engine) {
    this.engine = engine;
  }

  public <T extends MaestroWorkflow> T create(
      Class<T> clazz,
      @NonNull String correlationId,
      Class<? extends CallbackHandler> callbackHandler) {
    ProxyFactory factory = new ProxyFactory();
    factory.setSuperclass(clazz);
    factory.setFilter(
        method ->
            method.isAnnotationPresent(WorkflowMethod.class)
                || method.isAnnotationPresent(SignalConsumer.class));
    Object instance;
    try {
      instance =
          factory.create(
              new Class<?>[0],
              new Object[0],
              new WorkflowProxyHandler(engine, clazz, correlationId, callbackHandler));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return clazz.cast(instance);
  }
}
