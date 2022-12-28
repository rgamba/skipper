package io.github.rgamba.skipper.models;

import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.timers.TimerHandler;
import java.time.Instant;
import lombok.*;

@Value
@Builder(toBuilder = true)
public class Timer {
  @NonNull String timerId;
  Instant timeout;
  @NonNull Class<? extends TimerHandler> handlerClazz;
  @NonNull Anything payload;
  int retries;
  @NonNull int version;
}
