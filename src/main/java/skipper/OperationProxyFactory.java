package skipper;

import javassist.util.proxy.ProxyFactory;
import javax.annotation.Nullable;
import skipper.api.OperationConfig;
import skipper.api.SkipperWorkflow;
import skipper.api.annotations.WorkflowMethod;

public class OperationProxyFactory {

  public static <T> T create(Class<T> clazz, @Nullable OperationConfig config) {
    ProxyFactory factory = new ProxyFactory();
    factory.setSuperclass(clazz);
    Object instance;
    boolean isWorkflow = SkipperWorkflow.class.isAssignableFrom(clazz);
    if (isWorkflow) {
      factory.setFilter(method -> method.isAnnotationPresent(WorkflowMethod.class));
      factory.setInterfaces(new Class[] {SkipperWorkflow.class});
    }
    try {
      instance =
          factory.create(
              new Class<?>[0], new Object[0], new OperationProxyHandler(clazz, config, isWorkflow));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return clazz.cast(instance);
  }

  public static <T> T create(Class<T> clazz) {
    return create(clazz, null);
  }
}
