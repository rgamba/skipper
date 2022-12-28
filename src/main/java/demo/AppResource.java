package demo;

import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.api.WorkflowCreationRequest;
import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.WorkflowInstance;
import com.maestroworkflow.models.WorkflowType;
import java.util.ArrayList;
import java.util.UUID;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.NonNull;
import lombok.val;

@Path("/workflow")
@Produces(MediaType.APPLICATION_JSON)
public class AppResource {
  private final MaestroEngine engine;

  public AppResource(@NonNull MaestroEngine engine) {
    this.engine = engine;
  }

  @GET
  public WorkflowInstance getWorkflowInstance(@QueryParam("id") String id) {
    return engine.getWorkflowInstance(id);
  }

  @POST
  public WorkflowInstance createWorkflowInstance() {
    val request =
        WorkflowCreationRequest.builder()
            .workflowType(new WorkflowType(TransferWorkflow.class))
            .correlationId(UUID.randomUUID().toString())
            .arguments(
                new ArrayList<Anything>() {
                  {
                    add(Anything.of("Ricardo"));
                    add(Anything.of("Paola"));
                    add(Anything.of(100));
                  }
                })
            .build();
    val response = engine.createWorkflowInstance(request);
    new Thread(
            () -> {
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              engine.executeSignalConsumer(
                  response.getWorkflowInstance().getId(),
                  "approveTransfer",
                  new ArrayList<Anything>() {
                    {
                      add(Anything.of(Boolean.TRUE));
                    }
                  });
            })
        .start();
    return response.getWorkflowInstance();
  }
}
