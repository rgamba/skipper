package demo.services;

public class LedgerError extends RuntimeException {
  public LedgerError(String error) {
    super(error);
  }
}
