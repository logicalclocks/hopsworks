package io.hops.hopsworks.api.workflow;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import org.apache.oozie.client.OozieClientException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.workflow.NodeFacade;
import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Workflow;
import io.hops.hopsworks.common.dao.workflow.WorkflowExecution;
import io.hops.hopsworks.common.dao.workflow.WorkflowExecutionFacade;
import io.hops.hopsworks.common.dao.workflow.WorkflowJob;
import io.hops.hopsworks.common.dao.workflow.WorkflowJobFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFsService;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequestScoped
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class WorkflowExecutionService {

  @EJB
  private UserFacade userBean;

  @EJB
  private WorkflowExecutionFacade workflowExecutionFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private DistributedFsService dfs;

  @EJB
  private OozieFacade oozieFacade;

  @EJB
  private NodeFacade nodeFacade;

  @EJB
  private WorkflowJobFacade workflowJobFacade;

  public void setWorkflow(Workflow workflow) {
    this.workflow = workflow;
  }

  private Workflow workflow;

  @Inject
  private WorkflowJobService workflowJobService;

  public WorkflowExecutionService() {

  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response index() throws AppException {
    Collection<WorkflowExecution> executions = workflow.getWorkflowExecutions();
    GenericEntity<Collection<WorkflowExecution>> executionsList
            = new GenericEntity<Collection<WorkflowExecution>>(executions) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            executionsList).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response show(
          @PathParam("id") Integer id) throws AppException {

    WorkflowExecution execution;
    try {
      execution = workflowExecutionFacade.find(id, workflow);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WOKFLOW_EXECUTION_NOT_FOUND);
    }
    JsonNode json = new ObjectMapper().valueToTree(execution);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }

  @GET
  @Path("{id}/logs")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response logs(
          @PathParam("id") Integer id) throws AppException {
    WorkflowExecution execution;
    try {
      execution = workflowExecutionFacade.find(id, workflow);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WOKFLOW_EXECUTION_NOT_FOUND);
    }
    Collection<WorkflowJob> jobs = execution.getJobs();
    if (jobs == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WOKFLOW_JOB_NOT_FOUND);
    }
    String log;
    try {
      List<Map<String, String>> logs = oozieFacade.getLogs(execution);
      log = new ObjectMapper().writeValueAsString(logs);
    } catch (OozieClientException | IOException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              e.getMessage());
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            log).build();

  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response create(
          @Context HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(req.getRemoteUser());
    WorkflowExecution workflowExecution;
    if (workflow.getWorkflowExecutions().size() == 0
            || !workflow.getUpdatedAt().equals(workflow.getWorkflowExecutions().
                    get(0).getWorkflowTimestamp())) {
      workflowExecution = new WorkflowExecution();
      workflowExecution.setWorkflowId(workflow.getId());
      workflowExecution.setWorkflow(workflow);
      workflowExecution.setUser(user);
      workflowExecution.setUserId(user.getUid());
      workflowExecutionFacade.save(workflowExecution);
      workflowExecutionFacade.flush();
    } else {
      workflowExecution = workflow.getWorkflowExecutions().get(0);
    }

    this.oozieFacade.run(workflowExecution);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            workflowExecution).build();
  }

  @Path("{id}/jobs")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public WorkflowJobService jobs(
          @PathParam("id") Integer id) throws AppException {
    WorkflowExecution execution;
    try {
      execution = workflowExecutionFacade.find(id, workflow);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WOKFLOW_EXECUTION_NOT_FOUND);
    }
    this.workflowJobService.setWorkflowExecution(execution);

    return this.workflowJobService;
  }
}
