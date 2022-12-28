package com.maestroworkflow.models;

import com.google.gson.annotations.JsonAdapter;
import com.maestroworkflow.serde.StrongType;
import com.maestroworkflow.serde.StrongTypeAdapter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Anything implements Serializable {
  Object value;

  @JsonAdapter(StrongTypeAdapter.class)
  StrongType type;

  public Anything(Class<?> clazz, Object value, Class<?>... parameterTypes) {
    if (clazz.getTypeParameters().length != parameterTypes.length) {
      throw new IllegalArgumentException(
          String.format(
              "expected %d parameter types but got %s",
              value.getClass().getTypeParameters().length, parameterTypes.length));
    }
    this.value = clazz.cast(value);
    this.type =
        StrongType.builder()
            .type(clazz)
            .parameters(
                parameterTypes.length > 0
                    ? Arrays.stream(parameterTypes).collect(Collectors.toList())
                    : null)
            .build();
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
}
