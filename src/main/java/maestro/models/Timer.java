package maestro.models;

import java.time.Instant;
import lombok.*;
import maestro.common.Anything;
import maestro.timers.TimerHandler;

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
