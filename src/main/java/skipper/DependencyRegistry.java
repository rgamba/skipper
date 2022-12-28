package skipper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.*;
import skipper.api.CallbackHandler;
import skipper.api.ChildWorkflowCallbackHandler;
import skipper.api.SkipperWorkflow;

/**
 * Dependency registry is the component that provides all runtime dependencies to the workflow
 * engine.
 *
 * <p>This is the way for the workflow engine to discover all workflow, operation and handler
 * dependencies provided by the client.
 *
 * <p>Beware that modifying the registry by removing a dependency is a breaking change.
 */
public class DependencyRegistry {
  private final Map<Class<? extends SkipperWorkflow>, Supplier<SkipperWorkflow>> workflows;
  private final Map<Class<?>, Object> operations;
  private final Map<Class<? extends CallbackHandler>, CallbackHandler> callbackHandlers;

  private DependencyRegistry(
      @NonNull Map<Class<? extends SkipperWorkflow>, Supplier<SkipperWorkflow>> workflows,
      @NonNull Map<Class<?>, Object> operations,
      @NonNull Map<Class<? extends CallbackHandler>, CallbackHandler> callbackHandlers) {
    workflows.forEach(
        (key, value) -> {
          WorkflowInspector inspector = new WorkflowInspector(key, value.get());
          inspector.getWorkflowMethod(); // Basic validation of the workflow class
          if (value.get() == value.get()) {
            // The factory provided must not return the same object instance, it should instead
            // create a new instance
            // every time. This is a basic check and can be circumvented, but it will cover most
            // cases.
            throw new IllegalArgumentException(
                String.format(
                    "the workflow factory method for workflow '%s' must not be a singleton, it must return a new instance every time",
                    key.getName()));
          }
        });
    callbackHandlers.put(ChildWorkflowCallbackHandler.class, new ChildWorkflowCallbackHandler());
    this.workflows = workflows;
    this.operations = operations;
    this.callbackHandlers = callbackHandlers;
  }

  public <T extends SkipperWorkflow> T getWorkflow(@NonNull Class<T> workflowClassName) {
    if (!workflows.containsKey(workflowClassName)) {
      throw new IllegalArgumentException(
          String.format(
              "workflow '%s' was not found on the workflows registry",
              workflowClassName.getName()));
    }
    return workflowClassName.cast(workflows.get(workflowClassName).get());
  }

  public <T> T getOperation(@NonNull Class<T> operationName) {
    if (!operations.containsKey(operationName)) {
      throw new IllegalArgumentException(
          String.format(
              "operation '%s' was not found on the operations registry", operationName.getName()));
    }
    return operationName.cast(operations.get(operationName));
  }

  public <T extends CallbackHandler> T getCallbackHandler(@NonNull Class<T> callbackHandlerClass) {
    if (!callbackHandlers.containsKey(callbackHandlerClass)) {
      throw new IllegalArgumentException(
          String.format(
              "callback handler '%s' was not found on the callback handlers registry",
              callbackHandlerClass.getName()));
    }
    return callbackHandlerClass.cast(callbackHandlers.get(callbackHandlerClass));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Map<Class<? extends SkipperWorkflow>, Supplier<SkipperWorkflow>> workflows =
        new HashMap<>();
    private final Map<Class<?>, Object> operations = new HashMap<>();
    private final Map<Class<? extends CallbackHandler>, CallbackHandler> callbackHandlers =
        new HashMap<>();

    /**
     * Register a workflow factory. This is the workflow factory that will be used every time a new
     * instance of your workflow needs to be created by the workflow engine. The workflow instance
     * returned by the factory should be a valid workflow object, meaning it should have at least
     * one {@link skipper.api.annotations.WorkflowMethod} annotated method.
     *
     * @param workflow The workflow factory. It is important that your factory returns a **new**
     *     instance every time it is called. Returning a singleton is not allowed!
     */
    public Builder addWorkflowFactory(Supplier<SkipperWorkflow> workflow) {
      this.workflows.put(workflow.get().getClass(), workflow);
      return this;
    }

    /**
     * Register an operation instance on the operation registry.
     *
     * @param operation The workflow instance to be used whenever a request for the given operation
     *     needs to be executed.
     */
    public Builder addOperation(Object operation) {
      this.operations.put(operation.getClass(), operation);
      return this;
    }

    /**
     * Register a callback handler to the registry.
     *
     * @param handler The callback handler instance to be used by the workflow engine every to
     *     notify about updates on the workflow status.
     */
    public Builder addCallbackHandler(CallbackHandler handler) {
      this.callbackHandlers.put(handler.getClass(), handler);
      return this;
    }

    public DependencyRegistry build() {
      return new DependencyRegistry(workflows, operations, callbackHandlers);
    }
  }
}
