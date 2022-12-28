package skipper.models;

import java.time.Instant;
import lombok.*;
import skipper.common.Anything;
import skipper.timers.TimerHandler;

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
