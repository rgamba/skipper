package com.maestroworkflow.admin;

import com.maestroworkflow.MaestroEngine;
import com.maestroworkflow.models.OperationResponse;
import com.maestroworkflow.models.WorkflowInstance;
import lombok.NonNull;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;


@Path("/admin")
public class AdminResource {
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
    public List<OperationResponse> getWorkflowInstanceOperationResponses(@PathParam("id") String workflowInstanceId) {
        return engine.getWorkflowInstanceOperationResults(workflowInstanceId);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/workflow-instances/{id}/replay")
    public void replayWorkflowInstance(@PathParam("id") String workflowInstanceId) {
        engine.retryFailedWorkflowInstance(workflowInstanceId);
    }
}
