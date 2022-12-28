package com.maestroworkflow.models;

import com.maestroworkflow.timers.TimerHandler;
import lombok.*;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Timer {
    @NonNull String timerId;
    Instant timeout;
    @NonNull Class<? extends TimerHandler> handlerClazz;
    @NonNull Anything payload;
    int retries = 0;
    @NonNull int version;
}
