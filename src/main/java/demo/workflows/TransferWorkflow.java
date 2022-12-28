package demo.workflows;

import demo.operations.Operations;
import demo.services.LedgerError;
import java.time.Duration;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import skipper.OperationProxyFactory;
import skipper.api.*;
import skipper.api.annotations.StateField;
import skipper.api.annotations.WorkflowMethod;
import skipper.models.FixedRetryStrategy;

public class TransferWorkflow implements SkipperWorkflow {
  public static String SYSTEM_ACCOUNT = "system";
  public static Integer AMOUNT_APPROVAL_THRESHOLD = 100;

  private final Operations operations =
      OperationProxyFactory.create(
          Operations.class,
          OperationConfig.builder()
              .retryStrategy(
                  FixedRetryStrategy.builder()
                      .retryDelay(Duration.ofSeconds(2))
                      .maxRetries(2)
                      .build())
              .build());
  public final ApprovalWorkflow approvalWorkflow =
      OperationProxyFactory.create(
          ApprovalWorkflow.class, OperationConfig.builder().timeout(Duration.ofMinutes(1)).build());

  @StateField Boolean approvalRequired = false;

  @WorkflowMethod
  public TransferResult transfer(@NonNull String from, @NonNull String to, int amount) {
    validateAmount(amount);
    int transferFee = transferFee(amount);
    Saga saga = new Saga();

    try {
      if (amount >= AMOUNT_APPROVAL_THRESHOLD) {
        approvalRequired = true;
        if (!approvalWorkflow.getApproval(from, amount)) {
          return new TransferResult(false, "unable to get transfer approval");
        }
      }
      val debitAuthCode = operations.withdraw(from, amount + transferFee, genIdempotencyToken());
      saga.addCompensation(operations::rollbackWithdraw, debitAuthCode, genIdempotencyToken());
      val creditAuthCode = operations.deposit(to, amount, genIdempotencyToken());
      saga.addCompensation(operations::rollbackDeposit, creditAuthCode, genIdempotencyToken());
      val systemCreditAuthCode =
          operations.deposit(SYSTEM_ACCOUNT, transferFee, genIdempotencyToken());
      saga.addCompensation(
          operations::rollbackDeposit, systemCreditAuthCode, genIdempotencyToken());
      return new TransferResult(true, "transfer completed successfully");
    } catch (LedgerError | OperationError e) {
      // Attempt compensating actions. No error handling here, in case any of the compensations
      // fail, we need manual intervention.
      saga.compensate();
      return new TransferResult(
          false,
          String.format("unexpected error when trying to complete transfer: %s", e.getMessage()));
    }
  }

  private void validateAmount(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
  }

  private int transferFee(int amount) {
    return (int) Math.round(amount * .1);
  }

  @Value
  public static class TransferResult {
    boolean isSuccess;
    String message;
  }
}
