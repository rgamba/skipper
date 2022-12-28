package demo.resources;

import demo.services.Ledger;
import demo.workflowHandlers.TransferCallbackHandler;
import demo.workflows.TransferWorkflow;
import io.github.rgamba.skipper.client.SkipperClient;
import io.github.rgamba.skipper.models.WorkflowInstance;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.NonNull;
import lombok.val;

@Path("/transfers")
@Produces(MediaType.APPLICATION_JSON)
public class AppResource {
  private final SkipperClient client;
  private final Ledger ledger;

  public AppResource(@NonNull SkipperClient engine, @NonNull Ledger ledger) {
    this.client = engine;
    this.ledger = ledger;
  }

  @GET
  @Path("/{id}")
  public WorkflowInstance getWorkflowInstance(@PathParam("id") String id) {
    return client.getWorkflowInstance(id);
  }

  @GET
  @Path("/balances")
  public Map<String, Integer> getBalances() {
    return ledger.getBalances();
  }

  @POST
  @Path("/")
  public WorkflowInstance initiateTransfer(
      @QueryParam("amount") int amount,
      @NonNull @QueryParam("from") String sender,
      @NonNull @QueryParam("to") String receiver) {
    val response =
        client.createWorkflowInstance(
            TransferWorkflow.class,
            UUID.randomUUID().toString(),
            TransferCallbackHandler.class,
            sender,
            receiver,
            amount);
    return response.getWorkflowInstance();
  }

  @POST
  @Path("/{id}/set-approval")
  public void setManualApproval(
      @PathParam("id") String id, @QueryParam("isApproved") boolean isApproved) {
    client.sendInputSignal(id, "approveTransfer", isApproved);
  }
}
