package io.github.rgamba.skipper.serde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.FixedRetryStrategy;
import io.github.rgamba.skipper.models.NoRetry;
import io.github.rgamba.skipper.models.RetryStrategy;
import java.time.Duration;
import lombok.Getter;

public class SerdeUtils {
  @Getter
  private static final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(Class.class, new ClassTypeAdapter())
          .registerTypeAdapter(StrongType.class, new StrongTypeAdapter())
          .registerTypeAdapter(Anything.class, new AnythingAdapter())
          .registerTypeAdapter(NoRetry.class, new RetryStrategyAdapter())
          .registerTypeAdapter(FixedRetryStrategy.class, new RetryStrategyAdapter())
          .registerTypeAdapter(RetryStrategy.class, new RetryStrategyAdapter())
          .registerTypeAdapter(Duration.class, new DurationAdapter())
          .create();

  private SerdeUtils() {}
}
