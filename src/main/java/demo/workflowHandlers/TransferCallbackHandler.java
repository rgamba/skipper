package demo.workflowHandlers;

import demo.workflows.TransferWorkflow;
import lombok.NonNull;
import skipper.SkipperEngine;
import skipper.api.CallbackHandler;
import skipper.models.WorkflowInstance;

public class TransferCallbackHandler implements CallbackHandler {

  @Override
  public void handleUpdate(
      @NonNull WorkflowInstance workflowInstance, @NonNull SkipperEngine engine) {
    if (workflowInstance.getStatus().isCompleted()) {
      TransferWorkflow.TransferResult result =
          (TransferWorkflow.TransferResult) workflowInstance.getResult().getValue();
      System.out.printf("\n\n>> Transfer result received: %s\n\n", result);
    } else if (workflowInstance.getStatus().isError()) {
      System.out.printf("\n\n>> Transfer error: %s\n\n", workflowInstance.getStatusReason());
    }
  }
}
