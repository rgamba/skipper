package com.maestroworkflow.timers;

import com.maestroworkflow.MaestroEngine;
import lombok.NonNull;

public interface TimerHandler {
  void process(Object payload, @NonNull MaestroEngine engine);
}
