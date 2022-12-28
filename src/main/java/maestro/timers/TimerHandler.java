package maestro.timers;

import lombok.NonNull;
import maestro.MaestroEngine;

public interface TimerHandler {
  void process(Object payload, @NonNull MaestroEngine engine);
}
