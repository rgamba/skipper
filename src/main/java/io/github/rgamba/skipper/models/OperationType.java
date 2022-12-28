package io.github.rgamba.skipper.models;

import com.google.gson.annotations.JsonAdapter;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.api.WaitTimeout;
import io.github.rgamba.skipper.common.ValidationUtils;
import io.github.rgamba.skipper.serde.ClassTypeAdapter;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;

@Value
public class OperationType implements Serializable {
  public static int OPERATION_TYPE_MAX_LENGTH = 150;

  @JsonAdapter(ClassTypeAdapter.class)
  @NonNull
  Class<?> clazz;

  @NonNull String method;
  @NonNull ClazzType clazzType;

  public OperationType(@NonNull Class<?> clazz, @NonNull String method) {
    this(clazz, method, ClazzType.OPERATION);
  }

  public OperationType(
      @NonNull Class<?> clazz, @NonNull String method, @NonNull ClazzType clazzType) {
    ValidationUtils.when(clazzType == ClazzType.WORKFLOW)
        .thenExpect(
            SkipperWorkflow.class.isAssignableFrom(clazz), "clazz must implement MaestroWorkflow");
    ValidationUtils.require(clazz.getName().length() + method.length() <= OPERATION_TYPE_MAX_LENGTH)
        .orFail(
            "class name + method length should not be larger than %d chars",
            OPERATION_TYPE_MAX_LENGTH);

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
