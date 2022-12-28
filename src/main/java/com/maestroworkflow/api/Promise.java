package com.maestroworkflow.api;

import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;

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
