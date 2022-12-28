package com.maestroworkflow.models;

import com.google.gson.annotations.JsonAdapter;
import com.maestroworkflow.ValidationUtils;
import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.api.WaitTimeout;
import com.maestroworkflow.serde.ClassTypeAdapter;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;

@Value
public class OperationType implements Serializable {
  @JsonAdapter(ClassTypeAdapter.class)
  @NonNull
  Class<?> clazz;

  @NonNull String method;
  @NonNull ClazzType clazzType;

  public OperationType(@NonNull Class<?> clazz, @NonNull String method) {
    this.clazz = clazz;
    this.method = method;
    this.clazzType = ClazzType.OPERATION;
  }

  public OperationType(
      @NonNull Class<?> clazz, @NonNull String method, @NonNull ClazzType clazzType) {
    ValidationUtils.when(clazzType == ClazzType.WORKFLOW)
        .thenExpect(
            MaestroWorkflow.class.isAssignableFrom(clazz), "clazz must implement MaestroWorkflow");
    this.clazz = clazz;
    this.method = method;
    this.clazzType = clazzType;
  }

  public static OperationType waitTimeout(@NonNull String checkpoint) {
    return new OperationType(WaitTimeout.class, checkpoint, ClazzType.TIMEOUT);
  }

  public boolean isWaitTimeout() {
    return clazzType.equals(ClazzType.TIMEOUT);
  }

  public boolean isWorkflow() {
    return clazzType.equals(ClazzType.WORKFLOW);
  }

  public enum ClazzType {
    OPERATION,
    WORKFLOW,
    TIMEOUT;
  }
}
