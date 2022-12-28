package maestro.api;

public class OperationError extends RuntimeException {
  public OperationError(Throwable cause) {
    super(cause);
  }
}
