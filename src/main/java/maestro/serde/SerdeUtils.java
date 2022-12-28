package maestro.serde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import maestro.common.Anything;
import maestro.models.FixedRetryStrategy;
import maestro.models.NoRetry;
import maestro.models.RetryStrategy;

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
          .create();

  private SerdeUtils() {}
}
