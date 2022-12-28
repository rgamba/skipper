package demo.operations;

import com.google.inject.Inject;
import demo.services.Ledger;
import demo.services.LedgerError;
import lombok.NonNull;
import lombok.val;

public class Operations {

  private final Ledger ledger;

  public Operations() {
    ledger = null;
  }

  @Inject
  public Operations(@NonNull Ledger ledger) {
    this.ledger = ledger;
  }

  public String withdraw(String accountId, Integer amount, String idempotencyToken)
      throws LedgerError {
    return ledger.withdraw(accountId, amount, "transfer sent", idempotencyToken);
  }

  public boolean rollbackWithdraw(String creditId, String idempotencyKey) throws LedgerError {
    val transaction = ledger.getTransaction(creditId);
    ledger.deposit(
        transaction.getUserId(), transaction.getAmount(), "transfer send rollback", idempotencyKey);
    return true;
  }

  public String deposit(String account, Integer amount, String idempotencyKey) throws LedgerError {
    return ledger.deposit(account, amount, "transfer received", idempotencyKey);
  }

  public boolean rollbackDeposit(String debitId, String idempotencyKey) {
    val transaction = ledger.getTransaction(debitId);
    ledger.withdraw(
        transaction.getUserId(),
        transaction.getAmount(),
        "transfer receive rollback",
        idempotencyKey);
    return true;
  }

  public void notifyApprovalRequest(String account, Integer amount) {
    return;
  }
}
