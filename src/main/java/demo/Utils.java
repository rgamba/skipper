package demo;

import java.util.concurrent.ThreadLocalRandom;

public class Utils {
  public static void randomFail() {
    if (ThreadLocalRandom.current().nextInt(10) <= 3) {
      throw new RuntimeException("something went wrong!");
    }
  }

  public static void randomSleep() {
    try {
      Thread.sleep(ThreadLocalRandom.current().nextInt(2000));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
