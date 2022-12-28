package maestro.serde;

import static org.junit.Assert.assertEquals;

import com.google.gson.GsonBuilder;
import lombok.val;
import maestro.models.NoRetry;
import maestro.models.RetryStrategy;
import org.junit.Test;

public class RetryStrategyAdapterTest {
  @Test
  public void testOpConfig() {
    RetryStrategy retryStrategy = new NoRetry();
    val gson =
        new GsonBuilder().registerTypeAdapter(NoRetry.class, new RetryStrategyAdapter()).create();
    val ser = gson.toJson(retryStrategy);
    val deser = gson.fromJson(ser, NoRetry.class);
    assertEquals(retryStrategy, deser);
  }
}
