package io.github.rgamba.skipper.serde;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class StrongType {
  @NonNull Class<?> type;
  List<Class<?>> parameters;
}
