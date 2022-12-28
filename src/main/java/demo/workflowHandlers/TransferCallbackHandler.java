package demo.workflowHandlers;

import demo.workflows.TransferWorkflow;
import io.github.rgamba.skipper.SkipperEngine;
import io.github.rgamba.skipper.api.CallbackHandler;
import io.github.rgamba.skipper.models.WorkflowInstance;
import lombok.NonNull;

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
