package com.maestroworkflow.serde;

import static org.junit.Assert.*;

import com.maestroworkflow.models.Anything;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ClassTypeAdapterTest {
  @Test
  public void testClassTypeAdapter() {
    List<String> list = new ArrayList<>();
    list.add("ricardo");
    Anything value = new Anything(list, String.class);
    String ser = Utils.getGson().toJson(value);
    Anything deser = Utils.getGson().fromJson(ser, Anything.class);
    assertEquals(value, deser);
  }

  @Test
  public void testClassTypeAdapterWhenDeserAnInvalidClassName() {
    String ser = "\"invalidclass\"";
    Exception error =
        assertThrows(RuntimeException.class, () -> Utils.getGson().fromJson(ser, Class.class));
    assertTrue(error.getMessage().contains("unable to deserialize class"));
  }
}
