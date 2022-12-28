package io.github.rgamba.skipper.common;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
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
  public void testAnythingWhenTypeRequiresParametersAndTypeListSoItsInferred() {
    List<User> list = new ArrayList<>();
    List<Foo> foos = new ArrayList<>();
    foos.add(new Foo("123"));
    list.add(new User("ric", foos));

    Anything result = new Anything(list);
    assertEquals(result.getType().getParameters().size(), 1);
    assertEquals(result.getType().getParameters().get(0), User.class);
  }

  @Test
  public void testAnythingWhenTypeRequiresParametersAndTypeMapSoItsInferred() {
    Map<String, User> map = new HashMap<>();
    List<Foo> foos = new ArrayList<>();
    foos.add(new Foo("123"));
    map.put("first", new User("ric", foos));

    Anything result = new Anything(map);
    assertEquals(result.getType().getParameters().size(), 2);
    assertEquals(result.getType().getParameters().get(0), String.class);
    assertEquals(result.getType().getParameters().get(1), User.class);
  }

  @Test
  public void testAnythingWhenTypeRequiresParametersAndTypeCannotBeInferred() {
    Gen<String> gen = new Gen<>("test");

    val err = assertThrows(IllegalArgumentException.class, () -> new Anything(gen));
    assertTrue(err.getMessage().contains("expected 1 parameter types but got 0"));
  }

  @Test
  public void testAnythingWhenTypeRequiresParametersAndTypeCannotBeInferredButIsProvided() {
    Gen<String> gen = new Gen<>("test");

    Anything result = new Anything(gen, String.class);
    assertEquals(result.getType().getParameters().size(), 1);
    assertEquals(result.getType().getParameters().get(0), String.class);
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

  @Test
  public void testAnythingOfNullFails() {
    assertThrows(NullPointerException.class, () -> Anything.of(null));
  }

  @Test
  public void testWhenTypeIsPrimitive() {
    boolean b = true;
    Anything a = Anything.of(b);
  }

  private static class Gen<T> {
    T test;

    Gen(T test) {
      this.test = test;
    }
  }
}
