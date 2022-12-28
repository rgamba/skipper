package com.maestroworkflow.api;

import java.util.concurrent.Callable;
import lombok.NonNull;

public class Promise implements Callable<Object> {
  private final Callable<Object> callable;

  public Promise(@NonNull Callable<Object> callable) {
    this.callable = callable;
  }

  public static Promise from(@NonNull Callable<Object> callable) {
    return new Promise(callable);
  }

  @Override
  public Object call() throws Exception {
    return callable.call();
  }
}
