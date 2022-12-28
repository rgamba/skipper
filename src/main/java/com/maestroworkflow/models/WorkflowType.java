package com.maestroworkflow.models;

import com.maestroworkflow.api.MaestroWorkflow;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;

@Value
public class WorkflowType implements Serializable {
  @NonNull Class<? extends MaestroWorkflow> clazz;
}
