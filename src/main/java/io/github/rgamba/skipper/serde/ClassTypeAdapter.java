package io.github.rgamba.skipper.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public class ClassTypeAdapter extends TypeAdapter<Class> {
  @Override
  public void write(JsonWriter jsonWriter, Class aClass) throws IOException {
    jsonWriter.value(aClass.getName());
  }

  @Override
  public Class read(JsonReader jsonReader) throws IOException {
    String clazz = jsonReader.nextString();
    try {
      return Class.forName(clazz);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("unable to deserialize class type: " + clazz, e);
    }
  }
}
