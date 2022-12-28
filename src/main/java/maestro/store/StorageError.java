package maestro.store;

import lombok.Getter;

public class StorageError extends RuntimeException {
  @Getter private final Type type;

  public StorageError(String error) {
    super(error);
    type = Type.UNKNOWN;
  }

  public StorageError(String message, Throwable error) {
    this(message, error, Type.UNKNOWN);
  }

  public StorageError(String message, Throwable error, Type type) {
    super(message, error);
    this.type = type;
  }

  public enum Type {
    DUPLICATE_ENTRY,
    UNKNOWN
  }
}
