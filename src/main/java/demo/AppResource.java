package demo;

import java.util.UUID;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.NonNull;
import lombok.val;
import maestro.client.MaestroClient;
import maestro.models.WorkflowInstance;

@Path("/workflow")
@Produces(MediaType.APPLICATION_JSON)
public class AppResource {
  private final MaestroClient client;

  public AppResource(@NonNull MaestroClient engine) {
    this.client = engine;
  }

  @GET
  public WorkflowInstance getWorkflowInstance(@QueryParam("id") String id) {
    return client.getWorkflowInstance(id);
  }

  @POST
  public WorkflowInstance createWorkflowInstance() {
    val response =
        client.createWorkflowInstance(
            TransferWorkflow.class, UUID.randomUUID().toString(), 1, "Paola", 100);
    new Thread(
            () -> {
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              client.sendInputSignal(
                  response.getWorkflowInstance().getId(), "approveTransfer", Boolean.TRUE);
            })
        .start();
    return response.getWorkflowInstance();
  }
}
