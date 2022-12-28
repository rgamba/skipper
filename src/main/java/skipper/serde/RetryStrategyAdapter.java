package skipper.serde;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import lombok.SneakyThrows;
import skipper.models.RetryStrategy;

public class RetryStrategyAdapter extends TypeAdapter<RetryStrategy> {
  @Override
  public void write(JsonWriter jsonWriter, RetryStrategy strategy) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("clazz");
    jsonWriter.value(strategy.getClass().getName());
    jsonWriter.name("object");
    new Gson().getAdapter(Object.class).write(jsonWriter, strategy);
    jsonWriter.endObject();
  }

  @Override
  @SneakyThrows
  public RetryStrategy read(JsonReader jsonReader) throws IOException {
    RetryStrategy object = null;
    Class<? extends RetryStrategy> clazz = null;
    jsonReader.beginObject();
    while (jsonReader.hasNext() && !jsonReader.peek().equals(JsonToken.END_OBJECT)) {
      switch (jsonReader.nextName()) {
        case "clazz":
          clazz = Class.forName(jsonReader.nextString()).asSubclass(RetryStrategy.class);
          break;
        case "object":
          if (clazz == null) {
            throw new IllegalArgumentException(
                "clazz must be provided before value in the json string");
          }
          object = new Gson().getAdapter(clazz).read(jsonReader);
          break;
      }
    }
    if (clazz == null) {
      throw new IllegalArgumentException("clazz must be provided before value in the json string");
    }
    jsonReader.endObject();
    return clazz.cast(object);
  }
}
