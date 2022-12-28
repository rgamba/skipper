package skipper.admin;

import java.util.*;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import skipper.SkipperEngine;
import skipper.models.OperationRequest;
import skipper.models.OperationResponse;
import skipper.models.WorkflowInstance;

@Path("/admin")
public class AdminResource {
  @Value
  public static class ExecutionTrace {
    @NonNull OperationRequest request;
    OperationResponse response;
  }

  private final SkipperEngine engine;

  public AdminResource(@NonNull SkipperEngine engine) {
    this.engine = engine;
  }

  @GET
  @Path("/")
  @Produces("text/html;charset=UTF-8")
  public IndexView index() {
    return new IndexView("index.ftl");
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow-instances")
  public List<WorkflowInstance> getWorkflowInstances() {
    return engine.findWorkflowInstances();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow-instances/{id}")
  public WorkflowInstance getWorkflowInstance(@PathParam("id") String workflowInstanceId) {
    return engine.getWorkflowInstance(workflowInstanceId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow-instances/by-correlation-id/{id}")
  public WorkflowInstance getWorkflowInstanceByCorrelationId(
      @PathParam("id") String workflowInstanceId) {
    return engine.getWorkflowInstanceByCorrelationId(workflowInstanceId).get();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow-instances/{id}/operation-responses")
  public List<ExecutionTrace> getWorkflowInstanceOperationResponses(
      @PathParam("id") String workflowInstanceId) {
    val requests = engine.getWorkflowInstanceOperationRequests(workflowInstanceId);
    val responses = engine.getWorkflowInstanceOperationResults(workflowInstanceId);
    Set<String> usedRequestIds = new HashSet<>();

    List<ExecutionTrace> result =
        responses.stream()
            .map(
                response -> {
                  val req =
                      requests.stream()
                          .filter(
                              r ->
                                  r.getOperationRequestId()
                                      .equals(response.getOperationRequestId()))
                          .findFirst()
                          .get();
                  usedRequestIds.add(req.getOperationRequestId());
                  return new ExecutionTrace(req, response);
                })
            .collect(Collectors.toList());

    requests.stream()
        .filter(req -> !usedRequestIds.contains(req.getOperationRequestId()))
        .forEach(req -> result.add(new ExecutionTrace(req, null)));
    return result;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow-instances/{id}/replay")
  public void replayWorkflowInstance(@PathParam("id") String workflowInstanceId) {
    engine.retryFailedWorkflowInstance(workflowInstanceId);
  }
}
