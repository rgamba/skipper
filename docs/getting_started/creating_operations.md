# Creating your first operation

Before diving into creating workflows, we will start off by creating our first workflow operation.

As mentioned earlier, Skipper does not prescribe a specific format or schema when it comes to defining workflow
operations. The only 2 guidelines to know about workflow operations are:

1. Workflow operations must be idempotent
2. The workflow operation method must throw a **checked** exception for non-retriable exceptions. All other
exceptions will be treated as *retriable errors*.

Throughout this guide, we will be creating a demo workflow to enable a super simplified version of P2P transfers.
In order to enable P2P transfers, we need to expose 2 operations: `debit` and `credit`. (debit takes money out of
an account and credit deposits money into an account).

```java
public class TransferOperations {
    public TransferOperations() {} // No-args constructor is required by Skipper, will fix in the future
    
    public boolean credit(String account, long amount) {
        // Real implementations will implement all the logic needed to actually perform the operation here
        // by calling other dependencies, read/write to databases, etc. for our demo, we are just going to 
        // return true immediately.
        return true;
    }
    
    // Note that all expected exceptions must be checked. All other exceptions will be considered unexpected errors
    // and will be retried by skipper according to the retry policy.
    // In this case, not having enough balance is an app-level error that should be expected (retries will be of no use)
    // so it must be checked.
    public boolean debit(String account, long amount) throws NotEnouthBalance {
        return true;
    }
}
```

That's it! You've authored your first 2 operations! As you can see, your operations are completely decoupled from
any Skipper APIs.

## Registering your operation in the `DependencyRegistry`

The final step is registering our newly created operation in the registry. Update the `main` function we created in
the previous step, and replace the registry initiation step with:

```java
DependencyRegistry registry = DependencyRegistry.builder()
        .addOperation(() -> injector.getInstance(TransferOperations.class))
        .build();
```

## Next steps

[Learn how to create workflow definitions](creating_workflows.md)