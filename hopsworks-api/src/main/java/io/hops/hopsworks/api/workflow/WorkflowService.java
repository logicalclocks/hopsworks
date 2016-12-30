package io.hops.hopsworks.api.workflow;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.xml.sax.SAXException;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.workflow.Edge;
import io.hops.hopsworks.common.dao.workflow.EdgeFacade;
import io.hops.hopsworks.common.dao.workflow.NodeFacade;
import io.hops.hopsworks.common.dao.workflow.Workflow;
import io.hops.hopsworks.common.dao.workflow.WorkflowFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.workflows.nodes.BlankNode;
import io.hops.hopsworks.common.workflows.nodes.EndNode;
import io.hops.hopsworks.common.workflows.nodes.RootNode;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class WorkflowService {

  private final static Logger logger = Logger.getLogger(WorkflowService.class.
          getName());

  @EJB
  private WorkflowFacade workflowFacade;

  @EJB
  private NodeFacade nodeFacade;

  @EJB
  private EdgeFacade edgeFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  @Inject
  private NodeService nodeService;

  @Inject
  private EdgeService edgeService;

  @Inject
  private WorkflowExecutionService workflowExecutionService;

  public void setProject(Project project) {
    this.project = project;
  }

  public Project getProject() {
    return project;
  }

  Project project;

  public WorkflowService() {

  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response index() throws AppException {
    Collection<Workflow> workflows = project.getWorkflowCollection();
    GenericEntity<Collection<Workflow>> workflowsList
            = new GenericEntity<Collection<Workflow>>(workflows) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            workflowsList).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response create(
          Workflow workflow,
          @Context HttpServletRequest req) throws AppException {
    try {
      workflow.setProjectId(this.project.getId());
      workflowFacade.persist(workflow);
      workflowFacade.flush();

      RootNode root = new RootNode();
      root.setWorkflow(workflow);
      EndNode end = new EndNode();
      end.setWorkflow(workflow);
      BlankNode blank = new BlankNode();
      blank.setWorkflow(workflow);
      nodeFacade.persist(root);
      nodeFacade.persist(end);
      nodeFacade.persist(blank);
      nodeFacade.flush();

      Edge rootEdge = new Edge(root, blank);
      edgeFacade.save(rootEdge);

      Edge endEdge = new Edge(blank, end);
      edgeFacade.save(endEdge);
      edgeFacade.flush();

    } catch (EJBException | PersistenceException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
              getMessage());
    }

    JsonNode json = new ObjectMapper().valueToTree(workflowFacade.refresh(
            workflow));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }

  @POST
  @Path("/import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response create(
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail) throws
          AppException {

    Workflow workflow = new Workflow();
    workflow.setProjectId(this.project.getId());
    try {
      String xml = IOUtils.toString(uploadedInputStream, "utf-8")
              .replaceAll("\n", "")
              .replaceAll("\\s\\s+", " ")
              .replaceAll(">\\s*<", "><");

      nodeFacade.buildXml(xml, workflow);
    } catch (ParserConfigurationException | SAXException | ProcessingException |
            IOException | XPathException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              e.getMessage());
    }
    workflow = workflowFacade.refresh(workflow);
    JsonNode json = new ObjectMapper().valueToTree(workflow);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();

  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response show(
          @PathParam("id") Integer id) throws AppException {
    Workflow workflow;
    try {
      workflow = workflowFacade.find(id, project);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WORKFLOW_NOT_FOUND);
    }
    JsonNode json = new ObjectMapper().valueToTree(workflow);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response update(
          String stringParams,
          @PathParam("id") Integer id) throws AppException {
    try {
      Workflow workflow;
      try {
        workflow = workflowFacade.find(id, project);
      } catch (EJBException e) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.WORKFLOW_NOT_FOUND);
      }
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> paramsMap = mapper.convertValue(mapper.readTree(
              stringParams), Map.class);
      BeanUtils.populate(workflow, paramsMap);
      workflow = workflowFacade.merge(workflow);
      JsonNode json = new ObjectMapper().valueToTree(workflow);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json.toString()).build();
    } catch (IOException | IllegalAccessException | InvocationTargetException |
            EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
              getMessage());
    }
  }

  @DELETE
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response delete(
          @PathParam("id") Integer id) throws AppException {
    Workflow workflow;
    try {
      workflow = workflowFacade.find(id, project);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WORKFLOW_NOT_FOUND);
    }
    JsonNode json = new ObjectMapper().valueToTree(workflow);
    workflowFacade.remove(workflow);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }

  @Path("{id}/nodes")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public NodeService nodes(
          @PathParam("id") Integer id) throws AppException {
    Workflow workflow;
    try {
      workflow = workflowFacade.find(id, project);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WORKFLOW_NOT_FOUND);
    }
    this.nodeService.setWorkflow(workflow);

    return this.nodeService;
  }

  @Path("{id}/edges")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public EdgeService edges(
          @PathParam("id") Integer id) throws AppException {
    Workflow workflow;
    try {
      workflow = workflowFacade.find(id, project);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WORKFLOW_NOT_FOUND);
    }
    this.edgeService.setWorkflow(workflow);

    return this.edgeService;
  }

  @Path("{id}/executions")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public WorkflowExecutionService executions(
          @PathParam("id") Integer id) throws AppException {
    Workflow workflow;
    try {
      workflow = workflowFacade.find(id, project);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.WORKFLOW_NOT_FOUND);
    }
    this.workflowExecutionService.setWorkflow(workflow);

    return this.workflowExecutionService;
  }
}
