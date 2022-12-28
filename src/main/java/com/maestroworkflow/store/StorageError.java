package com.maestroworkflow.store;

public class StorageError extends RuntimeException {
  public StorageError(String error) {
    super(error);
  }

  public StorageError(Throwable error) {
    super(error);
  }

  public StorageError(String message, Throwable error) {
    super(message, error);
  }
}
