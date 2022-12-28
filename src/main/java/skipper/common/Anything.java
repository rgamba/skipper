package skipper.common;

import com.google.gson.annotations.JsonAdapter;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import lombok.*;
import skipper.serde.StrongType;
import skipper.serde.StrongTypeAdapter;

@Value
@Builder
@AllArgsConstructor
public class Anything implements Serializable {
  /**
   * When using reflection to get the return type of methods, when the return type is a primitive
   * type, the type is returned as a class with the name matching the primitive name (like the map
   * keys below) instead of the actual wrapper class. In order to fix this, in case we receive
   * classes whos name equals any of the keys in this map, we are just going to map them back to
   * their boxed type.
   */
  public static Map<String, Class<?>> PRIMITIVES_TO_BOXED =
      new HashMap<String, Class<?>>() {
        {
          put("boolean", Boolean.class);
          put("int", Integer.class);
          put("float", Float.class);
          put("byte", Byte.class);
          put("short", Short.class);
          put("long", Long.class);
        }
      };

  Object value;

  @JsonAdapter(StrongTypeAdapter.class)
  StrongType type;

  public Anything(
      @NonNull Class<?> clazz, @NonNull Object value, @NonNull Class<?>... parameterTypes) {
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
    if (PRIMITIVES_TO_BOXED.containsKey(clazz.getName())) {
      clazz = PRIMITIVES_TO_BOXED.get(clazz.getName());
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
    // Check if its an optional
    if (value instanceof Optional) {
      Optional<?> object = (Optional) value;
      object.ifPresent(o -> result.add(o.getClass()));
    }
    return result;
  }
}
