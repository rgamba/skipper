package io.github.rgamba.skipper.client;

import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.api.CallbackHandler;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.annotations.SignalConsumer;
import io.github.rgamba.skipper.api.annotations.WorkflowMethod;
import javassist.util.proxy.ProxyFactory;
import lombok.NonNull;

public class SkipperClientFactory {
  private final SkipperEngine engine;

  public SkipperClientFactory(@NonNull SkipperEngine engine) {
    this.engine = engine;
  }

  public <T extends SkipperWorkflow> T create(
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
