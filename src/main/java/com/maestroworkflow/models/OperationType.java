package com.maestroworkflow.models;

import com.maestroworkflow.ValidationUtils;
import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.api.WaitTimeout;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

import java.io.Serializable;

@Value
public class OperationType implements Serializable {
    @NonNull Class<?> clazz;
    @NonNull String method;
    @NonNull ClazzType clazzType;

    public OperationType(@NonNull Class<?> clazz, @NonNull String method) {
        this.clazz = clazz;
        this.method = method;
        this.clazzType = ClazzType.OPERATION;
    }

    public OperationType(@NonNull Class<?> clazz, @NonNull String method, @NonNull ClazzType clazzType) {
        ValidationUtils.when(clazzType == ClazzType.WORKFLOW)
                .thenExpect(MaestroWorkflow.class.isAssignableFrom(clazz), "clazz must implement MaestroWorkflow");
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
