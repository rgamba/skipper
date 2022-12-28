package skipper.api;

import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.SneakyThrows;

public class Promise implements Callable<Object> {
  private final Callable<Object> callable;

  public Promise(@NonNull Callable<Object> callable) {
    this.callable = callable;
  }

  public static Promise from(@NonNull Callable<Object> callable) {
    return new Promise(callable);
  }

  public static Promise of(Consumer operation) {
    return new Promise(
        () -> {
          operation.apply();
          return null;
        });
  }

  public static <A1> Promise of(Consumer1<A1> operation, A1 arg1) {
    return new Promise(
        () -> {
          operation.apply(arg1);
          return null;
        });
  }

  public static <A1, A2> Promise of(Consumer2<A1, A2> operation, A1 arg1, A2 arg2) {
    return new Promise(
        () -> {
          operation.apply(arg1, arg2);
          return null;
        });
  }

  public static <A1, A2, A3> Promise of(
      Consumer3<A1, A2, A3> operation, A1 arg1, A2 arg2, A3 arg3) {
    return new Promise(
        () -> {
          operation.apply(arg1, arg2, arg3);
          return null;
        });
  }

  public static <A1, A2, A3, A4> Promise of(
      Consumer4<A1, A2, A3, A4> operation, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return new Promise(
        () -> {
          operation.apply(arg1, arg2, arg3, arg4);
          return null;
        });
  }

  // Functions
  public static <A1> Promise of(Func1<A1, ?> operation, A1 arg1) {
    return new Promise(
        () -> {
          return operation.apply(arg1);
        });
  }

  public static <A1, A2> Promise of(Func2<A1, A2, ?> operation, A1 arg1, A2 arg2) {
    return new Promise(
        () -> {
          return operation.apply(arg1, arg2);
        });
  }

  public static <A1, A2, A3> Promise of(Func3<A1, A2, A3, ?> operation, A1 arg1, A2 arg2, A3 arg3) {
    return new Promise(
        () -> {
          return operation.apply(arg1, arg2, arg3);
        });
  }

  public static <A1, A2, A3, A4> Promise of(
      Func4<A1, A2, A3, A4, ?> operation, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    return new Promise(
        () -> {
          return operation.apply(arg1, arg2, arg3, arg4);
        });
  }

  @SneakyThrows
  public Object get() {
    return call();
  }

  @Override
  public Object call() throws Exception {
    return callable.call();
  }

  @FunctionalInterface
  public interface Func<R> {
    R apply();
  }

  @FunctionalInterface
  public interface Func1<T1, R> {
    R apply(T1 t1);
  }

  @FunctionalInterface
  public interface Func2<T1, T2, R> {
    R apply(T1 t1, T2 t2);
  }

  @FunctionalInterface
  public interface Func3<T1, T2, T3, R> {
    R apply(T1 t1, T2 t2, T3 t3);
  }

  @FunctionalInterface
  public interface Func4<T1, T2, T3, T4, R> {
    R apply(T1 t1, T2 t2, T3 t3, T4 t4);
  }

  @FunctionalInterface
  public interface Consumer {
    void apply();
  }

  @FunctionalInterface
  public interface Consumer1<T1> {
    void apply(T1 t1);
  }

  @FunctionalInterface
  public interface Consumer2<T1, T2> {
    void apply(T1 t1, T2 t2);
  }

  @FunctionalInterface
  public interface Consumer3<T1, T2, T3> {
    void apply(T1 t1, T2 t2, T3 t3);
  }

  @FunctionalInterface
  public interface Consumer4<T1, T2, T3, T4> {
    void apply(T1 t1, T2 t2, T3 t3, T4 t4);
  }

  @FunctionalInterface
  public interface Consumer5<T1, T2, T3, T4, T5> {
    void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
  }

  @FunctionalInterface
  public interface Consumer6<T1, T2, T3, T4, T5, T6> {
    void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
  }
}
