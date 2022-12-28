package io.github.rgamba.skipper.serde;

import static org.junit.Assert.assertEquals;

import com.google.gson.GsonBuilder;
import io.github.rgamba.skipper.models.NoRetry;
import io.github.rgamba.skipper.models.RetryStrategy;
import lombok.val;
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
