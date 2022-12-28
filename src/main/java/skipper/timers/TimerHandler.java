package skipper.timers;

import lombok.NonNull;
import skipper.SkipperEngine;

public interface TimerHandler {
  void process(Object payload, @NonNull SkipperEngine engine);
}
