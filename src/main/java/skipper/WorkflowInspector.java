package skipper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import skipper.api.SkipperWorkflow;
import skipper.api.annotations.SignalConsumer;
import skipper.api.annotations.StateField;
import skipper.api.annotations.WorkflowMethod;
import skipper.common.Anything;

public class WorkflowInspector {
  private final Class<? extends SkipperWorkflow> clazz;

  @Getter private final SkipperWorkflow instance;

  public WorkflowInspector(
      @NonNull Class<? extends SkipperWorkflow> clazz, @NonNull SkipperWorkflow instance) {
    this.clazz = clazz;
    this.instance = instance;
  }

  public Map<String, Anything> getState() {
    Map<String, Anything> result = new HashMap<>();
    for (val field : getStateFields().collect(Collectors.toList())) {
      try {
        if (!field.isAccessible()) {
          field.setAccessible(true);
        }
        if (field.get(instance) == null) {
          result.put(field.getName(), null);
        } else {
          result.put(field.getName(), new Anything(field.getType(), field.get(instance)));
        }
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(
            String.format("unable to access state field %s, it must be public", field.getName()));
      }
    }
    return result;
  }

  public void setState(@NonNull Map<String, Anything> newState) {
    getStateFields()
        .forEach(
            s -> {
              try {
                if (newState.containsKey(s.getName())) {
                  if (!s.isAccessible()) {
                    s.setAccessible(true);
                  }
                  s.set(instance, newState.get(s.getName()).getValue());
                }
              } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format(
                        "unable to set state field %s with value of type '%s', expected type is '%s'",
                        s.getName(), newState.get(s.getName()).getClazz(), s.getType()),
                    e);
              } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                    String.format(
                        "unable to access state field %s, it must be public", s.getName()));
              }
            });
  }

  public Method getWorkflowMethod() {
    return Stream.of(clazz.getDeclaredMethods())
        .filter(m -> m.isAnnotationPresent(WorkflowMethod.class))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("the workflow class %s does not have a WorkflowMethod", clazz)));
  }

  public Method getSignalConsumerMethod(String methodName) {
    return Stream.of(clazz.getDeclaredMethods())
        .filter(m -> m.isAnnotationPresent(SignalConsumer.class))
        .filter(m -> m.getName().equals(methodName))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "the workflow class %s does not have a SignalConsumer method named '%s'",
                        clazz, methodName)));
  }

  private Stream<Field> getStateFields() {
    return Stream.of(clazz.getDeclaredFields())
        .filter(f -> f.isAnnotationPresent(StateField.class));
  }

  public Object[] getMethodParams(@NonNull Method method, @NonNull List<Anything> methodArgs) {
    if (methodArgs.size() != method.getParameters().length) {
      throw new IllegalArgumentException(
          String.format(
              "expected %d method arguments but got %d",
              method.getParameters().length, methodArgs.size()));
    }
    val params = method.getParameters();
    val response = new Object[methodArgs.size()];
    for (int i = 0; i < methodArgs.size(); i++) {
      val argValue = methodArgs.get(i);
      if (argValue == null) {
        if (params[i].getType().isPrimitive()) {
          throw new IllegalArgumentException(
              String.format(
                  "null value provided for non-nullable argument '%s' in workflow method '%s'",
                  params[i].getName(), method.getName()));
        }
        response[i] = null;
      } else {
        val paramType = boxPrimitive(params[i].getType());
        if (!paramType.isAssignableFrom(argValue.getClazz())) {
          throw new IllegalStateException(
              String.format(
                  "workflowInstance provided the argument %s with type %s when the expected type is %s",
                  params[i].getName(), argValue.getClazz(), params[i].getType()));
        }
        response[i] = argValue.getClazz().cast(argValue.getValue());
      }
    }
    return response;
  }

  public Class<?> boxPrimitive(Class<?> clazz) {
    if (Anything.PRIMITIVES_TO_BOXED.containsKey(clazz.getName())) {
      return Anything.PRIMITIVES_TO_BOXED.get(clazz.getName());
    }
    return clazz;
  }

  public Object[] getWorkflowMethodParams(@NonNull List<Anything> methodArgs) {
    return getMethodParams(getWorkflowMethod(), methodArgs);
  }
}
