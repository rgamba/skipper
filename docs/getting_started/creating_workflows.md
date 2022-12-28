# Creating your first Workflow

A very basic workflow definition would be something like the below example. Note that this impl still has a lot
of room for improvement but our goal here is to illustrate how to author a simple workflow.

```java
public class TransferWorkflow implements SkipperWorkflow {
    private final TransferOperations operations = OperationProxyFactory.create(TransferOperations.class);

    @WorkflowMethod
    public boolean transfer(String fromAccount, String toAccount, long amount) {
        boolean debitSucceeded = operations.debit(fromAccount, amount);
        if (debitSucceeded) {
            boolean creditSucceeded = operations.credit(toAccount, amount);
            return true;
        }
        return false;
    }
}
```

There's a few things to unpack here.

First thing is that the workflow class must implement `SkipperWorkflow`!


The second thing to note is the `operations` field.

```java
private final TransferOperations operations = OperationProxyFactory.create(TransferOperations.class);
```

This field references the operaions we just created on the previous
step. **The most important thing to notice here is that we are NOT instantiating the `TransferOperations` class directly!
We are instead creating a proxy for that class that is not actually going to call the methods directly.**

Next we have the `transfer` method. This is our main workflow method, where all the business logic of the transfers
workflow is going to live. 

```java
@WorkflowMethod
public boolean transfer(String fromAccount, String toAccount, long amount) {
```

Notice how this method is anotated with `@WorkflowMethod`, this is how we let the
engine know this is our main workflow method.

As any other method, our workflow method can take one or more parameters. Beware that because of how skipper works,
**adding or re-ordering the arguments in the method is considered a breaking change**. A good practice to avoid this
breaking change is to just take in a single argument that will then have all other arguments as fields.

The first line of the workflow method invokes the `debit` operation.

```java
boolean debitSucceeded = operations.debit(fromAccount, amount);
```

When the workflow execution reaches a point where an operation needs to be executed, the workflow execution stops and
waits until the operation is completed. In this case, even though it might look like the workflow thread itself is going
to execute the `debit` function directly, in reality the workflow is just going to instruct the engine to schedule the
execution of the `debit` operation, which means that the workflow code will *sleep* and wake up only after the operation
has been completed. (For clarity, the workflow thread won't actually *sleep* until the operation is ready).

The rest of the code is pretty self-explanatory.

## Registering your workflow in the `DependencyRegistry`

Add the following line on the dependency registry builder:

```java
.addWorkflow(() -> injector.getInstance(TransferWorkflow.class))
```





