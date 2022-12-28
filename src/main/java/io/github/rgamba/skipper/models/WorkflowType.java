package io.github.rgamba.skipper.models;

import com.google.gson.annotations.JsonAdapter;
import io.github.rgamba.skipper.api.SkipperWorkflow;
import io.github.rgamba.skipper.common.ValidationUtils;
import io.github.rgamba.skipper.serde.ClassTypeAdapter;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;

@Value
public class WorkflowType implements Serializable {
  public static int WORKFLOW_NAME_MAX_LENGTH = 200;

  @JsonAdapter(ClassTypeAdapter.class)
  @NonNull
  Class<? extends SkipperWorkflow> clazz;

  public WorkflowType(@NonNull Class<? extends SkipperWorkflow> clazz) {
    ValidationUtils.require(clazz.getName().length() <= WORKFLOW_NAME_MAX_LENGTH)
        .orFail("workflow clazz name must not be larger than %d chars", WORKFLOW_NAME_MAX_LENGTH);
    this.clazz = clazz;
  }
}
