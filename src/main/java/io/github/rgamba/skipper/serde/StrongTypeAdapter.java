package io.github.rgamba.skipper.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.val;

public class StrongTypeAdapter extends TypeAdapter<StrongType> {
  @Override
  public void write(JsonWriter jsonWriter, StrongType type) throws IOException {
    jsonWriter.beginObject();
    jsonWriter.name("type");
    jsonWriter.value(type.getType().getName());
    if (type.getParameters() != null && !type.getParameters().isEmpty()) {
      jsonWriter.name("params");
      jsonWriter.beginArray();
      for (val param : type.getParameters()) {
        jsonWriter.value(param.getName());
      }
      jsonWriter.endArray();
    }
    jsonWriter.endObject();
  }

  @Override
  public StrongType read(JsonReader jsonReader) throws IOException {
    val type = StrongType.builder();
    try {
      jsonReader.beginObject();
      while (jsonReader.hasNext() && !jsonReader.peek().equals(JsonToken.END_OBJECT)) {
        switch (jsonReader.nextName()) {
          case "type":
            type.type(Class.forName(jsonReader.nextString()));
            break;
          case "params":
            jsonReader.beginArray();
            List<Class<?>> params = new ArrayList<>();
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
              params.add(Class.forName(jsonReader.nextString()));
            }
            jsonReader.endArray();
            type.parameters(params);
            break;
        }
      }
      jsonReader.endObject();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("unable to deserialize class type", e);
    }
    return type.build();
  }
}
