package io.hops.hopsworks.api.workflow;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.workflow.Edge;
import io.hops.hopsworks.common.dao.workflow.EdgeFacade;
import io.hops.hopsworks.common.dao.workflow.EdgePK;
import io.hops.hopsworks.common.dao.workflow.Workflow;
import io.hops.hopsworks.common.exception.AppException;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EdgeService {

  private final static Logger logger = Logger.getLogger(EdgeService.class.
          getName());

  @EJB
  private EdgeFacade edgeFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  public void setWorkflow(Workflow workflow) {
    this.workflow = workflow;
  }

  private Workflow workflow;

  public EdgeService() {

  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response index() throws AppException {
    Collection<Edge> edges = workflow.getEdges();
    GenericEntity<Collection<Edge>> edgesList
            = new GenericEntity<Collection<Edge>>(edges) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            edgesList).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response create(
          Edge edge,
          @Context HttpServletRequest req) throws AppException {
    edge.setWorkflowId(workflow.getId());
    try {
      edgeFacade.persist(edge);
    } catch (EJBException | PersistenceException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
              getMessage());
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            edge).build();

  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response show(
          @PathParam("id") String id) throws AppException {

    EdgePK edgePk = new EdgePK(id, workflow.getId());
    Edge edge;
    try {
      edge = edgeFacade.findById(edgePk);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EDGE_NOT_FOUND);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            edge).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response update(String stringParams, @PathParam("id") String id) throws
          AppException {
    EdgePK edgePk = new EdgePK(id, workflow.getId());
    Edge edge;
    try {
      edge = edgeFacade.findById(edgePk);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EDGE_NOT_FOUND);
    }

    ObjectMapper mapper = new ObjectMapper();
    try {
      Map<String, Object> paramsMap = mapper.convertValue(mapper.readTree(
              stringParams), Map.class);
      BeanUtils.populate(edge, paramsMap);
      edge = edgeFacade.merge(edge);
    } catch (IOException | IllegalAccessException | InvocationTargetException |
            EJBException | PersistenceException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
              getMessage());
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            edge).build();
  }

  @DELETE
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response delete(
          @PathParam("id") String id) throws AppException {
    EdgePK edgePk = new EdgePK(id, workflow.getId());
    Edge edge;
    try {
      edge = edgeFacade.findById(edgePk);
    } catch (EJBException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EDGE_NOT_FOUND);
    }
    JsonNode json = new ObjectMapper().valueToTree(edge);
    edgeFacade.remove(edge);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json.toString()).build();
  }
}
