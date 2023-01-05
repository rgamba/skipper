# skipper

Skipper is a stateful workflow engine framework library that allows you to define workflow as code without having to worry
about the underlying complexities of state management.

Skipper is designed with simplicity in mind. It is very lightweight and will run alongside your application binary and
its only dependency is a datastore (for now MySQL is the only supported datastore).

[Read the Getting Started Guide](docs/index.md) to learn more about skipper.

Or jump straight into the [Skipper Demo](https://github.com/rgamba/skipper_demo) to see it in action.

## Maven

```xml
<dependency>
  <groupId>io.github.rgamba</groupId>
  <artifactId>skipper</artifactId>
  <version>0.0.1</version>
</dependency>
```

## Sneak Peek

So how exactly does one define a workflow using Skipper? The following example is a simplified version of a peer-to-peer transfer workflow modeled using Skipper.

As you'll see, a skipper workflow is just a regular Java function! It takes in zero or more arguments as input, performs some business logic and outputs a result (optionally).

```java
@WorkflowMethod
public TransferResult transfer(String from, String to, int amount) {
  int transferFee = transferFee(amount);
  Saga saga = new Saga();
  try {
    // 1. Debit from source account
    val debitAuthCode = operations.withdraw(from, amount + transferFee);
    saga.addCompensation(operations::rollbackWithdraw, debitAuthCode);
    // 2. Credit to destination account
    val creditAuthCode = operations.deposit(to, amount);
    saga.addCompensation(operations::rollbackDeposit, creditAuthCode);
    // 3. Send transaction fee to system account
    val systemCreditAuthCode =
          operations.deposit(SYSTEM_ACCOUNT, transferFee);
    saga.addCompensation(
          operations::rollbackDeposit, systemCreditAuthCode);
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
```

You might be wondering, _but what if any of the operations fail? What about the retries? Who will take care of eventually taking this workflow to completion?_

The good news is that, as a workflow author, you don't have to worry about any of that. Skipper will take care of all those low-level issues for you, so you can focus on writing business logic for your use-case!

[Read the documentation](docs/index.md) to learn more!