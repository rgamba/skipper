package io.github.rgamba.skipper.api;

public class OperationError extends RuntimeException {
  public OperationError(Throwable cause) {
    super(cause);
  }
}
