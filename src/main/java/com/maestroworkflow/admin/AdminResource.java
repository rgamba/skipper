package com.maestroworkflow.admin;

import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.models.OperationRequest;
import com.maestroworkflow.models.OperationResponse;
import com.maestroworkflow.models.WorkflowInstance;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

@Path("/admin")
public class AdminResource {
  @Value
  public static class ExecutionTrace {
    @NonNull OperationRequest request;
    OperationResponse response;
  }

  private final MaestroEngine engine;

  public AdminResource(@NonNull MaestroEngine engine) {
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
  @Path("/workflow-instances/{id}/operation-responses")
  public List<ExecutionTrace> getWorkflowInstanceOperationResponses(
      @PathParam("id") String workflowInstanceId) {
    val requests = engine.getWorkflowInstanceOperationRequests(workflowInstanceId);
    val responses = engine.getWorkflowInstanceOperationResults(workflowInstanceId);
    val opReqMap =
        requests.stream()
            .collect(
                Collectors.toMap(OperationRequest::getOperationRequestId, Function.identity()));
    Set<String> usedRequests = new HashSet<>();
    List<ExecutionTrace> result =
        responses.stream()
            .map(
                response -> {
                  val req = opReqMap.get(response.getOperationRequestId());
                  usedRequests.add(req.getOperationRequestId());
                  return new ExecutionTrace(req, response);
                })
            .collect(Collectors.toList());

    opReqMap.forEach(
        (opReqId, req) -> {
          if (!usedRequests.contains(opReqId)) {
            result.add(new ExecutionTrace(req, null));
          }
        });
    return result;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow-instances/{id}/replay")
  public void replayWorkflowInstance(@PathParam("id") String workflowInstanceId) {
    engine.retryFailedWorkflowInstance(workflowInstanceId);
  }
}
