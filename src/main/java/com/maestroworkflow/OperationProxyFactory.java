package com.maestroworkflow;

import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.api.OperationConfig;
import com.maestroworkflow.api.WorkflowContext;
import com.maestroworkflow.api.annotations.WorkflowMethod;
import javassist.util.proxy.ProxyFactory;
import javax.annotation.Nullable;

public class OperationProxyFactory {

  public static <T> T create(Class<T> clazz, @Nullable OperationConfig config) {
    ProxyFactory factory = new ProxyFactory();
    factory.setSuperclass(clazz);
    Object instance;
    boolean isWorkflow = MaestroWorkflow.class.isAssignableFrom(clazz);
    if (isWorkflow) {
      factory.setFilter(method -> method.isAnnotationPresent(WorkflowMethod.class));
      factory.setInterfaces(new Class[] {MaestroWorkflow.class});
    }
    try {
      instance =
          factory.create(
              new Class<?>[0],
              new Object[0],
              new OperationProxyHandler(
                  WorkflowContext.get().getOperationResponses(),
                  WorkflowContext.get(),
                  clazz,
                  config,
                  isWorkflow));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return clazz.cast(instance);
  }

  public static <T> T create(Class<T> clazz) {
    return create(clazz, null);
  }
}
