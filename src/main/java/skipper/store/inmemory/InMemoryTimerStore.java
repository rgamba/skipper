package skipper.store.inmemory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.val;
import skipper.common.Anything;
import skipper.models.Timer;
import skipper.store.TimerStore;

@Singleton
public class InMemoryTimerStore implements TimerStore {
  private final Clock clock;
  private List<Timer> data = new ArrayList<>();
  private final ReentrantLock lock = new ReentrantLock();

  @Inject
  public InMemoryTimerStore(@Named("UTC") Clock clock) {
    this.clock = clock;
  }

  @Override
  public Timer createOrUpdate(@NonNull Timer timer) {
    lock.lock();
    try {
      val timerId = timer.getTimerId();
      val existingTimer = data.stream().filter(t -> t.getTimerId().equals(timerId)).findFirst();
      if (existingTimer.isPresent()) {
        timer = timer.toBuilder().version(existingTimer.get().getVersion() + 1).build();
      }
      data.removeIf(t -> timerId.equals(t.getTimerId()));
      data.add(timer);
    } finally {
      lock.unlock();
    }
    return timer;
  }

  @Override
  public Timer get(@NonNull String timerId) {
    lock.lock();
    try {
      return data.stream()
          .filter(t -> t.getTimerId().equals(timerId))
          .findFirst()
          .orElseThrow(IllegalArgumentException::new);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void update(@NonNull String timerId, Duration timeout) {
    lock.lock();
    try {
      data.replaceAll(
          t -> {
            if (t.getTimerId().equals(timerId)) {
              return t.toBuilder().timeout(clock.instant().plus(timeout)).build();
            }
            return t;
          });
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void update(
      @NonNull String timerId, @NonNull Duration timeout, @NonNull Anything payload) {
    lock.lock();
    try {
      data.replaceAll(
          t -> {
            if (t.getTimerId().equals(timerId)) {
              return t.toBuilder().timeout(clock.instant().plus(timeout)).payload(payload).build();
            }
            return t;
          });
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean delete(@NonNull Timer timer) {
    return data.removeIf(
        t -> t.getTimerId().equals(timer.getTimerId()) && t.getVersion() == timer.getVersion());
  }

  @Override
  public List<Timer> getExpiredTimers() {
    lock.lock();
    try {
      List<Timer> result = new ArrayList<>();
      data =
          data.stream()
              .map(
                  timer -> {
                    if (timer.getTimeout() == null
                        || timer.getTimeout().isBefore(clock.instant())) {
                      result.add(timer);
                      return timer
                          .toBuilder()
                          .timeout(clock.instant().plus(getLeaseDuration()))
                          .build();
                    }
                    return timer;
                  })
              .collect(Collectors.toList());
      return result;
    } finally {
      lock.unlock();
    }
  }

  private Duration getLeaseDuration() {
    return Duration.ofSeconds(30);
  }
}
