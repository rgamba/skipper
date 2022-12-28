package skipper.api;

import java.util.ArrayList;
import java.util.List;

public class Saga {
  private final List<Promise> compensations = new ArrayList<>();

  public <A1> void addCompensation(Promise.Func1<A1, ?> operation, A1 arg1) {
    compensations.add(Promise.of(operation, arg1));
  }

  public <A1, A2> void addCompensation(Promise.Func2<A1, A2, ?> operation, A1 arg1, A2 arg2) {
    compensations.add(Promise.of(operation, arg1, arg2));
  }

  public <A1, A2, A3> void addCompensation(
      Promise.Func3<A1, A2, A3, ?> operation, A1 arg1, A2 arg2, A3 arg3) {
    compensations.add(Promise.of(operation, arg1, arg2, arg3));
  }

  public <A1, A2, A3, A4> void addCompensation(
      Promise.Func4<A1, A2, A3, A4, ?> operation, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    compensations.add(Promise.of(operation, arg1, arg2, arg3, arg4));
  }

  public void addCompensation(Promise.Consumer operation) {
    compensations.add(Promise.of(operation));
  }

  public <A1> void addCompensation(Promise.Consumer1<A1> operation, A1 arg1) {
    compensations.add(Promise.of(operation, arg1));
  }

  public <A1, A2> void addCompensation(Promise.Consumer2<A1, A2> operation, A1 arg1, A2 arg2) {
    compensations.add(Promise.of(operation, arg1, arg2));
  }

  public <A1, A2, A3> void addCompensation(
      Promise.Consumer3<A1, A2, A3> operation, A1 arg1, A2 arg2, A3 arg3) {
    compensations.add(Promise.of(operation, arg1, arg2, arg3));
  }

  public <A1, A2, A3, A4> void addCompensation(
      Promise.Consumer4<A1, A2, A3, A4> operation, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
    compensations.add(Promise.of(operation, arg1, arg2, arg3, arg4));
  }

  public List<Object> compensate() {
    Promise[] arr = new Promise[compensations.size()];
    return Control.join(compensations.toArray(arr));
  }
}
