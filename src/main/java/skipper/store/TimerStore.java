package skipper.store;

import java.time.Duration;
import java.util.List;
import lombok.NonNull;
import skipper.common.Anything;
import skipper.models.Timer;

public interface TimerStore {
  Timer createOrUpdate(@NonNull Timer timer);

  Timer get(@NonNull String timerId);

  void update(@NonNull String timerId, Duration timeout);

  void update(@NonNull String timerId, @NonNull Duration timeout, @NonNull Anything payload);

  boolean delete(@NonNull Timer timer);

  List<Timer> getExpiredTimers();

  long countExpiredTimers();
}
