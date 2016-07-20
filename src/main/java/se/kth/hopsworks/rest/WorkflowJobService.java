package se.kth.hopsworks.rest;

import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.workflows.Workflow;
import se.kth.hopsworks.workflows.WorkflowExecution;
import se.kth.hopsworks.workflows.WorkflowJob;
import se.kth.hopsworks.workflows.WorkflowJobFacade;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@RequestScoped
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class WorkflowJobService {
    @EJB
    private WorkflowJobFacade workflowJobFacade;
    @EJB
    private NoCacheResponse noCacheResponse;

    public void setWorkflowExecution(WorkflowExecution execution) {
        this.execution = execution;
    }

    private WorkflowExecution execution;

    public WorkflowJobService() {

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response index() throws AppException {
        Collection<WorkflowJob> jobs = execution.getJobs();
        GenericEntity<Collection<WorkflowJob>> jobsList = new GenericEntity<Collection<WorkflowJob>>(jobs) {};
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jobsList).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response show(
            @PathParam("id") String id) throws AppException {

        WorkflowJob job = workflowJobFacade.find(id);
        if(job == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WOKFLOW_JOB_NOT_FOUND);
        }
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(job).build();
    }
}
