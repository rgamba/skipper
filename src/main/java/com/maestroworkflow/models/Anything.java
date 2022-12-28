package com.maestroworkflow.models;

import java.io.Serializable;
import lombok.Value;

@Value
public class Anything implements Serializable {
  Class<?> clazz;
  Object value;

  public Anything(Class<?> clazz, Object value) {
    clazz.cast(value);
    this.clazz = clazz;
    this.value = value;
  }

  public Anything(Object value) {
    this.clazz = value.getClass();
    this.value = value;
  }

  public static Anything of(Object value) {
    return new Anything(value);
  }
}
