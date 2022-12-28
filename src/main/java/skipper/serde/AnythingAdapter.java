package skipper.serde;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import lombok.val;
import skipper.common.Anything;

public class AnythingAdapter extends TypeAdapter<Anything> {
  @Override
  public void write(JsonWriter jsonWriter, Anything anything) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("type");
    new StrongTypeAdapter().write(jsonWriter, anything.getType());
    jsonWriter.name("value");
    new Gson().getAdapter(Object.class).write(jsonWriter, anything.getValue());
    jsonWriter.endObject();
  }

  @Override
  public Anything read(JsonReader jsonReader) throws IOException {
    val builder = Anything.builder();
    jsonReader.beginObject();
    StrongType type = null;

    while (jsonReader.hasNext() && !jsonReader.peek().equals(JsonToken.END_OBJECT)) {
      switch (jsonReader.nextName()) {
        case "type":
          type = new StrongTypeAdapter().read(jsonReader);
          builder.type(type);
          break;
        case "value":
          if (type == null) {
            throw new IllegalArgumentException(
                "type must be provided before value in the json string");
          }
          TypeToken tt = null;
          if (type.getParameters() != null) {
            val params = type.getParameters().stream().map(p -> (Type) p).toArray(Type[]::new);
            tt = TypeToken.getParameterized(type.getType(), params);
          } else {
            tt = TypeToken.getParameterized(type.getType());
          }
          builder.value(new Gson().getAdapter(tt).read(jsonReader));
          break;
      }
    }
    jsonReader.endObject();
    return builder.build();
  }
}
