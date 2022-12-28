package com.maestroworkflow.store;

import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.Timer;
import java.time.Duration;
import java.util.List;
import lombok.NonNull;

public interface TimerStore {
  Timer createOrUpdate(@NonNull Timer timer);

  Timer get(@NonNull String timerId);

  void update(@NonNull String timerId, @NonNull Duration timeout);

  void update(@NonNull String timerId, @NonNull Duration timeout, @NonNull Anything payload);

  boolean delete(@NonNull Timer timer);

  List<Timer> getExpiredTimers();
}
