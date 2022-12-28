package maestro.common;

import com.google.gson.annotations.JsonAdapter;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.val;
import maestro.serde.StrongType;
import maestro.serde.StrongTypeAdapter;

@Value
@Builder
@AllArgsConstructor
public class Anything implements Serializable {
  Object value;

  @JsonAdapter(StrongTypeAdapter.class)
  StrongType type;

  public Anything(Class<?> clazz, Object value, Class<?>... parameterTypes) {
    List<Class<?>> params =
        parameterTypes.length > 0
            ? Arrays.stream(parameterTypes).collect(Collectors.toList())
            : tryGetTypeParams(value);

    if (clazz.getTypeParameters().length != params.size()) {
      throw new IllegalArgumentException(
          String.format(
              "expected %d parameter types but got %s",
              value.getClass().getTypeParameters().length, parameterTypes.length));
    }
    this.value = clazz.cast(value);
    this.type =
        StrongType.builder().type(clazz).parameters(params.isEmpty() ? null : params).build();
  }

  public Anything(Object value, Class<?>... parameterTypes) {
    this(value.getClass(), value, parameterTypes);
  }

  public static Anything of(Object value, Class<?>... parameterTypes) {
    return new Anything(value, parameterTypes);
  }

  public Class<?> getClazz() {
    return type.getType();
  }

  private <T> T cast(Object value, Class<T> type) {
    return (T) value;
  }

  private List<Class<?>> tryGetTypeParams(Object value) {
    List<Class<?>> result = new ArrayList<>();
    // Check if list
    if (value instanceof List) {
      List<?> object = (List) value;
      if (!object.isEmpty()) {
        result.add(object.get(0).getClass());
        return result;
      }
    }
    // Check if a set
    if (value instanceof Set) {
      Set<?> object = (Set) value;
      if (!object.isEmpty()) {
        result.add(object.iterator().next().getClass());
        return result;
      }
    }
    // Check if a map
    if (value instanceof Map) {
      Map<?, ?> object = (Map) value;
      if (!object.isEmpty()) {
        val entry = object.entrySet().iterator().next();
        result.add(entry.getKey().getClass());
        result.add(entry.getValue().getClass());
      }
    }
    return result;
  }
}
