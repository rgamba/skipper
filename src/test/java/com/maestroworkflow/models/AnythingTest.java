package com.maestroworkflow.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.junit.Test;

public class AnythingTest {
  @Value
  private static class User {
    @NonNull String name;
    List<Foo> foos;
  }

  @Value
  private static class Foo {
    String bar;
  }

  @Test
  public void testAnythingWhenTypeRequiresParameters() {
    List<User> list = new ArrayList<>();
    List<Foo> foos = new ArrayList<>();
    foos.add(new Foo("123"));
    list.add(new User("ric", foos));

    assertThrows(IllegalArgumentException.class, () -> new Anything(list));
  }

  @Test
  public void testAnythingWhenTypeRequiresParametersAndParametersAreSent() {
    List<User> list = new ArrayList<>();
    List<Foo> foos = new ArrayList<>();
    foos.add(new Foo("123"));
    list.add(new User("ric", foos));

    new Anything(list, User.class);
  }

  @Test
  public void testAnythingWhenTypeIsNotGenericButParametersAreSent() {
    assertThrows(IllegalArgumentException.class, () -> new Anything("hello", User.class));
  }

  @Test
  public void testAnythingHappyPath() {
    Anything a = new Anything("test");
    Anything b = new Anything(1);
    List<User> list = new ArrayList<>();
    List<Foo> foos = new ArrayList<>();
    foos.add(new Foo("123"));
    list.add(new User("ric", foos));
    Anything c = new Anything(list, User.class);

    assertEquals(String.class, a.getClazz());
    assertEquals(Integer.class, b.getClazz());
    assertEquals(ArrayList.class, c.getClazz());
  }
}
