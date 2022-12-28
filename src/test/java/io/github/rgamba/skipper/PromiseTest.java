package io.github.rgamba.skipper;

import static org.junit.Assert.assertEquals;

import io.github.rgamba.skipper.api.Promise;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PromiseTest {
  public static class Foo {
    Integer bar(Integer a) {
      return a;
    }

    String spam(String a) {
      return a;
    }
  }

  @Test
  public void testPromise() throws Exception {
    Foo foo = new Foo();
    Promise promise = new Promise(() -> foo.bar(1));
    assertEquals((Integer) 1, promise.call());

    List<Promise> promises =
        new ArrayList<Promise>() {
          {
            add(new Promise(() -> foo.bar(1)));
            add(new Promise(() -> foo.spam("test")));
          }
        };

    assertEquals(1, promises.get(0).call());
    assertEquals("test", promises.get(1).call());
  }
}
