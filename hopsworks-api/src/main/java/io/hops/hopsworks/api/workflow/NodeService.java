package io.hops.hopsworks.api.workflow;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.workflow.Workflow;
import io.hops.hopsworks.common.exception.AppException;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import io.hops.hopsworks.common.dao.workflow.Node;
import io.hops.hopsworks.common.dao.workflow.NodeFacade;
import io.hops.hopsworks.common.dao.workflow.NodePK;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NodeService {

  private final static Logger logger = Logger.getLogger(NodeService.class.
          getName());

  @EJB
  private NodeFacade nodeFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  public void setWorkflow(Workflow workflow) {
    this.workflow = workflow;
  }

  private Workflow workflow;

  public NodeService() {

  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response index() throws AppException {
    Collection<Node> nodes = workflow.getNodes();
    GenericEntity<Collection<Node>> nodesList
            = new GenericEntity<Collection<Node>>(nodes) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            nodesList).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response create(
          String stringParams,
          @Context HttpServletRequest req) throws AppException {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode params = mapper.readTree(stringParams);
      Map<String, Object> paramsMap = mapper.convertValue(params, Map.class);
      if (paramsMap.get("type") == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "missing type input");
      }
      String className = LOWER_HYPHEN.to(UPPER_CAMEL, paramsMap.get("type").
              toString());
      Class nodeClass = Class.forName("se.kth.hopsworks.workflows.nodes."
              + className);
      Node node = (Node) nodeClass.newInstance();
      node.setWorkflow(workflow);
      node.setWorkflowId(workflow.getId());
      paramsMap.put("data", params.get("data"));
      BeanUtils.populate(node, paramsMap);

      nodeFacade.persist(node);
      JsonNode json = new ObjectMapper().valueToTree(node);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json.toString()).build();
    } catch (ClassNotFoundException | IllegalAccessException |
            InstantiationException | InvocationTargetException | IOException |
            EJBException | PersistenceException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
              getMessage());
    }

  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response show(
          @PathParam("id") String id) throws AppException {
    NodePK nodePk = new NodePK(id, workflow.getId());
    Node node;
    try {
      node = nodeFacade.findById(nodePk);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.NODE_NOT_FOUND);
    }

    JsonNode json = new ObjectMapper().valueToTree(node);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response update(
          String stringParams,
          @PathParam("id") String id) throws AppException {
    try {
      NodePK nodePk = new NodePK(id, workflow.getId());
      Node node;
      try {
        node = nodeFacade.findById(nodePk);
      } catch (EJBException e) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.NODE_NOT_FOUND);
      }
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode params = (ObjectNode) mapper.readTree(stringParams);
      Map<String, Object> paramsMap = mapper.convertValue(params, Map.class);

      if (params.get("type") == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "missing type input");
      }

      String className = LOWER_HYPHEN.to(UPPER_CAMEL, params.get("type").
              getValueAsText());
      paramsMap.put("classname", className);
      paramsMap.put("data", params.get("data"));
      BeanUtils.populate(node, paramsMap);
      node = nodeFacade.merge(node);

      JsonNode json = new ObjectMapper().valueToTree(node);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json.toString()).build();
    } catch (IOException | IllegalAccessException | InvocationTargetException |
            EJBException | PersistenceException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
              getMessage());
    }

  }

  @DELETE
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response delete(
          @PathParam("id") String id) throws AppException {
    NodePK nodePk = new NodePK(id, workflow.getId());
    Node node;
    try {
      node = nodeFacade.findById(nodePk);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.NODE_NOT_FOUND);
    }
    JsonNode json = new ObjectMapper().valueToTree(node);
    nodeFacade.remove(node);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }
}
