package io.github.rgamba.skipper.store;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.NonNull;

public final class DateTimeUtil {
  private DateTimeUtil() {}

  public static Timestamp instantToTimestamp(@NonNull Instant instant) {
    return Timestamp.from(truncateInstant(instant));
  }

  public static Instant truncateInstant(@NonNull Instant instant) {
    return instant.truncatedTo(ChronoUnit.SECONDS);
  }
}
