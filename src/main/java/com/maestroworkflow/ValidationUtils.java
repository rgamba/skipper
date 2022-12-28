package com.maestroworkflow;

import java.util.Formatter;

public class ValidationUtils {
  public static Condition when(boolean condition) {
    return new Condition(condition);
  }

  public static Condition require(boolean condition) {
    return new Condition(condition);
  }

  public static class Condition {
    private boolean condition;

    private Condition(boolean condition) {
      this.condition = condition;
    }

    public void thenExpect(boolean expectation, String errorMsg, Object... args) {
      if (condition && !expectation) {
        throw new IllegalArgumentException(new Formatter().format(errorMsg, args).toString());
      }
    }

    public void orFail(String errorMsg, Object... args) {
      if (!condition) {
        throw new IllegalArgumentException(new Formatter().format(errorMsg, args).toString());
      }
    }
  }
}
