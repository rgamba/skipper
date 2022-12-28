# Creating your first Workflow Instance

Now that we have created and registered `TransferWorkflow` in the workflow engine, we can go ahead and
create our first `workflow instance`. As its name suggests, a workflow instance is an instance of a particular
workflow. In our case this workflow happens to be `TransferWorkflow`. In other words, a workflow instance represents
a full execution of the workflow method of `TransferWorkflow` with the arguments provided at workflow instance
creation time.

## Instantiating the workflow client

The first step is to create an instance of the workflow engine client. This client will provide a friendly API to
interact with the workflow engine.

In our `main` function, add the following code:

```java
SkipperClient workflowClient = new SkipperClient(engine);
```

Next, we invoke workflow instance creation:

```java
WorkflowCreationResponse response = workflowClient.createWorkflowInstance(
        TransferWorkflow.class, // First argument is the classname of the workflow type
        "transfer-001", // This is the correlation ID. A client-generated id to locate this workflow instance
        "fromAccount", // These are the arguments of the workflow method. This one maps to the "fromAccount" arg
        "toAccount", // This maps to the "toAccount" argument on the workflow method
        100 // Finally this maps to the third workflow argument "amount"
);
```

If we inspect the `response` immediately after, we'll notice we have a `response.workflowInstance`. This is the
newly created workflow instance. It is important to persist the `response.workflowInstance.getId()` to query the
workflow instance later on. 

Note that the workflow is not executed synchronously, which means that the `response` will contain a workflow instance
that is currently in `ACTIVE` status, which means that the actual execution (and result) of the workflow are not
yet completed.

## Queriying a workflow instance

Once we have a workflow instance ID, we can check the status (and the result) of the workflow. This is a far from
ideal way of checking the status of a workflow and waiting for it to complete. It should convey the idea at least:

```java

WorkflowInstance instance = response.workflowInstance;
while (instance.getStatus == WorkflowInstance.Status.ACTIVE) {
    Thread.sleep(500);
    instance = client.getWorkflowInstance(response.workflowInstance.getId());
    if (instance.getStatus().isCompleted()) {
        // Since the return type cannot be downcasted automatically by the engine, we need to manually cast.
        boolean result = (boolean) instance.getResult().getValue();
        System.out.println("The workflow completed with result: %s", result);
    } else if (instance.getStatus().isError()) {
        System.out.println("Workflow failed to complete: %s", instance.getStatusReason());    
    }
}

```

There are other, much more efficient ways of getting notified about the result of a workflow instance. See `CallbackHandlers`
for more details on how to do that.
