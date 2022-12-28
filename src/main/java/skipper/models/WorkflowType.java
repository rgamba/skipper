package skipper.models;

import com.google.gson.annotations.JsonAdapter;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;
import skipper.api.SkipperWorkflow;
import skipper.common.ValidationUtils;
import skipper.serde.ClassTypeAdapter;

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
