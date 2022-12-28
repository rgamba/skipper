package com.maestroworkflow.models;

import com.maestroworkflow.api.MaestroWorkflow;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;

@Value
public class WorkflowType implements Serializable {
    @NonNull Class<? extends MaestroWorkflow> clazz;
}
