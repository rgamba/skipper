package skipper.serde;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.junit.Test;

public class DurationAdapterTest {
  @Test
  public void testDurationAdapter() {
    Duration duration = Duration.ofMillis(1001);
    String ser = SerdeUtils.getGson().toJson(duration);
    Duration deser = SerdeUtils.getGson().fromJson(ser, Duration.class);
    assertEquals(duration, deser);
  }
}
