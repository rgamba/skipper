package com.maestroworkflow.models;

import com.maestroworkflow.timers.TimerHandler;
import java.time.Instant;
import lombok.*;

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
