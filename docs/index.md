# Skipper

Skipper is a workflow engine that allows you to define reliable stateful workflows as code, without having to worry 
about low level details like error retries, state management, synchronization, etc.

You can think of skipper as the stateful version of async I/O. Typically, async IO APIs abstract away all the complexities
of scheduling so that you can focus on the business logic. Skipper's goal is that the user can define complex workflows
by just focusing on the business logic.

## How to know if skipper is a good option for my use-case?

### Skipper is a good option if:

- Your workflow needs to be guaranteed to run to a terminal state (failing in the middle is not an option).
- Your workflow is a combination of synchronous and asynchronous steps (Think of async as manual approvals or some task that is completed at some point in the future).
- Your workflow is composed of a defined, well known set of steps.
- Your workflow does not need to complete in less than 1 second.
- Your workflow is composed of complex business logic that with forks/branches/joins or even arbitrary waits or sleeps.

### Skipper is not a good option if:

- It is OK for a workflow to fail and delegate retries to the user/upstream caller.
- The workflow needs to be completed in 300ms or less.
- The workflow needs to be executed synchronously.
- The business logic is very simple (1 or 2 sequential steps). Even though skipper could deal with it, a simple task runner will take care of it.
- The number of steps of the workflow are dynamically created are runtime and not know upfront

## Next steps

Ready to learn more? [Read the overview](getting_started/overview.md) to learn more about skipper's concepts.

or ready to get started? [Jump to the installation guide](getting_started/installation.md)!