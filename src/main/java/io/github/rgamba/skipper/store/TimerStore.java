package io.github.rgamba.skipper.store;

import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.Timer;
import java.time.Duration;
import java.util.List;
import lombok.NonNull;

public interface TimerStore {
  Timer createOrUpdate(@NonNull Timer timer);

  Timer get(@NonNull String timerId);

  void update(@NonNull String timerId, Duration timeout);

  void update(@NonNull String timerId, @NonNull Duration timeout, @NonNull Anything payload);

  boolean delete(@NonNull Timer timer);

  List<Timer> getExpiredTimers();

  long countExpiredTimers();
}
