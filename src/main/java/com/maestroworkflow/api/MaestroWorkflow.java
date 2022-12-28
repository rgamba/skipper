package com.maestroworkflow.api;

import lombok.NonNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

public interface MaestroWorkflow {
    default List<Object> join(@NonNull Callable<Object>... promises) {
        return Control.join(promises);
    }

    default void waitUntil(@NonNull Callable<Boolean> condition, Duration timeout) {
        Control.waitUntil(condition, timeout);
    }
}
