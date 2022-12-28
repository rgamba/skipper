package io.github.rgamba.skipper.timers;

import io.github.rgamba.skipper.SkipperEngine;
import lombok.NonNull;

public interface TimerHandler {
  void process(Object payload, @NonNull SkipperEngine engine);
}
