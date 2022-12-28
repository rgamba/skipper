package io.github.rgamba.skipper.api;

import io.github.rgamba.skipper.runtime.DecisionThread;
import io.github.rgamba.skipper.runtime.StopWorkflowExecution;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;

public class Timeout extends Promise {
  public Timeout(@NonNull Duration timeout) {
    super(
        () -> {
          Instant lastCheckpoint =
              DecisionThread.getLatestCurrentExecutionCheckpoint()
                  .orElse(DecisionThread.getWorkflowContext().getWorkflowInstanceCreationTime());
          Instant now = DecisionThread.getWorkflowContext().getCurrentTime();
          Instant timeoutEnd = lastCheckpoint.plus(timeout);
          if (now.isBefore(timeoutEnd)) {
            Duration timeDelta = Duration.between(now, timeoutEnd);
            throw new StopWorkflowExecution(timeDelta);
          }
          return null;
        });
  }

  public static Timeout of(@NonNull Duration duration) {
    return new Timeout(duration);
  }
}
