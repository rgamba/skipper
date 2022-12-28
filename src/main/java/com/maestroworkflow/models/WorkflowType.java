package com.maestroworkflow.models;

import com.google.gson.annotations.JsonAdapter;
import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.serde.ClassTypeAdapter;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;

@Value
public class WorkflowType implements Serializable {
  @JsonAdapter(ClassTypeAdapter.class)
  @NonNull
  Class<? extends MaestroWorkflow> clazz;
}
