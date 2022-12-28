# Overview

## What is a workflow?

There are lots of definitions of workflows. In the context of Skipper, a workflow is considered to be the logic
that defines and coordinates the execution of a set of pre-defined steps or operations and that is guaranteed
to run to a terminal state.

There are lots of ways that workflows can be defined. Some frameworks use static configuration files to define
the orchestration logic of the workflow, such as JSON, XML, etc. Skipper takes a different approach. Skipper favors code
over configuration, and therefore skipper workflows are defined as code. This has many benefits that we will cover
later on.

For now, just know that the piece of code that defines a workflow in skipper **must** be deterministic.

## What is an operation?

In the context of Skipper, an operation is a function that performs a certain task. In workflow terminology, this is
also known as an *activity*.

Operations are completely decoupled from workflow logic, therefore they are reusable. A few examples of operations are:

- Sending out a transactional email
- Inserting, updating or reading data from a database
- Calling an external webservice

Operations might or might not have a return value and/or side effects. If they do have a return value, this value will
be passed on to the workflow who invoked the operation.

Any class with methods can be considered a skipper operation. Skipper does not prescribe or require the operation to
adhere to any framework or API. The only requirement (hard requirement) is that any method that is to be used as
a skipper operation **must be idempotent**.

