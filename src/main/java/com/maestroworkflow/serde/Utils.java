package com.maestroworkflow.serde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maestroworkflow.models.Anything;
import lombok.Getter;

public class Utils {
  @Getter
  private static final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(Class.class, new ClassTypeAdapter())
          .registerTypeAdapter(StrongType.class, new StrongTypeAdapter())
          .registerTypeAdapter(Anything.class, new AnythingAdapter())
          .create();

  private Utils() {}
}
